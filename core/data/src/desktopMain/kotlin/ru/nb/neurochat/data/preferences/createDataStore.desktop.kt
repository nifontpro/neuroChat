package ru.nb.neurochat.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun createDataStore(): DataStore<Preferences> = createDataStore {
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
    File(directory, DATA_STORE_FILE_NAME).absolutePath
}
