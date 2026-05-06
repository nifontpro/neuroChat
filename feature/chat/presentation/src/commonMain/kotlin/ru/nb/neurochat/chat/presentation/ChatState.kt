package ru.nb.neurochat.chat.presentation

import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource.Companion.MAIN_BRANCH_ID
import ru.nb.neurochat.domain.model.Branch
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ContextStrategy
import ru.nb.neurochat.domain.model.Fact
import ru.nb.neurochat.domain.model.TokenUsage
import ru.nb.neurochat.domain.util.DataError

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: DataError? = null,
    val currentModel: String = "",
    val currentTemperature: Double? = null,
    val thinkingEnabled: Boolean = false,
    val systemPrompt: String? = null,
    val maxContextMessages: Int = 0,
    val maxTokens: Int? = null,
    val conversationSummary: String? = null,
    val isConnected: Boolean = true,
    val isSettingsOpen: Boolean = false,
    val showStatistics: Boolean = false,
    val lastUsage: TokenUsage? = null,
    val sessionTotalTokens: Int = 0,

    val contextStrategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW,
    val facts: List<Fact> = emptyList(),
    val isUpdatingFacts: Boolean = false,

    val branches: List<Branch> = emptyList(),
    val currentBranchId: Long = MAIN_BRANCH_ID,

    val availableModels: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
)
