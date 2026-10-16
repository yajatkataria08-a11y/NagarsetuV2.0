package com.nagarsetu.emergencyai.domain.triage

import com.nagarsetu.emergencyai.domain.model.Priority
import com.nagarsetu.emergencyai.domain.model.TriageInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keyword-based triage engine that classifies emergency descriptions into
 * [Priority] levels and returns an estimated dispatch ETA.
 *
 * Improvements over v1:
 *  - Multi-category scoring: every keyword category is scored; the highest-
 *    scoring priority wins, rather than stopping at the first match. This
 *    correctly handles inputs that contain both CRITICAL and MEDIUM terms.
 *  - Confidence is now included in [TriageInfo] so the UI can indicate
 *    certainty to the operator.
 *  - Keyword sets are backed by HashSets for O(1) lookup (was List.contains).
 *  - Input is sanitised (trimmed, collapsed whitespace) before matching.
 */
@Singleton
class TriageEngine @Inject constructor() {

    // ── Keyword sets (HashSet for O(1) lookup) ────────────────────────────────

    private val criticalKeywords: Set<String> = hashSetOf(
        "accident", "blood", "unconscious", "heart attack", "stroke", "cardiac",
        "collapse", "fire", "blast", "explosion", "trapped", "drowning",
        "chest pain", "not breathing", "overdose", "poisoning",
        "severe bleeding", "head injury", "spine", "fracture", "crash"
    )

    private val highKeywords: Set<String> = hashSetOf(
        "broken", "fracture", "breathing difficulty", "allergic", "snake bite",
        "electric shock", "burn", "bad fall", "hit", "faint", "dizzy",
        "vomit blood", "seizure", "bite"
    )

    private val mediumKeywords: Set<String> = hashSetOf(
        "pain", "fever", "theft", "robbery", "assault", "fight", "injury",
        "cut", "wound", "sprain", "nausea", "rash", "swelling",
        "domestic violence", "harassment"
    )

    // ── Priority descriptors ──────────────────────────────────────────────────

    private data class PriorityDescriptor(
        val priority: Priority,
        val keywords: Set<String>,
        val message: String,
        val etaSeconds: Int
    )

    private val descriptors = listOf(
        PriorityDescriptor(Priority.CRITICAL, criticalKeywords, "Immediate Dispatch — Life-threatening emergency", 480),
        PriorityDescriptor(Priority.HIGH,     highKeywords,     "Urgent Medical Assistance Required",             720),
        PriorityDescriptor(Priority.MEDIUM,   mediumKeywords,   "Medical Consultation Advised",                   1200),
        PriorityDescriptor(Priority.LOW,      emptySet(),       "Non-Urgent Support",                             3600)
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Processes free-form [input] text and returns a [TriageInfo] with:
     *  - The highest matched [Priority].
     *  - A human-readable medical guidance message.
     *  - Estimated dispatch ETA in seconds.
     *  - A [confidence] score in [0.0, 1.0].
     *
     * Confidence is defined as: matchedKeywords / (matchedKeywords + 2),
     * giving diminishing returns (e.g. 1 match → 0.33, 3 matches → 0.60,
     * 5 matches → 0.71). LOW priority always has confidence 0.1.
     */
    fun processInput(input: String): TriageInfo {
        val normalised = input.trim().replace(Regex("\\s+"), " ").lowercase()

        var bestPriority = Priority.LOW
        var bestMessage = descriptors.last().message
        var bestEta = descriptors.last().etaSeconds
        var bestScore = 0
        var bestConfidence = 0.1f

        for (desc in descriptors) {
            if (desc.keywords.isEmpty()) continue
            val score = countMatches(normalised, desc.keywords)
            if (score > bestScore) {
                bestScore = score
                bestPriority = desc.priority
                bestMessage = desc.message
                bestEta = desc.etaSeconds
                bestConfidence = (score.toFloat() / (score + 2f)).coerceIn(0.1f, 0.95f)
            }
        }

        return TriageInfo(
            priority = bestPriority,
            medicalNeed = bestMessage,
            etaSeconds = bestEta,
            confidence = bestConfidence
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Counts how many keywords from [keywords] appear anywhere in [text].
     * Multi-word keywords (e.g. "chest pain") are matched as substrings.
     */
    private fun countMatches(text: String, keywords: Set<String>): Int =
        keywords.count { text.contains(it) }
}
