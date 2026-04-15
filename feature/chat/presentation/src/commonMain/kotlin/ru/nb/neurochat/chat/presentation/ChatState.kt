package ru.nb.neurochat.chat.presentation

import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.TokenUsage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentModel: String = "",
    val currentTemperature: Double? = null,
    val thinkingEnabled: Boolean = false,
    val systemPrompt: String? = null,
    val maxContextMessages: Int = 0,
    val isConnected: Boolean = true,
    val isSettingsOpen: Boolean = false,
    val showStatistics: Boolean = false,
    val lastUsage: TokenUsage? = null,
    val sessionTotalTokens: Int = 0,
)
