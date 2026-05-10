package ru.nb.neurochat.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class, BranchEntity::class],
    version = 3,
)
@ConstructedBy(NeuroChatDatabaseConstructor::class)
abstract class NeuroChatDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun branchDao(): BranchDao

    companion object {
        const val DB_NAME = "neurochat.db"
    }
}
