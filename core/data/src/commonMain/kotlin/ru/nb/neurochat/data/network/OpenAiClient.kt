package ru.nb.neurochat.data.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
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
import kotlinx.serialization.json.Json
import ru.nb.neurochat.data.model.ChatCompletionResponse
import ru.nb.neurochat.data.model.ChatRequest
import ru.nb.neurochat.data.model.ChatStreamChunk
import ru.nb.neurochat.data.model.MessageDto
import ru.nb.neurochat.data.model.StreamErrorChunk
import ru.nb.neurochat.data.model.StreamOptions
import ru.nb.neurochat.data.model.ThinkingConfig
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.StreamToken
import ru.nb.neurochat.domain.model.TokenUsage

// OpenAI-совместимый клиент. Работает с любыми провайдерами, поддерживающими REST /chat/completions
// с SSE-стримингом (OpenAI, LiteLLM, OpenRouter, локальный llama.cpp и т.д.).
// baseSettings задаются один раз на уровне DI (URL, ключ, таймаут). Конкретные настройки запроса
// (модель, температура, thinking) передаются в chatStream(settings) — могут меняться динамически.
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
            connectTimeoutMillis = 30_000
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

    // Стриминг ответа модели. Возвращает Flow<StreamToken>, где каждый токен содержит
    // либо content/reasoningContent, либо финальный usage (от провайдера, поле usage в SSE).
    // channelFlow — чтобы можно было суспендить send() и прозрачно отменяться при cancel из VM.
    fun chatStream(messages: List<MessageDto>, settings: ApiSettings): Flow<StreamToken> =
        channelFlow {
            log.i { "→ ${baseSettings.baseUrl}, model=${settings.model}" }

            // Anthropic-extension: thinking требует temperature=1.0. Для обычного запроса
            // используем пользовательскую (может быть null — тогда провайдер берёт дефолт).
            val thinking: ThinkingConfig? = settings.thinkingBudget?.let {
                ThinkingConfig(type = "enabled", budgetTokens = it)
            }
            val temperature = if (thinking != null) 1.0 else settings.temperature

            val requestBody = json.encodeToString(
                ChatRequest.serializer(),
                ChatRequest(
                    model = settings.model,
                    messages = messages,
                    temperature = temperature,
                    stream = true,
                    thinking = thinking,
                    streamOptions = StreamOptions(),
                )
            )

            client.preparePost("${baseSettings.baseUrl}/chat/completions") {
                setBody(TextContent(requestBody, ContentType.Application.Json))
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw IllegalStateException("HTTP ${response.status.value}: ${response.bodyAsText()}")
                }

                // Некоторые провайдеры на ошибке/отсутствии стриминга возвращают обычный JSON —
                // обрабатываем этот fallback как одноразовый токен.
                val ct = response.headers["Content-Type"] ?: ""
                if (!ct.contains("event-stream")) {
                    val body = response.bodyAsText()
                    val completion =
                        runCatching { json.decodeFromString<ChatCompletionResponse>(body) }.getOrNull()
                    val content = completion?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        send(StreamToken(content, isThinking = false))
                    } else {
                        val error =
                            runCatching { json.decodeFromString<StreamErrorChunk>(body) }.getOrNull()?.error
                        if (error != null) throw IllegalStateException(error.message)
                    }
                    return@execute
                }

                // Читаем SSE построчно. Формат: "data: {json}\n\n", финальный маркер "data: [DONE]".
                var tokenCount = 0
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readLineStrict() ?: break
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    val chunk =
                        runCatching { json.decodeFromString<ChatStreamChunk>(data) }.getOrNull()
                    if (chunk != null) {
                        val delta = chunk.choices.firstOrNull()?.delta
                        delta?.reasoningContent?.let {
                            send(StreamToken(it, isThinking = true))
                            tokenCount++
                        }
                        delta?.content?.let {
                            send(StreamToken(it, isThinking = false))
                            tokenCount++
                        }
                        chunk.usage?.let { u ->
                            send(StreamToken(
                                text = "",
                                usage = TokenUsage(
                                    promptTokens = u.promptTokens,
                                    completionTokens = u.completionTokens,
                                    totalTokens = u.totalTokens,
                                )
                            ))
                        }
                        continue
                    }

                    val streamError =
                        runCatching { json.decodeFromString<StreamErrorChunk>(data) }.getOrNull()?.error
                    if (streamError != null) throw IllegalStateException(streamError.message)
                }
                log.i { "SSE done, tokens=$tokenCount" }
            }
        }

    fun close() = client.close()
}
