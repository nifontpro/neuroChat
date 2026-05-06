package ru.nb.neurochat.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentBranchId: Long?,
    val createdAt: Long,
)
