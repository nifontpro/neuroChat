package ru.nb.neurochat.chat.presentation.util

import org.jetbrains.compose.resources.StringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.error_bad_request
import ru.nb.neurochat.chat.presentation.generated.resources.error_forbidden
import ru.nb.neurochat.chat.presentation.generated.resources.error_no_internet
import ru.nb.neurochat.chat.presentation.generated.resources.error_not_connected
import ru.nb.neurochat.chat.presentation.generated.resources.error_not_found
import ru.nb.neurochat.chat.presentation.generated.resources.error_request_timeout
import ru.nb.neurochat.chat.presentation.generated.resources.error_serialization
import ru.nb.neurochat.chat.presentation.generated.resources.error_server
import ru.nb.neurochat.chat.presentation.generated.resources.error_service_unavailable
import ru.nb.neurochat.chat.presentation.generated.resources.error_too_many_requests
import ru.nb.neurochat.chat.presentation.generated.resources.error_unauthorized
import ru.nb.neurochat.chat.presentation.generated.resources.error_unknown
import ru.nb.neurochat.chat.presentation.generated.resources.error_upstream_down
import ru.nb.neurochat.domain.util.DataError

internal fun DataError.toMessageRes(): StringResource = when (this) {
    DataError.Remote.BAD_REQUEST -> Res.string.error_bad_request
    DataError.Remote.UNAUTHORIZED -> Res.string.error_unauthorized
    DataError.Remote.FORBIDDEN -> Res.string.error_forbidden
    DataError.Remote.NOT_FOUND -> Res.string.error_not_found
    DataError.Remote.REQUEST_TIMEOUT -> Res.string.error_request_timeout
    DataError.Remote.TOO_MANY_REQUESTS -> Res.string.error_too_many_requests
    DataError.Remote.NO_INTERNET -> Res.string.error_no_internet
    DataError.Remote.SERVER_ERROR -> Res.string.error_server
    DataError.Remote.SERVICE_UNAVAILABLE -> Res.string.error_service_unavailable
    DataError.Remote.UPSTREAM_DOWN -> Res.string.error_upstream_down
    DataError.Remote.SERIALIZATION -> Res.string.error_serialization
    DataError.Remote.UNKNOWN -> Res.string.error_unknown
    DataError.Connection.NOT_CONNECTED -> Res.string.error_not_connected
}
