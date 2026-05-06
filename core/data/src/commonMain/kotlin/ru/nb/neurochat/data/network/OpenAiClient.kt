package ru.nb.neurochat.data.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.readLineStrict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.nb.neurochat.data.model.ChatCompletionResponse
import ru.nb.neurochat.data.model.ChatRequest
import ru.nb.neurochat.data.model.ChatStreamChunk
import ru.nb.neurochat.data.model.MessageDto
import ru.nb.neurochat.data.model.ModelsListResponse
import ru.nb.neurochat.data.model.StreamErrorChunk
import ru.nb.neurochat.data.model.StreamOptions
import ru.nb.neurochat.data.model.ThinkingConfig
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.StreamToken
import ru.nb.neurochat.domain.model.TokenUsage
import ru.nb.neurochat.domain.util.DataError

private const val CONNECT_TIMEOUT_MS = 30_000L
private const val THINKING_TEMPERATURE = 1.0
private const val SSE_DATA_PREFIX = "data: "
private const val SSE_DONE_MARKER = "[DONE]"

/** OpenAI-совместимый клиент. Работает с любыми провайдерами, поддерживающими REST /chat/completions
 * с SSE-стримингом (OpenAI, LiteLLM, OpenRouter, локальный llama.cpp и т.д.).
 * @param baseSettings настройки API (baseUrl, apiKey, timeoutSeconds), считываются из BuildKonfig
 */
internal class OpenAiClient(private val baseSettings: ApiSettings) {

    private val log = Logger.withTag("OpenAiClient")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = baseSettings.timeoutSeconds * 1_000
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = baseSettings.timeoutSeconds * 1_000
        }
        install(DefaultRequest) {
            header(HttpHeaders.Authorization, "Bearer ${baseSettings.apiKey}")
        }
        install(Logging) {
            level = LogLevel.HEADERS
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    log.d { message }
                }
            }
        }
        expectSuccess = false
    }

    /**
     * GET /models — список доступных моделей у текущего провайдера (OpenAI-совместимый ответ).
     * Бросает [ApiException] при HTTP-ошибке.
     */
    suspend fun listModels(): List<String> {
        val response = client.get("${baseSettings.baseUrl}/models")
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            log.w { "GET /models HTTP ${response.status.value}: ${body.take(200)}" }
            throw ApiException(
                dataError = response.status.toDataError(),
                providerMessage = body.take(500).takeIf { it.isNotBlank() },
            )
        }
        val text = response.bodyAsText()
        return json.decodeFromString<ModelsListResponse>(text)
            .data
            .map { it.id }
            .filter { it.isNotBlank() }
            .sorted()
    }

    /**
     * Стриминг ответа модели. Бросает [ApiException] при HTTP/SSE ошибке —
     * вышестоящий ChatRepository мапит её в Result.Failure(DataError).
     */
    fun chatStream(messages: List<MessageDto>, settings: ApiSettings): Flow<StreamToken> =
        channelFlow {
            log.i { "→ ${baseSettings.baseUrl}, model=${settings.model}" }

            val requestBody = buildRequestBody(messages, settings)

            client.preparePost("${baseSettings.baseUrl}/chat/completions") {
                setBody(TextContent(requestBody, ContentType.Application.Json))
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    log.w { "HTTP ${response.status.value}: $body" }
                    throw ApiException(
                        dataError = response.status.toDataError(),
                        providerMessage = body.take(500).takeIf { it.isNotBlank() },
                    )
                }

                val ct = response.headers["Content-Type"] ?: ""
                if (!ct.contains("event-stream")) {
                    handleNonStreamingResponse(response.bodyAsText())
                    return@execute
                }

                consumeSseStream(response.bodyAsChannel())
            }
        }

    private fun buildRequestBody(messages: List<MessageDto>, settings: ApiSettings): String {
        // Anthropic-extension: thinking требует temperature=1.0.
        val thinking: ThinkingConfig? = settings.thinkingBudget?.let {
            ThinkingConfig(type = "enabled", budgetTokens = it)
        }
        val temperature = if (thinking != null) THINKING_TEMPERATURE else settings.temperature

        // Anthropic: max_tokens должен быть строго больше budget_tokens.
        val requestedMaxTokens = settings.maxTokens
        val maxTokens = if (thinking != null && requestedMaxTokens != null)
            maxOf(requestedMaxTokens, thinking.budgetTokens + 1)
        else
            requestedMaxTokens

        return json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(
                model = settings.model,
                messages = messages,
                temperature = temperature,
                stream = true,
                thinking = thinking,
                streamOptions = StreamOptions(),
                maxTokens = maxTokens,
            )
        )
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<StreamToken>.handleNonStreamingResponse(
        body: String,
    ) {
        val completion = decodeOrNull<ChatCompletionResponse>(body, "non-stream completion")
        val content = completion?.choices?.firstOrNull()?.message?.content
        if (content != null) {
            send(StreamToken(content, isThinking = false))
            return
        }

        val error = decodeOrNull<StreamErrorChunk>(body, "non-stream error")?.error
        if (error != null) {
            // HTTP 200 + JSON `{"error":...}` без `choices` — провайдер вернул валидационную
            // ошибку (модель не найдена, неверный параметр и т.п.). Это семантика 4xx,
            // мапим в BAD_REQUEST, а не UNKNOWN.
            throw ApiException(
                dataError = DataError.Remote.BAD_REQUEST,
                providerMessage = error.message,
            )
        }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<StreamToken>.consumeSseStream(
        channel: io.ktor.utils.io.ByteReadChannel,
    ) {
        var tokenCount = 0
        while (!channel.isClosedForRead) {
            val line = channel.readLineStrict() ?: break
            if (!line.startsWith(SSE_DATA_PREFIX)) continue
            val data = line.removePrefix(SSE_DATA_PREFIX).trim()
            if (data == SSE_DONE_MARKER) break

            val chunk = decodeOrNull<ChatStreamChunk>(data, "stream chunk")
            if (chunk != null) {
                tokenCount += emitChunkTokens(chunk)
                continue
            }

            // Если не chunk — пробуем как ошибку от провайдера.
            val streamError = decodeOrNull<StreamErrorChunk>(data, "stream error")?.error
            if (streamError != null) {
                throw ApiException(
                    dataError = DataError.Remote.UNKNOWN,
                    providerMessage = streamError.message,
                )
            }
        }
        log.i { "SSE done, tokens=$tokenCount" }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<StreamToken>.emitChunkTokens(
        chunk: ChatStreamChunk,
    ): Int {
        var emitted = 0
        val delta = chunk.choices.firstOrNull()?.delta
        delta?.reasoningContent?.let {
            send(StreamToken(it, isThinking = true))
            emitted++
        }
        delta?.content?.let {
            send(StreamToken(it, isThinking = false))
            emitted++
        }
        chunk.usage?.let { u ->
            send(
                StreamToken(
                    text = "",
                    usage = TokenUsage(
                        promptTokens = u.promptTokens,
                        completionTokens = u.completionTokens,
                        totalTokens = u.totalTokens,
                    ),
                )
            )
        }
        return emitted
    }

    private inline fun <reified T> decodeOrNull(raw: String, label: String): T? = try {
        json.decodeFromString<T>(raw)
    } catch (e: SerializationException) {
        log.v { "skip $label: ${e.message}; raw=${raw.take(200)}" }
        null
    }

    fun close() = client.close()
}
