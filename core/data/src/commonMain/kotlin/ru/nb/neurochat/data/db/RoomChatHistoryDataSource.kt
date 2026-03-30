package ru.nb.neurochat.data.db

import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource
import ru.nb.neurochat.domain.model.ChatMessage

class RoomChatHistoryDataSource(private val dao: ChatMessageDao) : IChatHistoryDataSource {

    override suspend fun getMessages(): List<ChatMessage> =
        dao.getAll().map { it.toChatMessage() }

    override suspend fun saveMessage(message: ChatMessage) {
        dao.insert(message.toEntity())
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }
}
