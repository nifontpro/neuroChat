package ru.nb.neurochat.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
)
@ConstructedBy(NeuroChatDatabaseConstructor::class)
abstract class NeuroChatDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DB_NAME = "neurochat.db"
    }
}
