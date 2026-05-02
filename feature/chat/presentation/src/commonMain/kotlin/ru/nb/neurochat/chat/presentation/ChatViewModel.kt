package ru.nb.neurochat.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
import ru.nb.neurochat.data.connectivity.ConnectivityObserver
import ru.nb.neurochat.data.preferences.UserSettingsStorage
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.model.ResponseStatistics
import ru.nb.neurochat.domain.repository.IChatRepository
import ru.nb.neurochat.domain.usecase.BuildChatContextUseCase
import ru.nb.neurochat.domain.usecase.CommandResult
import ru.nb.neurochat.domain.usecase.HandleCommandUseCase
import ru.nb.neurochat.domain.util.Result
import kotlin.time.TimeSource

internal const val DEFAULT_THINKING_BUDGET_TOKENS = 10_000
private const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * ViewModel фичи «Чат». Тонкая координация:
 * — хранит ChatState, отдаёт его как StateFlow;
 * — диспатчит ChatAction на handlers;
 * — запускает стрим репозитория, накапливает токены, обновляет последнее сообщение;
 * — мапит CommandResult use case'а в системные сообщения с локализацией.
 */
class ChatViewModel(
    private val baseSettings: ApiSettings,
    private val repository: IChatRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val settingsStorage: UserSettingsStorage,
    private val historyDataSource: IChatHistoryDataSource,
    private val handleCommand: HandleCommandUseCase,
    private val buildChatContext: BuildChatContextUseCase,
) : ViewModel() {

    private val log = Logger.withTag("ChatViewModel")

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
            started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = _state.value,
        )

    private var streamingJob: Job? = null

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnInputChange -> _state.update { it.copy(inputText = action.text) }
            ChatAction.OnSendMessage -> sendMessage()
            ChatAction.OnStopStreaming -> stopStreaming()
            ChatAction.OnClearHistory -> clearHistory()
            ChatAction.OnSettingsClick -> _state.update { it.copy(isSettingsOpen = true) }
            ChatAction.OnDismissSettings -> _state.update { it.copy(isSettingsOpen = false) }
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
            viewModelScope.launch { applyCommand(text) }
            return
        }

        startStreamingForUserMessage(text)
    }

    private fun startStreamingForUserMessage(text: String) {
        val userMessage = ChatMessage(role = ChatRole.User, content = text)
        _state.update { it.copy(inputText = "", isLoading = true, error = null, lastUsage = null) }

        streamingJob = viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    messages = state.messages + userMessage + ChatMessage(ChatRole.Assistant, "")
                )
            }

            val context = buildChatContext(
                history = _state.value.messages.dropLast(1),
                systemPrompt = currentSettings.systemPrompt,
                maxContextMessages = _state.value.maxContextMessages,
            )

            val accumulated = StringBuilder()
            var tokenCount = 0
            val timeMark = TimeSource.Monotonic.markNow()
            var streamError = false

            log.i { "stream start: model=${currentSettings.model}, ctx=${context.size}" }
            repository.streamMessage(context, currentSettings).collect { result ->
                when (result) {
                    is Result.Failure -> {
                        streamError = true
                        log.w { "stream failed: ${result.error}" }
                        _state.update { it.copy(isLoading = false, error = result.error) }
                        eventChannel.send(ChatEvent.OnError(result.error))
                    }
                    is Result.Success -> consumeToken(result.data, accumulated)
                        .also { if (it) tokenCount++ }
                }
            }

            val durationMs = timeMark.elapsedNow().inWholeMilliseconds
            updateLastMessageStatistics(
                ResponseStatistics(durationMs, tokenCount, accumulated.length)
            )
            _state.update { it.copy(isLoading = false) }

            if (!streamError) {
                val assistantMessage = _state.value.messages.last()
                historyDataSource.saveMessage(userMessage)
                historyDataSource.saveMessage(assistantMessage)
                log.i { "stream done: tokens=$tokenCount, durMs=$durationMs" }
            }
        }
    }

    /** @return true если получен текстовый токен (для подсчёта). */
    private fun consumeToken(token: ru.nb.neurochat.domain.model.StreamToken, acc: StringBuilder): Boolean {
        token.usage?.let { usage ->
            _state.update { state ->
                state.copy(
                    lastUsage = usage,
                    sessionTotalTokens = state.sessionTotalTokens + usage.totalTokens,
                )
            }
        }
        if (!token.isThinking && token.text.isNotEmpty()) {
            acc.append(token.text)
            updateLastMessage(acc.toString())
            return true
        }
        return false
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

    private suspend fun applyCommand(text: String) {
        when (val result = handleCommand(text, currentSettings)) {
            is CommandResult.SystemPromptChanged -> {
                currentSettings = result.settings
                val prompt = result.prompt
                _state.update { it.copy(systemPrompt = prompt) }
                postSystemMessage(
                    if (prompt != null) getString(Res.string.cmd_system_set, prompt)
                    else getString(Res.string.cmd_system_reset)
                )
            }
            is CommandResult.TemperatureChanged -> {
                currentSettings = result.settings
                _state.update { it.copy(currentTemperature = result.value) }
                postSystemMessage(getString(Res.string.cmd_t_set, result.value.toString()))
            }
            is CommandResult.ThinkingChanged -> {
                currentSettings = result.settings
                _state.update { it.copy(thinkingEnabled = result.enabled) }
                postSystemMessage(
                    if (result.enabled) getString(Res.string.cmd_think_on, result.budget.toString())
                    else getString(Res.string.cmd_think_off)
                )
            }
            CommandResult.TemperatureHint -> postSystemMessage(getString(Res.string.cmd_t_hint))
            CommandResult.ThinkingHint -> postSystemMessage(getString(Res.string.cmd_think_hint))
            CommandResult.Help -> postSystemMessage(getString(Res.string.cmd_help))
            CommandResult.UnknownCommand -> postSystemMessage(getString(Res.string.cmd_unknown))
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
        val budget = if (enabled) DEFAULT_THINKING_BUDGET_TOKENS else null
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
        viewModelScope.launch { historyDataSource.clearAll() }
        _state.update {
            it.copy(messages = emptyList(), error = null, lastUsage = null, sessionTotalTokens = 0)
        }
    }
}
