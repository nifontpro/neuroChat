package ru.nb.neurochat.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_help
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_model_set
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_system_reset
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_system_set
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_t_hint
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_t_set
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_think_hint
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_think_off
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_think_on
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_unknown
import ru.nb.neurochat.chat.presentation.generated.resources.error_unknown
import ru.nb.neurochat.data.connectivity.ConnectivityObserver
import ru.nb.neurochat.data.preferences.UserSettingsStorage
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.model.ResponseStatistics
import ru.nb.neurochat.domain.repository.IChatRepository
import kotlin.time.TimeSource

class ChatViewModel(
    private val baseSettings: ApiSettings,
    private val repository: IChatRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val settingsStorage: UserSettingsStorage,
    private val historyDataSource: IChatHistoryDataSource,
) : ViewModel() {

    private var currentSettings = baseSettings

    private val eventChannel = Channel<ChatEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(
        ChatState(
            currentModel = baseSettings.model,
            currentTemperature = baseSettings.temperature,
            thinkingEnabled = baseSettings.thinkingBudget != null,
            systemPrompt = baseSettings.systemPrompt,
        )
    )

    val state = _state
        .onStart {
            observeConnectivity()
            loadSavedSettings()
            loadHistory()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = _state.value,
        )

    private var streamingJob: Job? = null

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnInputChange -> {
                _state.update { it.copy(inputText = action.text) }
            }
            ChatAction.OnSendMessage -> sendMessage()
            ChatAction.OnStopStreaming -> stopStreaming()
            ChatAction.OnClearHistory -> clearHistory()
            ChatAction.OnSettingsClick -> {
                _state.update { it.copy(isSettingsOpen = true) }
            }
            ChatAction.OnDismissSettings -> {
                _state.update { it.copy(isSettingsOpen = false) }
            }
            is ChatAction.OnSelectModel -> selectModel(action.model)
            is ChatAction.OnTemperatureChange -> updateTemperature(action.temperature)
            is ChatAction.OnThinkingToggle -> toggleThinking(action.enabled)
            is ChatAction.OnSystemPromptChange -> updateSystemPrompt(action.prompt)
            is ChatAction.OnMaxContextChange -> updateMaxContext(action.count)
            is ChatAction.OnToggleStatistics -> toggleStatistics(action.enabled)
            ChatAction.OnResetSettings -> resetSettings()
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val messages = historyDataSource.getMessages()
            if (messages.isNotEmpty()) {
                _state.update { it.copy(messages = messages) }
            }
        }
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            val saved = settingsStorage.load() ?: return@launch
            saved.model?.let { selectModel(it) }
            saved.temperature?.let { updateTemperature(it) }
            saved.thinkingEnabled?.let { toggleThinking(it) }
            saved.systemPrompt?.let { updateSystemPrompt(it) }
            saved.maxContextMessages?.let { updateMaxContext(it) }
            saved.showStatistics?.let { toggleStatistics(it) }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { connected ->
                _state.update { it.copy(isConnected = connected) }
            }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isLoading) return

        if (text.startsWith("/")) {
            _state.update { it.copy(inputText = "") }
            viewModelScope.launch { handleCommand(text) }
            return
        }

        val userMessage = ChatMessage(role = ChatRole.User, content = text)
        _state.update { it.copy(inputText = "", isLoading = true, error = null, lastUsage = null) }

        streamingJob = viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    messages = state.messages + userMessage + ChatMessage(ChatRole.Assistant, "")
                )
            }

            val contextMessages = _state.value.messages.dropLast(1).let { msgs ->
                val limit = _state.value.maxContextMessages
                if (limit > 0 && msgs.size > limit) msgs.takeLast(limit) else msgs
            }
            val history = buildList {
                currentSettings.systemPrompt?.let { add(ChatMessage(ChatRole.System, it)) }
                addAll(contextMessages)
            }

            val accumulated = StringBuilder()
            var tokenCount = 0
            val timeMark = TimeSource.Monotonic.markNow()
            var streamError = false

            repository.streamMessage(history, currentSettings)
                .catch { e ->
                    streamError = true
                    _state.update { it.copy(isLoading = false, error = e.message) }
                    eventChannel.send(ChatEvent.OnError(e.message ?: getString(Res.string.error_unknown)))
                }
                .collect { token ->
                    token.usage?.let { usage ->
                        _state.update { state ->
                            state.copy(
                                lastUsage = usage,
                                sessionTotalTokens = state.sessionTotalTokens + usage.totalTokens,
                            )
                        }
                    }
                    if (!token.isThinking && token.text.isNotEmpty()) {
                        accumulated.append(token.text)
                        tokenCount++
                        updateLastMessage(accumulated.toString())
                    }
                }

            val durationMs = timeMark.elapsedNow().inWholeMilliseconds
            val statistics = ResponseStatistics(
                durationMs = durationMs,
                tokenCount = tokenCount,
                charCount = accumulated.length,
            )
            updateLastMessageStatistics(statistics)
            _state.update { it.copy(isLoading = false) }

            if (!streamError) {
                val assistantMessage = _state.value.messages.last()
                historyDataSource.saveMessage(userMessage)
                historyDataSource.saveMessage(assistantMessage)
            }
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

    private fun updateLastMessageStatistics(statistics: ResponseStatistics) {
        _state.update { state ->
            val messages = state.messages.toMutableList()
            if (messages.isNotEmpty() && messages.last().role == ChatRole.Assistant) {
                messages[messages.lastIndex] = messages.last().copy(statistics = statistics)
            }
            state.copy(messages = messages)
        }
    }

    private suspend fun handleCommand(text: String) {
        val parts = text.split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim() ?: ""

        when (cmd) {
            "/system" -> {
                val prompt = arg.ifBlank { null }
                currentSettings = currentSettings.copy(systemPrompt = prompt)
                _state.update { it.copy(systemPrompt = prompt) }
                postSystemMessage(
                    if (prompt != null) getString(Res.string.cmd_system_set, prompt)
                    else getString(Res.string.cmd_system_reset)
                )
            }

            "/t" -> {
                val temp = arg.toDoubleOrNull()
                if (temp != null && temp in 0.0..2.0) {
                    currentSettings = currentSettings.copy(temperature = temp)
                    _state.update { it.copy(currentTemperature = temp) }
                    postSystemMessage(getString(Res.string.cmd_t_set, temp.toString()))
                } else {
                    postSystemMessage(getString(Res.string.cmd_t_hint))
                }
            }

            "/think" -> {
                when (arg.lowercase()) {
                    "on" -> {
                        val budget = 10_000
                        currentSettings = currentSettings.copy(thinkingBudget = budget)
                        _state.update { it.copy(thinkingEnabled = true) }
                        postSystemMessage(getString(Res.string.cmd_think_on, budget.toString()))
                    }
                    "off" -> {
                        currentSettings = currentSettings.copy(thinkingBudget = null)
                        _state.update { it.copy(thinkingEnabled = false) }
                        postSystemMessage(getString(Res.string.cmd_think_off))
                    }
                    else -> postSystemMessage(getString(Res.string.cmd_think_hint))
                }
            }

            "/?" -> {
                postSystemMessage(getString(Res.string.cmd_help))
            }

            else -> postSystemMessage(getString(Res.string.cmd_unknown))
        }
    }

    private fun postSystemMessage(text: String) {
        _state.update { state ->
            state.copy(messages = state.messages + ChatMessage(ChatRole.System, text))
        }
    }

    private fun selectModel(model: String) {
        currentSettings = currentSettings.copy(model = model)
        _state.update { it.copy(currentModel = model) }
        viewModelScope.launch {
            settingsStorage.saveModel(model)
            postSystemMessage(getString(Res.string.cmd_model_set, model))
        }
    }

    private fun updateTemperature(temp: Double?) {
        currentSettings = currentSettings.copy(temperature = temp)
        _state.update { it.copy(currentTemperature = temp) }
        viewModelScope.launch { settingsStorage.saveTemperature(temp) }
    }

    private fun updateSystemPrompt(prompt: String?) {
        currentSettings = currentSettings.copy(systemPrompt = prompt)
        _state.update { it.copy(systemPrompt = prompt) }
        viewModelScope.launch { settingsStorage.saveSystemPrompt(prompt) }
    }

    private fun updateMaxContext(count: Int) {
        _state.update { it.copy(maxContextMessages = count) }
        viewModelScope.launch { settingsStorage.saveMaxContext(count) }
    }

    private fun toggleStatistics(enabled: Boolean) {
        _state.update { it.copy(showStatistics = enabled) }
        viewModelScope.launch { settingsStorage.saveShowStatistics(enabled) }
    }

    private fun toggleThinking(enabled: Boolean) {
        val budget = if (enabled) 10_000 else null
        currentSettings = currentSettings.copy(thinkingBudget = budget)
        _state.update { it.copy(thinkingEnabled = enabled) }
        viewModelScope.launch { settingsStorage.saveThinkingEnabled(enabled) }
    }

    private fun resetSettings() {
        viewModelScope.launch {
            settingsStorage.clear()
            currentSettings = baseSettings
            _state.update {
                it.copy(
                    currentModel = baseSettings.model,
                    currentTemperature = baseSettings.temperature,
                    thinkingEnabled = baseSettings.thinkingBudget != null,
                    systemPrompt = baseSettings.systemPrompt,
                    maxContextMessages = 0,
                    showStatistics = false,
                )
            }
        }
    }

    private fun stopStreaming() {
        streamingJob?.cancel()
        _state.update { it.copy(isLoading = false) }
    }

    private fun clearHistory() {
        stopStreaming()
        viewModelScope.launch {
            historyDataSource.clearAll()
        }
        _state.update { it.copy(messages = emptyList(), error = null, lastUsage = null, sessionTotalTokens = 0) }
    }
}
