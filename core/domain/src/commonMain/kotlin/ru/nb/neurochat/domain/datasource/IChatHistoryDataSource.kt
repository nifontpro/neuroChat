package ru.nb.neurochat.domain.datasource

import ru.nb.neurochat.domain.model.Branch
import ru.nb.neurochat.domain.model.ChatMessage

interface IChatHistoryDataSource {
    suspend fun getMessages(branchId: Long): List<ChatMessage>
    suspend fun saveMessage(message: ChatMessage, branchId: Long)
    suspend fun clearAll(branchId: Long)

    suspend fun getBranches(): List<Branch>
    suspend fun findBranch(id: Long): Branch?
    suspend fun ensureMainBranch(): Branch
    suspend fun createBranchFrom(parent: Branch, newName: String): Branch
    suspend fun renameBranch(branchId: Long, newName: String)
    suspend fun deleteBranch(branchId: Long)

    companion object {
        const val MAIN_BRANCH_ID: Long = 1L
        const val MAIN_BRANCH_NAME: String = "main"
    }
}
