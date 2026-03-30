package ru.nb.neurochat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM messages ORDER BY id ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    @Insert
    suspend fun insert(entity: ChatMessageEntity)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
