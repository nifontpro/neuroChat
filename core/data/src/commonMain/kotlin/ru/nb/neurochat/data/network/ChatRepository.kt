package ru.nb.neurochat.data.network

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.nb.neurochat.data.model.MessageDto
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.StreamToken
import ru.nb.neurochat.domain.repository.IChatRepository
import ru.nb.neurochat.domain.util.DataError
import ru.nb.neurochat.domain.util.Result

// Реализация доменного репозитория: мапит доменные ChatMessage в DTO и оборачивает поток
// в Result, перехватывая исключения клиента и приводя их к DataError.
internal class ChatRepository(private val client: OpenAiClient) : IChatRepository {

    private val log = Logger.withTag("ChatRepository")

    override fun streamMessage(
        history: List<ChatMessage>,
        settings: ApiSettings,
    ): Flow<Result<StreamToken, DataError>> =
        client.chatStream(history.map { MessageDto(it.role.value, it.content) }, settings)
            .map<StreamToken, Result<StreamToken, DataError>> { Result.Success(it) }
            .catch { e ->
                val error = when (e) {
                    is ApiException -> e.dataError
                    else -> e.toDataError()
                }
                log.w(e) { "stream failed: $error" }
                emit(Result.Failure(error))
            }
}
