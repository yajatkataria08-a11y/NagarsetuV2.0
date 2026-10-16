package com.nagarsetu.backend.core.assistant

import android.util.Log
import com.nagarsetu.core.data.AssetDataRepository
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AiChatbotService
 *
 * Routes queries to Groq (fast, cheap) or Gemini (complex / fallback).
 *
 * Keys come from BuildConfig, injected from local.properties at compile time.
 * Neither key is hardcoded or committed to source control.
 *
 * Groq endpoint:   https://api.groq.com/openai/v1/
 *   Model:         llama-3.1-8b-instant  (fast) or llama-3.3-70b-versatile (hard)
 * Gemini endpoint: https://generativelanguage.googleapis.com/
 *   Model:         gemini-1.5-flash
 */
@Singleton
class AiChatbotService @Inject constructor(
    private val assets: AssetDataRepository,
    private val config: AiAssistantConfig,
    private val httpClient: okhttp3.OkHttpClient
) {
    private val groqKey: String   get() = config.groqApiKey
    private val geminiKey: String get() = config.geminiApiKey
    private val TIMEOUT = 30_000L

    private val groqApi: GroqApiService = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/v1/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(GroqApiService::class.java)

    private val geminiApi: GeminiApiService = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(GeminiApiService::class.java)

    suspend fun getAiResponse(query: String): AssistantReply {
        val context = getRelevantContext(query)
        val prompt = buildString {
            append("You are NagarSetu AI, an expert civic guide for Bhopal, India. ")
            append("Knowledge Base (Real Bhopal/India Traffic Rules & Fines): ")
            append("- No Helmet: ₹1000 fine + 3-month license disqualification. ")
            append("- No Seatbelt: ₹1000 fine. ")
            append("- Triple Riding: ₹1000 fine. ")
            append("- Mobile while driving: ₹1000 to ₹5000 fine. ")
            append("- Signal Jump (Red light): ₹1000 to ₹5000 fine. ")
            append("- Overspeeding: ₹1000 to ₹4000 depending on vehicle. ")
            append("- Wrong Parking: ₹500 fine + towing charges. ")
            append("- No Driving License (DL): ₹5000 fine. ")
            append("- No RC: ₹2000 to ₹5000 fine. ")
            append("- No Insurance: ₹2000 fine. ")
            append("- No PUC: ₹10000 fine. ")
            append("- Drunk Driving: ₹10000 fine + possible jail. ")
            append("- Wrong Side/Dangerous driving: up to ₹5000 fine. ")
            append("- Illegal U-turn: ₹500 fine. ")
            append("- Number Plate violation: up to ₹5000. ")
            append("\n\n")
            
            if (context.isNotBlank()) {
                append("Use this GeoData context:\n$context\n\n")
            }
            append("User: $query\n")
            append("Guidelines: ")
            append("1. Answer concisely in a friendly, helpful tone. ")
            append("2. Support Hinglish (Mixed Hindi and English) as Bhopal residents use it. ")
            append("3. Only mention Indian/Bhopal context rules. ")
            append("4. If you don't know the exact answer, politely suggest contacting Bhopal Traffic Police or BMC.")
        }

        if (groqKey.isBlank() || groqKey.startsWith("YOUR_") || groqKey.startsWith("gsk_xxx")) {
            Log.w("AiChatbotService", "GROQ_API_KEY not configured — returning offline reply")
            return AssistantReply(
                answer = "AI assistant is offline (API key not configured). Please check local.properties.",
                confidence = 0f
            )
        }

        return try {
            val response = if (isHardQuery(query)) {
                callGemini(prompt) ?: callGroq(prompt, hard = true)
            } else {
                withTimeoutOrNull(TIMEOUT) { callGroq(prompt) } ?: callGemini(prompt)
            }
            AssistantReply(
                answer     = response ?: "Sorry, I'm taking too long. Please try again.",
                sources    = if (context.isNotBlank()) listOf("geo.json") else emptyList(),
                confidence = if (response != null) 0.9f else 0f
            )
        } catch (e: Exception) {
            Log.e("AiChatbotService", "API error", e)
            AssistantReply("Sorry, I encountered an error: ${e.message?.take(80)}")
        }
    }

    // ── private ──────────────────────────────────────────────────────────────

    private fun getRelevantContext(query: String): String {
        val geo      = assets.loadGeoJson()
        val features = geo.getAsJsonArray("features") ?: return ""
        val keywords = query.lowercase().split(" ").filter { it.length > 3 }
        val matches  = mutableListOf<String>()
        for (i in 0 until features.size()) {
            val props = features[i].asJsonObject.getAsJsonObject("properties")
            if (keywords.any { props.toString().lowercase().contains(it) }) {
                matches += props.toString()
            }
            if (matches.size >= 5) break
        }
        return matches.joinToString("\n---\n")
    }

    private fun isHardQuery(query: String): Boolean {
        val hardWords = listOf("explain", "compare", "analysis", "difference", "detailed", "why", "suggest")
        return query.length > 80 || hardWords.any { query.lowercase().contains(it) }
    }

    private suspend fun callGroq(prompt: String, hard: Boolean = false): String? {
        return try {
            val model = if (hard) "llama-3.3-70b-versatile" else "llama-3.1-8b-instant"
            val request  = GroqRequest(model = model, messages = listOf(GroqMessage("user", prompt)))
            val response = groqApi.getCompletion("Bearer $groqKey", request)
            if (response.isSuccessful) response.body()?.choices?.firstOrNull()?.message?.content
            else { Log.e("AiChatbotService", "Groq HTTP ${response.code()}"); null }
        } catch (e: Exception) { Log.e("AiChatbotService", "Groq error: ${e.message}"); null }
    }

    private suspend fun callGemini(prompt: String): String? {
        if (geminiKey.isBlank() || geminiKey.startsWith("YOUR_")) return null
        return withTimeoutOrNull(TIMEOUT) {
            try {
                val request  = GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(prompt)))))
                val response = geminiApi.getCompletion(geminiKey, request)
                if (response.isSuccessful)
                    response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                else { Log.e("AiChatbotService", "Gemini HTTP ${response.code()}"); null }
            } catch (e: Exception) { Log.e("AiChatbotService", "Gemini error: ${e.message}"); null }
        }
    }
}

data class AiAssistantConfig(
    val groqApiKey: String,
    val geminiApiKey: String
)
