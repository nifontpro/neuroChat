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
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_branch_created
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_branch_hint
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_branch_switched
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_branches_list
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_compact_done
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_compact_failed
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_compact_start
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_help
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_model_set
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_strategy_hint
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_strategy_set
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_switch_hint
import ru.nb.neurochat.chat.presentation.generated.resources.cmd_switch_unknown
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
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource.Companion.MAIN_BRANCH_ID
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.model.ContextStrategy
import ru.nb.neurochat.domain.model.ResponseStatistics
import ru.nb.neurochat.domain.repository.IChatRepository
import ru.nb.neurochat.domain.usecase.BuildChatContextUseCase
import ru.nb.neurochat.domain.usecase.CommandResult
import ru.nb.neurochat.domain.usecase.HandleCommandUseCase
import ru.nb.neurochat.domain.usecase.UpdateFactsUseCase
import ru.nb.neurochat.domain.util.Result
import kotlin.time.TimeSource

internal const val DEFAULT_THINKING_BUDGET_TOKENS = 10_000
private const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L
private const val COMPACT_SYSTEM_PROMPT =
    "Summarize the following conversation concisely. Preserve all key facts, decisions, and context. Output only the summary, no preamble."
private const val FACTS_RECENT_WINDOW = 6

/**
 * ViewModel фичи «Чат». Тонкая координация:
 * — хранит ChatState, отдаёт его как StateFlow;
 * — диспатчит ChatAction на handlers;
 * — запускает стрим репозитория, накапливает токены, обновляет последнее сообщение;
 * — мапит CommandResult use case'а в системные сообщения с локализацией.
 *
 * Для стратегии STICKY_FACTS после каждого ответа модели в фоне зовётся [UpdateFactsUseCase],
 * результат сохраняется в DataStore и подмешивается в контекст следующего запроса.
 */
class ChatViewModel(
    private val baseSettings: ApiSettings,
    private val repository: IChatRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val settingsStorage: UserSettingsStorage,
    private val historyDataSource: IChatHistoryDataSource,
    private val handleCommand: HandleCommandUseCase,
    private val buildChatContext: BuildChatContextUseCase,
    private val updateFacts: UpdateFactsUseCase,
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
            initBranches()
            loadSavedSettings()
            loadHistory()
            loadAvailableModels()
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
            is ChatAction.OnMaxTokensChange -> updateMaxTokens(action.count)
            is ChatAction.OnToggleStatistics -> toggleStatistics(action.enabled)
            ChatAction.OnResetSettings -> resetSettings()
            is ChatAction.OnContextStrategyChange -> changeContextStrategy(action.strategy)
            ChatAction.OnClearFacts -> clearFacts()
            is ChatAction.OnCreateBranch -> createBranch(action.name)
            is ChatAction.OnSwitchBranch -> switchBranch(action.branchId)
            is ChatAction.OnDeleteBranch -> deleteBranch(action.branchId)
        }
    }

    private fun initBranches() {
        viewModelScope.launch {
            historyDataSource.ensureMainBranch()
            val branches = historyDataSource.getBranches()
            _state.update { it.copy(branches = branches) }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val branchId = _state.value.currentBranchId
            val messages = historyDataSource.getMessages(branchId)
            _state.update { it.copy(messages = messages) }
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
            saved.maxTokens?.let { updateMaxTokens(it) }
            saved.conversationSummary?.let { _state.update { s -> s.copy(conversationSummary = it) } }
            _state.update { it.copy(contextStrategy = saved.contextStrategy) }
            saved.facts?.let { _state.update { s -> s.copy(facts = it) } }
            saved.currentBranchId?.let { _state.update { s -> s.copy(currentBranchId = it) } }
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
        val branchId = _state.value.currentBranchId
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
                conversationSummary = _state.value.conversationSummary,
                strategy = _state.value.contextStrategy,
                facts = _state.value.facts,
            )

            val accumulated = StringBuilder()
            var tokenCount = 0
            val timeMark = TimeSource.Monotonic.markNow()
            var streamError = false

            log.i {
                "stream start: model=${currentSettings.model}, ctx=${context.size}, " +
                    "strategy=${_state.value.contextStrategy.key}, branch=$branchId"
            }
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
                historyDataSource.saveMessage(userMessage, branchId)
                historyDataSource.saveMessage(assistantMessage, branchId)
                log.i { "stream done: tokens=$tokenCount, durMs=$durationMs" }

                if (_state.value.contextStrategy == ContextStrategy.STICKY_FACTS) {
                    refreshFactsInBackground()
                }
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
            CommandResult.CompactRequested -> compactHistory()
            is CommandResult.StrategyChanged -> {
                changeContextStrategy(result.strategy)
                postSystemMessage(getString(Res.string.cmd_strategy_set, result.strategy.key))
            }
            CommandResult.StrategyHint -> postSystemMessage(getString(Res.string.cmd_strategy_hint))
            is CommandResult.BranchCreateRequested -> createBranch(result.name)
            is CommandResult.BranchSwitchRequested -> switchBranchByTarget(result.target)
            CommandResult.BranchesListRequested -> postBranchesList()
            CommandResult.BranchHint -> postSystemMessage(getString(Res.string.cmd_branch_hint))
            CommandResult.SwitchHint -> postSystemMessage(getString(Res.string.cmd_switch_hint))
        }
    }

    private fun postSystemMessage(text: String) {
        _state.update { state ->
            state.copy(messages = state.messages + ChatMessage(ChatRole.System, text))
        }
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingModels = true) }
            when (val result = repository.listModels()) {
                is Result.Success -> {
                    val models = result.data
                    if (models.isNotEmpty()) {
                        _state.update { it.copy(availableModels = models) }
                        log.i { "loaded ${models.size} models from provider" }
                        sanitizeCurrentModel(models)
                    }
                }
                is Result.Failure -> {
                    log.w { "models load failed: ${result.error} (using fallback)" }
                }
            }
            _state.update { it.copy(isLoadingModels = false) }
        }
    }

    /**
     * После загрузки списка моделей с провайдера: если текущая модель отсутствует в списке
     * (например, сохранена в DataStore от прежнего провайдера), переключаемся на baseSettings.model
     * либо на первую модель из списка.
     */
    private fun sanitizeCurrentModel(models: List<String>) {
        val current = _state.value.currentModel
        if (current.isNotBlank() && current in models) return
        val fallback = if (baseSettings.model in models) baseSettings.model else models.first()
        log.w { "current model '$current' not available; switching to '$fallback'" }
        selectModel(fallback)
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

    private fun updateMaxTokens(count: Int?) {
        currentSettings = currentSettings.copy(maxTokens = count)
        _state.update { it.copy(maxTokens = count) }
        viewModelScope.launch { settingsStorage.saveMaxTokens(count) }
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

    private fun changeContextStrategy(strategy: ContextStrategy) {
        if (_state.value.contextStrategy == strategy) return
        _state.update { it.copy(contextStrategy = strategy) }
        viewModelScope.launch { settingsStorage.saveContextStrategy(strategy) }
    }

    private fun clearFacts() {
        _state.update { it.copy(facts = emptyList()) }
        viewModelScope.launch { settingsStorage.saveFacts(emptyList()) }
    }

    private fun createBranch(name: String) {
        val cleanName = name.trim().ifBlank { return }
        viewModelScope.launch {
            val current = _state.value.branches.firstOrNull { it.id == _state.value.currentBranchId }
                ?: historyDataSource.ensureMainBranch()
            val newBranch = historyDataSource.createBranchFrom(current, cleanName)
            val branches = historyDataSource.getBranches()
            val newMessages = historyDataSource.getMessages(newBranch.id)
            _state.update {
                it.copy(
                    branches = branches,
                    currentBranchId = newBranch.id,
                    messages = newMessages,
                    error = null,
                )
            }
            settingsStorage.saveCurrentBranchId(newBranch.id)
            postSystemMessage(getString(Res.string.cmd_branch_created, newBranch.name))
        }
    }

    private fun switchBranch(branchId: Long) {
        if (branchId == _state.value.currentBranchId) return
        viewModelScope.launch {
            val branch = _state.value.branches.firstOrNull { it.id == branchId } ?: return@launch
            val messages = historyDataSource.getMessages(branchId)
            _state.update {
                it.copy(
                    currentBranchId = branchId,
                    messages = messages,
                    error = null,
                )
            }
            settingsStorage.saveCurrentBranchId(branchId)
            postSystemMessage(getString(Res.string.cmd_branch_switched, branch.name))
        }
    }

    private suspend fun switchBranchByTarget(target: String) {
        val byId = target.toLongOrNull()
        val branch = _state.value.branches.firstOrNull {
            it.id == byId || it.name.equals(target, ignoreCase = true)
        }
        if (branch == null) {
            postSystemMessage(getString(Res.string.cmd_switch_unknown, target))
            return
        }
        switchBranch(branch.id)
    }

    private fun deleteBranch(branchId: Long) {
        if (branchId == MAIN_BRANCH_ID) return
        viewModelScope.launch {
            historyDataSource.deleteBranch(branchId)
            val branches = historyDataSource.getBranches()
            val needSwitch = _state.value.currentBranchId == branchId
            val nextBranchId = if (needSwitch) MAIN_BRANCH_ID else _state.value.currentBranchId
            val messages = if (needSwitch) historyDataSource.getMessages(MAIN_BRANCH_ID)
            else _state.value.messages
            _state.update {
                it.copy(
                    branches = branches,
                    currentBranchId = nextBranchId,
                    messages = messages,
                )
            }
            if (needSwitch) settingsStorage.saveCurrentBranchId(MAIN_BRANCH_ID)
        }
    }

    private suspend fun postBranchesList() {
        val branches = _state.value.branches
        val current = _state.value.currentBranchId
        val rendered = branches.joinToString("\n") { b ->
            val marker = if (b.id == current) "→" else " "
            "$marker ${b.id}: ${b.name}"
        }
        postSystemMessage(getString(Res.string.cmd_branches_list, rendered))
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
                    maxTokens = null,
                    conversationSummary = null,
                    showStatistics = false,
                    contextStrategy = ContextStrategy.SLIDING_WINDOW,
                    facts = emptyList(),
                )
            }
        }
    }

    private suspend fun compactHistory() {
        val branchId = _state.value.currentBranchId
        val messages = _state.value.messages.filter { it.role != ChatRole.System }
        if (messages.isEmpty()) return

        postSystemMessage(getString(Res.string.cmd_compact_start))
        _state.update { it.copy(isLoading = true) }

        val compactPrompt = buildList {
            add(ChatMessage(ChatRole.System, COMPACT_SYSTEM_PROMPT))
            addAll(messages)
        }

        val summary = StringBuilder()
        var failed = false

        repository.streamMessage(compactPrompt, currentSettings).collect { result ->
            when (result) {
                is Result.Failure -> {
                    failed = true
                    log.w { "compact failed: ${result.error}" }
                }
                is Result.Success -> {
                    val token = result.data
                    if (!token.isThinking && token.text.isNotEmpty()) summary.append(token.text)
                }
            }
        }

        _state.update { it.copy(isLoading = false) }

        if (failed || summary.isBlank()) {
            postSystemMessage(getString(Res.string.cmd_compact_failed))
            return
        }

        val summaryText = summary.toString()
        _state.update { it.copy(messages = emptyList(), conversationSummary = summaryText) }
        viewModelScope.launch {
            historyDataSource.clearAll(branchId)
            settingsStorage.saveConversationSummary(summaryText)
        }
        postSystemMessage(getString(Res.string.cmd_compact_done))
        log.i { "history compacted: ${summaryText.length} chars (branch=$branchId)" }
    }

    private fun refreshFactsInBackground() {
        if (_state.value.isUpdatingFacts) return
        _state.update { it.copy(isUpdatingFacts = true) }
        viewModelScope.launch {
            try {
                val recent = _state.value.messages
                    .filter { it.role != ChatRole.System }
                    .takeLast(FACTS_RECENT_WINDOW)
                val factsSettings = currentSettings.copy(thinkingBudget = null, maxTokens = null)
                val updated = updateFacts(_state.value.facts, recent, factsSettings)
                if (updated != _state.value.facts) {
                    _state.update { it.copy(facts = updated) }
                    settingsStorage.saveFacts(updated)
                    log.i { "facts updated: ${updated.size} items" }
                }
            } catch (e: Exception) {
                log.w(e) { "facts refresh failed" }
            } finally {
                _state.update { it.copy(isUpdatingFacts = false) }
            }
        }
    }

    private fun stopStreaming() {
        streamingJob?.cancel()
        _state.update { it.copy(isLoading = false) }
    }

    private fun clearHistory() {
        stopStreaming()
        val branchId = _state.value.currentBranchId
        viewModelScope.launch {
            historyDataSource.clearAll(branchId)
            settingsStorage.saveConversationSummary(null)
            settingsStorage.saveFacts(emptyList())
        }
        _state.update {
            it.copy(
                messages = emptyList(),
                conversationSummary = null,
                facts = emptyList(),
                error = null,
                lastUsage = null,
                sessionTotalTokens = 0,
            )
        }
    }
}

