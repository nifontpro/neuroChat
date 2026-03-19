package ru.nb.neurochat.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nb.neurochat.data.connectivity.ConnectivityObserver
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.repository.IChatRepository

class ChatViewModel(
    private val repository: IChatRepository,
    private val baseSettings: ApiSettings,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private var currentSettings = baseSettings

    private val _state = MutableStateFlow(
        ChatState(
            currentModel = baseSettings.model,
            currentTemperature = baseSettings.temperature,
            thinkingEnabled = baseSettings.thinkingBudget != null,
            systemPrompt = baseSettings.systemPrompt,
        )
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var streamingJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { connected ->
                _state.update { it.copy(isConnected = connected) }
            }
        }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isLoading) return

        if (text.startsWith("/")) {
            handleCommand(text)
            _state.update { it.copy(inputText = "") }
            return
        }

        val userMessage = ChatMessage(role = ChatRole.User, content = text)
        _state.update { it.copy(inputText = "", isLoading = true, error = null) }

        streamingJob = viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    messages = state.messages + userMessage + ChatMessage(ChatRole.Assistant, "")
                )
            }

            val history = buildList {
                currentSettings.systemPrompt?.let { add(ChatMessage(ChatRole.System, it)) }
                addAll(_state.value.messages.dropLast(1))
            }

            val accumulated = StringBuilder()

            repository.streamMessage(history, currentSettings)
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { token ->
                    if (!token.isThinking) {
                        accumulated.append(token.text)
                        updateLastMessage(accumulated.toString())
                    }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun updateLastMessage(text: String) {
        _state.update { state ->
            val messages = state.messages.toMutableList()
            if (messages.isNotEmpty() && messages.last().role == ChatRole.Assistant) {
                messages[messages.lastIndex] = messages.last().copy(content = text)
            }
            state.copy(messages = messages)
        }
    }

    private fun handleCommand(text: String) {
        val parts = text.split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim() ?: ""

        when (cmd) {
            "/system" -> {
                val prompt = arg.ifBlank { null }
                currentSettings = currentSettings.copy(systemPrompt = prompt)
                _state.update { it.copy(systemPrompt = prompt) }
                postSystemMessage(
                    "Системный промпт ${if (prompt != null) "установлен: $prompt" else "сброшен"}"
                )
            }

            "/t" -> {
                val temp = arg.toDoubleOrNull()
                if (temp != null && temp in 0.0..2.0) {
                    currentSettings = currentSettings.copy(temperature = temp)
                    _state.update { it.copy(currentTemperature = temp) }
                    postSystemMessage("Temperature: $temp")
                } else {
                    postSystemMessage("Используй: /t <0.0-2.0>")
                }
            }

            "/think" -> {
                when (arg.lowercase()) {
                    "on" -> {
                        val budget = 10_000
                        currentSettings = currentSettings.copy(thinkingBudget = budget)
                        _state.update { it.copy(thinkingEnabled = true) }
                        postSystemMessage("Режим thinking включён (budget: $budget токенов)")
                    }
                    "off" -> {
                        currentSettings = currentSettings.copy(thinkingBudget = null)
                        _state.update { it.copy(thinkingEnabled = false) }
                        postSystemMessage("Режим thinking выключён")
                    }
                    else -> postSystemMessage("Используй: /think on | /think off")
                }
            }

            "/?" -> {
                postSystemMessage(
                    """
                    Команды:
                    /system <текст> — задать системный промпт (без аргумента — сброс)
                    /t <0.0-2.0> — температура
                    /think on|off — режим расширенного мышления
                    /? — справка
                    """.trimIndent()
                )
            }

            else -> postSystemMessage("Неизвестная команда. Напиши /? для справки.")
        }
    }

    private fun postSystemMessage(text: String) {
        _state.update { state ->
            state.copy(messages = state.messages + ChatMessage(ChatRole.System, text))
        }
    }

    fun selectModel(model: String) {
        currentSettings = currentSettings.copy(model = model)
        _state.update { it.copy(currentModel = model) }
        postSystemMessage("Модель: $model")
    }

    fun updateTemperature(temp: Double?) {
        currentSettings = currentSettings.copy(temperature = temp)
        _state.update { it.copy(currentTemperature = temp) }
    }

    fun updateSystemPrompt(prompt: String?) {
        currentSettings = currentSettings.copy(systemPrompt = prompt)
        _state.update { it.copy(systemPrompt = prompt) }
    }

    fun toggleThinking(enabled: Boolean) {
        val budget = if (enabled) 10_000 else null
        currentSettings = currentSettings.copy(thinkingBudget = budget)
        _state.update { it.copy(thinkingEnabled = enabled) }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _state.update { it.copy(isLoading = false) }
    }

    fun clearHistory() {
        stopStreaming()
        _state.update { it.copy(messages = emptyList(), error = null) }
    }
}
