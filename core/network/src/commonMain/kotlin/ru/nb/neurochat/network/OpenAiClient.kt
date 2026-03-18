package ru.nb.neurochat.network

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
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.StreamToken
import ru.nb.neurochat.network.model.ChatCompletionResponse
import ru.nb.neurochat.network.model.ChatRequest
import ru.nb.neurochat.network.model.ChatStreamChunk
import ru.nb.neurochat.network.model.MessageDto
import ru.nb.neurochat.network.model.StreamErrorChunk
import ru.nb.neurochat.network.model.ThinkingConfig

// baseUrl и apiKey фиксированы при создании клиента.
// model, temperature, thinkingBudget передаются per-call через ApiSettings.
internal class OpenAiClient(private val baseSettings: ApiSettings) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    // Без ContentNegotiation — он ставит Accept: application/json,
    // из-за чего LiteLLM прокси отвечает JSON вместо SSE стрима.
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
                    println("[NeuroChat HTTP] $message")
                }
            }
        }
        expectSuccess = false
    }

    fun chatStream(messages: List<MessageDto>, settings: ApiSettings): Flow<StreamToken> = channelFlow {
        println("[HTTP] → ${baseSettings.baseUrl}, model=${settings.model}")

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
            )
        )

        println("[HTTP] body: $requestBody")
        client.preparePost("${baseSettings.baseUrl}/chat/completions") {
            setBody(TextContent(requestBody, ContentType.Application.Json))
        }.execute { response ->
            println("[HTTP] status=${response.status.value}, content-type=${response.headers["Content-Type"]}")
            if (!response.status.isSuccess()) {
                throw IllegalStateException("HTTP ${response.status.value}: ${response.bodyAsText()}")
            }

            val ct = response.headers["Content-Type"] ?: ""
            if (!ct.contains("event-stream")) {
                println("[HTTP] NON-STREAM response ($ct)")
                val body = response.bodyAsText()
                val completion = runCatching { json.decodeFromString<ChatCompletionResponse>(body) }.getOrNull()
                val content = completion?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    send(StreamToken(content, isThinking = false))
                } else {
                    val error = runCatching { json.decodeFromString<StreamErrorChunk>(body) }.getOrNull()?.error
                    if (error != null) throw IllegalStateException(error.message)
                }
                return@execute
            }

            // SSE стрим
            println("[HTTP] SSE STREAM started")
            var tokenCount = 0
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                val chunk = runCatching { json.decodeFromString<ChatStreamChunk>(data) }.getOrNull()
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
                    continue
                }

                val streamError = runCatching { json.decodeFromString<StreamErrorChunk>(data) }.getOrNull()?.error
                if (streamError != null) throw IllegalStateException(streamError.message)
            }
            println("[HTTP] SSE STREAM done, tokens=$tokenCount")
        }
    }

    fun close() = client.close()
}
