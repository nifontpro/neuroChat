package ru.nb.neurochat.domain.datasource

import ru.nb.neurochat.domain.model.ChatMessage

interface IChatHistoryDataSource {
    suspend fun getMessages(): List<ChatMessage>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun clearAll()
}
