package ru.nb.neurochat.data.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import ru.nb.neurochat.domain.util.DataError

internal fun HttpStatusCode.toDataError(): DataError.Remote = when (value) {
    400 -> DataError.Remote.BAD_REQUEST
    401 -> DataError.Remote.UNAUTHORIZED
    403 -> DataError.Remote.FORBIDDEN
    404 -> DataError.Remote.NOT_FOUND
    408 -> DataError.Remote.REQUEST_TIMEOUT
    429 -> DataError.Remote.TOO_MANY_REQUESTS
    503 -> DataError.Remote.SERVICE_UNAVAILABLE
    else -> when {
        // Cloudflare-specific: 520 Unknown error, 521 Web server is down,
        // 522 Connection timed out, 523 Origin unreachable, 524 A timeout occurred,
        // 525-527 — TLS/Railgun ошибки между CF и origin.
        // Все они означают «прокси на месте, origin не доступен или не отвечает».
        value in 520..527 -> DataError.Remote.UPSTREAM_DOWN
        value in 500..599 -> DataError.Remote.SERVER_ERROR
        else -> DataError.Remote.UNKNOWN
    }
}

internal fun Throwable.toDataError(): DataError.Remote = when (this) {
    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException -> DataError.Remote.REQUEST_TIMEOUT

    is SerializationException -> DataError.Remote.SERIALIZATION

    else -> {
        val msg = message?.lowercase().orEmpty()
        when {
            "unresolved" in msg ||
                "host" in msg ||
                "network" in msg ||
                "connect" in msg -> DataError.Remote.NO_INTERNET

            else -> DataError.Remote.UNKNOWN
        }
    }
}

/**
 * Исключение, бросаемое из OpenAiClient при HTTP/SSE ошибке.
 * Содержит типизированный DataError + опциональное message от провайдера.
 */
internal class ApiException(
    val dataError: DataError.Remote,
    val providerMessage: String? = null,
) : RuntimeException(providerMessage ?: dataError.name)
