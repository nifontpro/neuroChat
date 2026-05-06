package ru.nb.neurochat.chat.presentation

import ru.nb.neurochat.domain.model.ContextStrategy

sealed interface ChatAction {
    data class OnInputChange(val text: String) : ChatAction
    data object OnSendMessage : ChatAction
    data object OnStopStreaming : ChatAction
    data object OnClearHistory : ChatAction
    data object OnSettingsClick : ChatAction
    data object OnDismissSettings : ChatAction

    // Settings actions
    data class OnSelectModel(val model: String) : ChatAction
    data class OnTemperatureChange(val temperature: Double?) : ChatAction
    data class OnThinkingToggle(val enabled: Boolean) : ChatAction
    data class OnSystemPromptChange(val prompt: String?) : ChatAction
    data class OnMaxContextChange(val count: Int) : ChatAction
    data class OnMaxTokensChange(val count: Int?) : ChatAction
    data class OnToggleStatistics(val enabled: Boolean) : ChatAction
    data object OnResetSettings : ChatAction

    // Context strategy
    data class OnContextStrategyChange(val strategy: ContextStrategy) : ChatAction

    // Sticky Facts
    data object OnClearFacts : ChatAction

    // Branching
    data class OnCreateBranch(val name: String) : ChatAction
    data class OnSwitchBranch(val branchId: Long) : ChatAction
    data class OnDeleteBranch(val branchId: Long) : ChatAction
}
