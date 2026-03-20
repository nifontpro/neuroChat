package ru.nb.neurochat.chat.presentation

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
}
