package ru.nb.neurochat.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index(value = ["branchId"])],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val role: String,
    val content: String,
    val durationMs: Long?,
    val tokenCount: Int?,
    val charCount: Int?,
)
