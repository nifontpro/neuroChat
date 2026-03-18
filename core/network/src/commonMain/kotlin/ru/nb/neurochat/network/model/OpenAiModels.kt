package ru.nb.neurochat.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Double? = null,
    val stream: Boolean = true,
    val thinking: ThinkingConfig? = null,
)

@Serializable
internal data class ThinkingConfig(
    val type: String,
    @SerialName("budget_tokens") val budgetTokens: Int,
)

@Serializable
internal data class MessageDto(
    val role: String,
    val content: String,
)

@Serializable
internal data class ChatStreamChunk(
    val choices: List<StreamChoice>,
)

@Serializable
internal data class StreamChoice(
    val delta: StreamDelta,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class StreamDelta(
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

// Полный (не-стриминговый) ответ: Content-Type: application/json
@Serializable
internal data class ChatCompletionResponse(
    val choices: List<CompletionChoice>,
)

@Serializable
internal data class CompletionChoice(
    val message: CompletionMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class CompletionMessage(
    val role: String,
    val content: String? = null,
)

// OpenAI возвращает ошибку в теле стрима: data: {"error": {...}}
@Serializable
internal data class StreamErrorChunk(
    val error: StreamError? = null,
)

@Serializable
internal data class StreamError(
    val message: String,
    val type: String? = null,
    val code: String? = null,
)
