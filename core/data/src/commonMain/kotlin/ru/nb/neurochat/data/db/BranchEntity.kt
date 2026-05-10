package ru.nb.neurochat.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "branches",
    indices = [Index(value = ["parentBranchId"])],
)
data class BranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentBranchId: Long?,
    val forkFromMessageId: Long?,
    val createdAt: Long,
)
