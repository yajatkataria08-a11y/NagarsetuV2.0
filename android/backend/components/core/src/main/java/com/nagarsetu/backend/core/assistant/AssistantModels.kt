package com.nagarsetu.backend.core.assistant

import com.google.gson.annotations.SerializedName

data class AssistantMessage(
    val role: AssistantRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedRoute: String? = null,
    val confidence: Float = 1f
)

enum class AssistantRole { USER, ASSISTANT, SYSTEM }

data class AssistantReply(
    val answer: String,
    val suggestedRoute: String? = null,
    val sources: List<String> = emptyList(),
    val confidence: Float = 0.85f
)

enum class CivicIntent {
    PARKING, CHARGING, EMERGENCY, REPORT, ROAD, LEGAL, HEALTH, ROUTE, PREDICTIVE, RAKSHA, GENERAL
}

// --- Groq API Models ---
data class GroqRequest(
    val model: String = "llama-3.1-8b-instant",   // valid Groq model (api.groq.com)
    val messages: List<GroqMessage>,
    val temperature: Double = 0.1
)
data class GroqMessage(val role: String, val content: String)
data class GroqResponse(val choices: List<GroqChoice>)
data class GroqChoice(val message: GroqMessage)

// --- Gemini API Models ---
data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiPart(val text: String)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContent?)
