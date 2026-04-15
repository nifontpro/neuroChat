package ru.nb.neurochat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Double? = null,
    val stream: Boolean = true,
    val thinking: ThinkingConfig? = null,
    @SerialName("stream_options") val streamOptions: StreamOptions? = null,
)

@Serializable
internal data class StreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean = true,
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
    val usage: UsageDto? = null,
)

@Serializable
internal data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
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
