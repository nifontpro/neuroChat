package ru.nb.neurochat.data.db

import co.touchlab.kermit.Logger
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource
import ru.nb.neurochat.domain.model.ChatMessage

class RoomChatHistoryDataSource(private val dao: ChatMessageDao) : IChatHistoryDataSource {

    private val log = Logger.withTag("ChatHistory")

    override suspend fun getMessages(): List<ChatMessage> = try {
        dao.getAll().map { it.toChatMessage() }
    } catch (e: Exception) {
        log.e(e) { "getMessages failed" }
        emptyList()
    }

    override suspend fun saveMessage(message: ChatMessage) {
        try {
            dao.insert(message.toEntity())
        } catch (e: Exception) {
            log.e(e) { "saveMessage failed: role=${message.role}" }
        }
    }

    override suspend fun clearAll() {
        try {
            dao.deleteAll()
            log.i { "history cleared" }
        } catch (e: Exception) {
            log.e(e) { "clearAll failed" }
        }
    }
}
