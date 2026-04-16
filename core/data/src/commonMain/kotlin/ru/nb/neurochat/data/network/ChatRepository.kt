package ru.nb.neurochat.data.network

import kotlinx.coroutines.flow.Flow
import ru.nb.neurochat.data.model.MessageDto
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.StreamToken
import ru.nb.neurochat.domain.repository.IChatRepository

// Реализация доменного репозитория: мапит доменные ChatMessage в DTO и проксирует стрим в клиент.
// Здесь нет бизнес-логики — весь контракт определён в OpenAiClient.
internal class ChatRepository(private val client: OpenAiClient) : IChatRepository {

    override fun streamMessage(
        history: List<ChatMessage>,
        settings: ApiSettings,
    ): Flow<StreamToken> =
        client.chatStream(history.map { MessageDto(it.role.value, it.content) }, settings)
}
