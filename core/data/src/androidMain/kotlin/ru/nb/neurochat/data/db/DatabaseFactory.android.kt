package ru.nb.neurochat.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<NeuroChatDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(NeuroChatDatabase.DB_NAME)
        return Room.databaseBuilder(
            context = context.applicationContext,
            name = dbFile.absolutePath,
        )
    }
}
