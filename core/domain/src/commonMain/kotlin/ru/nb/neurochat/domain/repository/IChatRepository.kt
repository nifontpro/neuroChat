package ru.nb.neurochat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.StreamToken

interface IChatRepository {
    fun streamMessage(history: List<ChatMessage>, settings: ApiSettings): Flow<StreamToken>
}
