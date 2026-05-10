package ru.nb.neurochat.data.db

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import co.touchlab.kermit.Logger
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource.Companion.MAIN_BRANCH_ID
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource.Companion.MAIN_BRANCH_NAME
import ru.nb.neurochat.domain.model.Branch
import ru.nb.neurochat.domain.model.ChatMessage

/**
 * Реализация [IChatHistoryDataSource] поверх Room в ссылочной модели ветвления.
 *
 * Ветка хранит только свои собственные сообщения. История родителей не дублируется —
 * она наследуется через [BranchEntity.parentBranchId] + [BranchEntity.forkFromMessageId]
 * (точка отсечения: id последнего собственного сообщения родителя на момент создания).
 *
 * При чтении [getMessages] поднимается рекурсивно по цепочке предков; для каждой ветки
 * берутся только сообщения с id <= cutoff (cutoff — forkFromMessageId следующей ветки
 * вниз по цепочке к текущей). Результат сортируется по [ChatMessageEntity.createdAt].
 *
 * При очистке [clearAll] и удалении [deleteBranch] ветки, у которой есть потомки,
 * наследие сначала «материализуется» в потомков (физически копируется только видимая
 * им часть), после чего потомки перепривязываются к родителю удаляемой ветки.
 */
@OptIn(ExperimentalTime::class)
class RoomChatHistoryDataSource(
    private val database: NeuroChatDatabase,
    private val messageDao: ChatMessageDao,
    private val branchDao: BranchDao,
) : IChatHistoryDataSource {

    private val log = Logger.withTag("ChatHistory")

    override suspend fun getMessages(branchId: Long): List<ChatMessage> = try {
        val chain = buildAncestorChainTopDown(branchId)
        if (chain.isEmpty()) {
            emptyList()
        } else {
            val accumulated = mutableListOf<ChatMessageEntity>()
            for ((id, cutoff) in chain) {
                val slice = when {
                    cutoff == null -> messageDao.getAll(id)
                    cutoff <= 0L -> emptyList()
                    else -> messageDao.getUpTo(id, cutoff)
                }
                accumulated.addAll(slice)
            }
            // createdAt — основной хронологический ключ, id — стабильный tie-breaker
            accumulated.sortedWith(compareBy({ it.createdAt }, { it.id })).map { it.toChatMessage() }
        }
    } catch (e: Exception) {
        log.e(e) { "getMessages failed (branchId=$branchId)" }
        emptyList()
    }

    override suspend fun saveMessage(message: ChatMessage, branchId: Long) {
        try {
            // Защита инварианта: сообщение всегда привязано к существующей ветке.
            // Иначе можно «потерять» данные — записать в branchId без BranchEntity,
            // что нарушит логику цепочки наследования и дерева веток.
            if (branchDao.findById(branchId) == null) {
                log.e {
                    "saveMessage rejected: branchId=$branchId does not exist " +
                        "(role=${message.role}, content.len=${message.content.length})"
                }
                return
            }
            val createdAt = Clock.System.now().toEpochMilliseconds()
            messageDao.insert(message.toEntity(branchId, createdAt))
        } catch (e: Exception) {
            log.e(e) { "saveMessage failed: role=${message.role} branchId=$branchId" }
        }
    }

    override suspend fun clearAll(branchId: Long) {
        try {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val target = branchDao.findById(branchId) ?: return@immediateTransaction
                    // Перед удалением сообщений переселяем потомков, иначе они потеряют видимую часть.
                    val children = branchDao.findChildren(branchId)
                    for (child in children) {
                        materializeFromParentInto(child = child, parent = target)
                    }
                    messageDao.deleteAllInBranch(branchId)
                    // Ветка превращается в пустую root: чтобы /clear скрыл и унаследованную историю,
                    // отвязываем её от собственного предка.
                    if (target.parentBranchId != null || target.forkFromMessageId != null) {
                        branchDao.update(
                            target.copy(parentBranchId = null, forkFromMessageId = null)
                        )
                    }
                    log.i { "branch $branchId cleared (children migrated=${children.size})" }
                }
            }
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

    override suspend fun findBranch(id: Long): Branch? = try {
        branchDao.findById(id)?.toBranch()
    } catch (e: Exception) {
        log.e(e) { "findBranch failed (id=$id)" }
        null
    }

    override suspend fun ensureMainBranch(): Branch {
        val existing = branchDao.findById(MAIN_BRANCH_ID)
        if (existing != null) return existing.toBranch()
        val entity = BranchEntity(
            id = MAIN_BRANCH_ID,
            name = MAIN_BRANCH_NAME,
            parentBranchId = null,
            forkFromMessageId = null,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        branchDao.insert(entity)
        log.i { "main branch created" }
        return entity.toBranch()
    }

    override suspend fun createBranchFrom(parent: Branch, newName: String): Branch {
        // forkPoint — id последнего СОБСТВЕННОГО сообщения родителя на момент ответвления.
        // Предки родителя подцепятся автоматически через цепочку parentBranchId.
        // Используем 0L (не null) когда сообщений у родителя ещё нет: нужно явно
        // зафиксировать «нет унаследованных сообщений из этого звена», чтобы cutoff=null
        // не интерпретировался как «брать всё» при последующем чтении.
        val forkPoint = messageDao.maxOwnId(parent.id) ?: 0L
        val newEntity = BranchEntity(
            name = newName,
            parentBranchId = parent.id,
            forkFromMessageId = forkPoint,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        val newId = branchDao.insert(newEntity)
        log.i { "branch '$newName' (id=$newId) forked from ${parent.id} at msgId=$forkPoint" }
        return Branch(
            id = newId,
            name = newName,
            parentBranchId = parent.id,
            forkFromMessageId = forkPoint,
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
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val target = branchDao.findById(branchId) ?: return@immediateTransaction
                    val children = branchDao.findChildren(branchId)
                    for (child in children) {
                        materializeFromParentInto(child = child, parent = target)
                    }
                    messageDao.deleteAllInBranch(branchId)
                    branchDao.deleteById(branchId)
                    log.i { "branch $branchId deleted (children migrated=${children.size})" }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "deleteBranch failed (id=$branchId)" }
        }
    }

    /**
     * Переносит в [child] видимую ему часть собственных сообщений [parent] (id <= cutoff),
     * после чего перепривязывает [child] к предку [parent]. Используется при разрушении
     * звена в цепочке наследования (clearAll / deleteBranch), чтобы потомки не потеряли
     * историю.
     *
     * Сообщения предков самого [parent] не копируются — они остаются доступны [child]
     * через обновлённую ссылку parentBranchId.
     */
    private suspend fun materializeFromParentInto(child: BranchEntity, parent: BranchEntity) {
        val cutoff = child.forkFromMessageId
        val parentMessages = when {
            cutoff == null -> messageDao.getAll(parent.id)
            cutoff <= 0L -> emptyList()
            else -> messageDao.getUpTo(parent.id, cutoff)
        }
        if (parentMessages.isNotEmpty()) {
            // id = 0 → autoGenerate; branchId меняем на child; createdAt сохраняем,
            // чтобы хронология при сортировке осталась корректной
            val cloned = parentMessages.map { it.copy(id = 0L, branchId = child.id) }
            messageDao.insertAll(cloned)
        }
        // Дед становится новым родителем; forkFromMessageId наследуется от parent —
        // это та точка, в которой parent отделился от деда
        val updated = child.copy(
            parentBranchId = parent.parentBranchId,
            forkFromMessageId = parent.forkFromMessageId,
        )
        if (updated != child) branchDao.update(updated)
    }

    /**
     * Строит цепочку веток от корня к [leafId] с cutoff для каждого звена.
     * cutoff звена N — это forkFromMessageId следующего звена (N+1). Для последнего
     * звена (сам [leafId]) cutoff = null — берём все его собственные сообщения.
     */
    private suspend fun buildAncestorChainTopDown(leafId: Long): List<Pair<Long, Long?>> {
        val leaf = branchDao.findById(leafId) ?: return emptyList()
        val nodes = mutableListOf(leaf)
        val visited = mutableSetOf(leaf.id)
        var current = leaf
        while (true) {
            val parentId = current.parentBranchId ?: break
            if (parentId in visited) {
                log.w { "cycle detected at branch $parentId in ancestry of $leafId; stopping" }
                break
            }
            val parent = branchDao.findById(parentId) ?: break
            nodes.add(parent)
            visited.add(parent.id)
            current = parent
        }
        val topDown = nodes.asReversed()
        return topDown.mapIndexed { index, node ->
            val isLast = index == topDown.lastIndex
            val cutoff = if (isLast) null else topDown[index + 1].forkFromMessageId
            node.id to cutoff
        }
    }
}
