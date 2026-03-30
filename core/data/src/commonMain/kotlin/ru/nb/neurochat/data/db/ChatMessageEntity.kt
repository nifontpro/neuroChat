package ru.nb.neurochat.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val durationMs: Long?,
    val tokenCount: Int?,
    val charCount: Int?,
)
