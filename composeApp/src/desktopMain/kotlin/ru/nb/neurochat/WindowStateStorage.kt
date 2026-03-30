package ru.nb.neurochat

import java.io.File
import java.util.Properties

class WindowStateStorage {

    private val file: File = run {
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()
        val directory = when {
            osName.contains("mac") -> File(userHome, "Library/Application Support/NeuroChat")
            osName.contains("win") -> File(System.getenv("APPDATA"), "NeuroChat")
            else -> File(userHome, ".local/share/NeuroChat")
        }
        directory.mkdirs()
        File(directory, "window_state.properties")
    }

    data class WindowBounds(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val isMaximized: Boolean,
    )

    fun load(): WindowBounds? {
        if (!file.exists()) return null
        return try {
            val props = Properties()
            file.inputStream().use { props.load(it) }
            WindowBounds(
                x = props.getProperty("x")?.toFloatOrNull() ?: return null,
                y = props.getProperty("y")?.toFloatOrNull() ?: return null,
                width = props.getProperty("width")?.toFloatOrNull() ?: return null,
                height = props.getProperty("height")?.toFloatOrNull() ?: return null,
                isMaximized = props.getProperty("maximized")?.toBooleanStrictOrNull() ?: false,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun save(bounds: WindowBounds) {
        try {
            val props = Properties()
            props.setProperty("x", bounds.x.toString())
            props.setProperty("y", bounds.y.toString())
            props.setProperty("width", bounds.width.toString())
            props.setProperty("height", bounds.height.toString())
            props.setProperty("maximized", bounds.isMaximized.toString())
            file.outputStream().use { props.store(it, null) }
        } catch (_: Exception) {
            // игнорируем ошибки записи
        }
    }
}