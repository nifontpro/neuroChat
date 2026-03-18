package ru.nb.neurochat.network.repository

import kotlinx.coroutines.flow.Flow
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.StreamToken
import ru.nb.neurochat.domain.repository.IChatRepository
import ru.nb.neurochat.network.OpenAiClient
import ru.nb.neurochat.network.model.MessageDto

internal class ChatRepository(private val client: OpenAiClient) : IChatRepository {

    override fun streamMessage(history: List<ChatMessage>, settings: ApiSettings): Flow<StreamToken> =
        client.chatStream(history.map { MessageDto(it.role.value, it.content) }, settings)
}
