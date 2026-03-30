package ru.nb.neurochat.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<NeuroChatDatabase> {
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()
        val directory = when {
            osName.contains("mac") -> File(userHome, "Library/Application Support/NeuroChat")
            osName.contains("win") -> File(System.getenv("APPDATA"), "NeuroChat")
            else -> File(userHome, ".local/share/NeuroChat")
        }
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val dbFile = File(directory, NeuroChatDatabase.DB_NAME)
        return Room.databaseBuilder(name = dbFile.absolutePath)
    }
}
