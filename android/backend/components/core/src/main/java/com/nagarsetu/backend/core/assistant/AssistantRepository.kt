package com.nagarsetu.backend.core.assistant

import com.nagarsetu.core.data.local.dao.ChatDao
import com.nagarsetu.core.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantRepository @Inject constructor(
    private val assistant: NagarSetuAssistant,
    private val aiService: AiChatbotService,
    private val chatDao: ChatDao
) {
    val history: Flow<List<AssistantMessage>> = chatDao.observeAll().map { list ->
        list.map { e ->
            AssistantMessage(
                role = if (e.role == "user") AssistantRole.USER else AssistantRole.ASSISTANT,
                text = e.text,
                timestamp = e.timestamp
            )
        }
    }

    suspend fun sendUserMessage(text: String): AssistantReply {
        // 1. Save user message
        chatDao.insert(ChatMessageEntity(role = "user", text = text, timestamp = System.currentTimeMillis()))

        // 2. Try to get local intent/answer first (for speed)
        val localReply = assistant.reply(text)
        
        // 3. If local match is weak, use the AI APIs with your keys
        val finalReply = if (localReply.confidence < 0.6f) {
            aiService.getAiResponse(text)
        } else {
            localReply
        }

        // 4. Format and save bot response
        val botText = buildString {
            append(finalReply.answer)
            if (finalReply.suggestedRoute != null) {
                append("\n\n→ Tap **Go** to open ${finalReply.suggestedRoute.replace('_', ' ')}")
            }
            if (finalReply.sources.isNotEmpty()) {
                append("\n_Sources: ${finalReply.sources.joinToString()} • Confidence: ${(finalReply.confidence * 100).toInt()}%_")
            }
        }
        
        chatDao.insert(ChatMessageEntity(role = "assistant", text = botText, timestamp = System.currentTimeMillis()))
        return finalReply.copy(answer = botText)
    }

    suspend fun addAssistantGreeting() {
        val greet = assistant.greet()
        chatDao.insert(
            ChatMessageEntity(
                role = "assistant",
                text = greet.answer,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() {
        chatDao.clearAll()
    }
}
