package ru.nb.neurochat.data.db

import co.touchlab.kermit.Logger
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource.Companion.MAIN_BRANCH_ID
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource.Companion.MAIN_BRANCH_NAME
import ru.nb.neurochat.domain.model.Branch
import ru.nb.neurochat.domain.model.ChatMessage

/**
 * Реализация [IChatHistoryDataSource] поверх Room. Сообщения и ветки хранятся в одной БД,
 * связь по [ChatMessageEntity.branchId]. Главная ветка («main») создаётся при старте,
 * её id фиксирован константой [MAIN_BRANCH_ID].
 */
@OptIn(ExperimentalTime::class)
class RoomChatHistoryDataSource(
    private val messageDao: ChatMessageDao,
    private val branchDao: BranchDao,
) : IChatHistoryDataSource {

    private val log = Logger.withTag("ChatHistory")

    override suspend fun getMessages(branchId: Long): List<ChatMessage> = try {
        messageDao.getAll(branchId).map { it.toChatMessage() }
    } catch (e: Exception) {
        log.e(e) { "getMessages failed (branchId=$branchId)" }
        emptyList()
    }

    override suspend fun saveMessage(message: ChatMessage, branchId: Long) {
        try {
            messageDao.insert(message.toEntity(branchId))
        } catch (e: Exception) {
            log.e(e) { "saveMessage failed: role=${message.role} branchId=$branchId" }
        }
    }

    override suspend fun clearAll(branchId: Long) {
        try {
            messageDao.deleteAllInBranch(branchId)
            log.i { "branch $branchId cleared" }
        } catch (e: Exception) {
            log.e(e) { "clearAll failed (branchId=$branchId)" }
        }
    }

    override suspend fun getBranches(): List<Branch> = try {
        branchDao.getAll().map { it.toBranch() }
    } catch (e: Exception) {
        log.e(e) { "getBranches failed" }
        emptyList()
    }

    override suspend fun ensureMainBranch(): Branch {
        val existing = branchDao.findById(MAIN_BRANCH_ID)
        if (existing != null) return existing.toBranch()
        val entity = BranchEntity(
            id = MAIN_BRANCH_ID,
            name = MAIN_BRANCH_NAME,
            parentBranchId = null,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        branchDao.insert(entity)
        log.i { "main branch created" }
        return entity.toBranch()
    }

    override suspend fun createBranchFrom(parent: Branch, newName: String): Branch {
        val newEntity = BranchEntity(
            name = newName,
            parentBranchId = parent.id,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        val newId = branchDao.insert(newEntity)
        // копируем все сообщения родителя в новую ветку
        val parentMessages = messageDao.getAll(parent.id)
        if (parentMessages.isNotEmpty()) {
            val cloned = parentMessages.map { it.copy(id = 0, branchId = newId) }
            messageDao.insertAll(cloned)
        }
        log.i { "branch '$newName' (id=$newId) forked from ${parent.id}, ${parentMessages.size} msgs copied" }
        return Branch(
            id = newId,
            name = newName,
            parentBranchId = parent.id,
            createdAt = newEntity.createdAt,
        )
    }

    override suspend fun renameBranch(branchId: Long, newName: String) {
        val entity = branchDao.findById(branchId) ?: return
        branchDao.update(entity.copy(name = newName))
    }

    override suspend fun deleteBranch(branchId: Long) {
        if (branchId == MAIN_BRANCH_ID) return
        try {
            messageDao.deleteAllInBranch(branchId)
            branchDao.deleteById(branchId)
            log.i { "branch $branchId deleted" }
        } catch (e: Exception) {
            log.e(e) { "deleteBranch failed (id=$branchId)" }
        }
    }
}
