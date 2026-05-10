package ru.nb.neurochat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM messages WHERE branchId = :branchId ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(branchId: Long): List<ChatMessageEntity>

    @Query(
        "SELECT * FROM messages WHERE branchId = :branchId AND id <= :maxId " +
            "ORDER BY createdAt ASC, id ASC"
    )
    suspend fun getUpTo(branchId: Long, maxId: Long): List<ChatMessageEntity>

    @Query("SELECT MAX(id) FROM messages WHERE branchId = :branchId")
    suspend fun maxOwnId(branchId: Long): Long?

    @Insert
    suspend fun insert(entity: ChatMessageEntity)

    @Insert
    suspend fun insertAll(entities: List<ChatMessageEntity>)

    @Query("DELETE FROM messages WHERE branchId = :branchId")
    suspend fun deleteAllInBranch(branchId: Long)
}
