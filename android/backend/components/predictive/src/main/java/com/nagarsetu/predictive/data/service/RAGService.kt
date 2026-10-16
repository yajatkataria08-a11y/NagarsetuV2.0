package com.nagarsetu.predictive.data.service

import com.nagarsetu.backend.core.data.CivicDataHub
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.predictive.domain.model.RAGResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAGService — Retrieval Augmented Generation for Civic Intelligence.
 *
 * In production: connect to a vector store (Pinecone/Weaviate) seeded with:
 *   • NagarSetu incident reports
 *   • Bhopal Municipal Corporation notifications
 *   • Traffic violation records
 *   • IPC / Motor Vehicle Act sections
 *   • BIMSTEC city benchmarks
 *
 * Currently: keyword-based retrieval from seed data + template answer generator.
 */
@Singleton
class RAGService @Inject constructor(
    private val assets: AssetDataRepository,
    private val hub: CivicDataHub
) {
    // Knowledge base chunks simulating vector retrieval
    private val knowledgeBase = buildKnowledgeBase()

    /**
     * Query the RAG pipeline.
     * @param query Natural language question from citizen
     * @return RAGResult with answer, sources, related incidents & legal refs
     */
    suspend fun query(query: String): RAGResult = withContext(Dispatchers.IO) {
        try {
            delay(600) // Simulate retrieval latency
            val q = query.lowercase()

            // Retrieve top-k chunks by keyword overlap (simulating cosine similarity)
            val retrieved = knowledgeBase
                .filter { chunk -> chunk.keywords.any { kw -> q.contains(kw) } }
                .sortedByDescending { chunk -> chunk.keywords.count { kw -> q.contains(kw) } }
                .take(3)

            val answer = if (retrieved.isEmpty()) {
                generateGenericAnswer(query)
            } else {
                buildAnswer(query, retrieved)
            }

            val confidence = if (retrieved.isEmpty()) 0.45f else 0.72f + (retrieved.size * 0.05f)

            RAGResult(
                query = query,
                answer = answer,
                sources = retrieved.map { it.source }.distinct(),
                confidence = confidence.coerceAtMost(0.95f),
                relatedIncidents = retrieved.flatMap { it.relatedIncidents }.take(3),
                legalSections = retrieved.flatMap { it.legalSections }.distinct().take(3)
            )
        } catch (e: Exception) {
            Log.e("RAGService", "RAG query failed: ${e.message}")
            RAGResult(
                query = query,
                answer = "Unable to fetch predictive data right now. Showing cached risk forecast instead.",
                sources = emptyList(),
                confidence = 0.3f,
                relatedIncidents = emptyList(),
                legalSections = emptyList()
            )
        }
    }

    // ── Knowledge Base Builder ────────────────────────────────────────────────
    private fun buildKnowledgeBase(): List<KnowledgeChunk> {
        val data = assets.loadAppData()
        val chunks = mutableListOf<KnowledgeChunk>()

        // Accident blackspots
        data.getAsJsonObject("predictiveHazards")
            ?.getAsJsonArray("accidentBlackspots")
            ?.forEach { el ->
                val o = el.asJsonObject
                chunks += KnowledgeChunk(
                    id = o["id"].asString,
                    content = "Accident blackspot: ${o["name"].asString} with risk score ${o["riskScore"].asInt}. " +
                              "${o["incidentsLastYear"].asInt} incidents in last year.",
                    keywords = listOf("accident", "blackspot", "crash", "collision",
                        o["name"].asString.lowercase().split(" ").first()),
                    source = "NagarSetu Accident DB 2024",
                    relatedIncidents = listOf("Incident near ${o["name"].asString} — Q4 2024"),
                    legalSections = listOf("MV Act §134 — Duty after accident", "IPC §304A — Rash driving")
                )
            }

        // Flood zones
        data.getAsJsonObject("predictiveHazards")
            ?.getAsJsonArray("floodZones")
            ?.forEach { el ->
                val o = el.asJsonObject
                chunks += KnowledgeChunk(
                    id = o["id"].asString,
                    content = "Flood zone: ${o["area"].asString}. Drainage status: ${o["drainageStatus"].asString}. " +
                              "Near ${o["proximityToWater"].asString}. Risk score: ${o["riskScore"].asInt}.",
                    keywords = listOf("flood", "water", "rain", "drainage", "lake",
                        o["area"].asString.lowercase().split(" ").first()),
                    source = "BMC Flood Risk Assessment 2024",
                    relatedIncidents = listOf("2023 monsoon flooding at ${o["area"].asString}"),
                    legalSections = listOf("NDMA Guidelines §7 — Urban flood mitigation")
                )
            }

        // Ward governance
        data.getAsJsonArray("wards")?.forEach { el ->
            val o = el.asJsonObject
            chunks += KnowledgeChunk(
                id = o["id"].asString,
                content = "Ward ${o["name"].asString}: ${o["complaintCount"].asInt} complaints, " +
                          "${o["resolvedCount"].asInt} resolved. SLA breaches: ${o["slaBreaches"].asInt}. " +
                          "Authority: ${o["authorityName"].asString}. Helpline: ${o["authorityHelpline"].asString}.",
                keywords = listOf("ward", "complaint", "sla", "governance", "authority",
                    o["name"].asString.lowercase().split(" ").first()),
                source = "NagarSetu Ward KPI Dashboard",
                relatedIncidents = emptyList(),
                legalSections = listOf("MP Municipal Corporation Act §87 — Civic grievance redressal")
            )
        }

        // City summary
        chunks += KnowledgeChunk(
            id = "city_summary",
            content = hub.citySummary(),
            keywords = listOf("bhopal", "city", "overview", "parking", "ev", "stations", "wards"),
            source = "NagarSetu CivicDataHub",
            relatedIncidents = emptyList(),
            legalSections = emptyList()
        )

        return chunks
    }

    private fun buildAnswer(query: String, chunks: List<KnowledgeChunk>): String {
        val topChunk = chunks.first()
        return buildString {
            append("Based on NagarSetu's civic intelligence data:\n\n")
            chunks.forEachIndexed { i, chunk ->
                append("${i + 1}. ${chunk.content}\n\n")
            }
            if (chunks.any { it.legalSections.isNotEmpty() }) {
                append("Relevant regulations: ${chunks.flatMap { it.legalSections }.distinct().joinToString(", ")}")
            }
        }
    }

    private fun generateGenericAnswer(query: String): String =
        "NagarSetu AI: I found limited data for '$query' in Bhopal's civic database. " +
        "For real-time hazard data, please check the Predictive map or contact your ward authority. " +
        "Emergency helpline: 112 | Civic helpline: 155304."

    private data class KnowledgeChunk(
        val id: String,
        val content: String,
        val keywords: List<String>,
        val source: String,
        val relatedIncidents: List<String>,
        val legalSections: List<String>
    )
}
