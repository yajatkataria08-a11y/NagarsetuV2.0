package com.nagarsetu.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.backend.core.assistant.AssistantMessage
import com.nagarsetu.backend.core.assistant.AssistantRepository
import com.nagarsetu.backend.core.assistant.NagarSetuAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantUiState(
    val messages: List<AssistantMessage> = emptyList(),
    val isLoading: Boolean = false,
    val lastSuggestedRoute: String? = null,
    val quickPrompts: List<Pair<String, String>> = emptyList()
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: AssistantRepository,
    private val assistant: NagarSetuAssistant
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantUiState(quickPrompts = assistant.quickPrompts()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.history.collect { history ->
                _state.update { it.copy(messages = history) }
            }
        }
        viewModelScope.launch {
            val initial = repository.history.first()
            if (initial.isEmpty()) repository.addAssistantGreeting()
        }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val reply = repository.sendUserMessage(text.trim())
            _state.update {
                it.copy(isLoading = false, lastSuggestedRoute = reply.suggestedRoute)
            }
        }
    }

    fun sendQuick(prompt: String) = send(prompt)

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
            repository.addAssistantGreeting()
            _state.update { it.copy(lastSuggestedRoute = null) }
        }
    }
}
