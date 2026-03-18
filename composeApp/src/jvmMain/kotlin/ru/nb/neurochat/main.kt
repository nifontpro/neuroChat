package ru.nb.neurochat

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.nb.neurochat.di.initKoin
import ru.nb.neurochat.domain.model.ApiSettings
import java.io.File
import java.util.Properties

fun main() {
    val props = Properties()
    listOf("litellm.properties", "../litellm.properties")
        .map(::File)
        .firstOrNull { it.exists() }
        ?.inputStream()
        ?.use(props::load)

    val settings = ApiSettings(
        baseUrl = props.getProperty("litellm.baseUrl", ""),
        apiKey = props.getProperty("litellm.apiKey", ""),
        model = props.getProperty("litellm.model", "gpt-4o"),
        timeoutSeconds = props.getProperty("litellm.timeoutSeconds", "300").toLong(),
        systemPrompt = "Ты умный и дружелюбный ассистент.",
        temperature = 0.7,
    )

    initKoin(settings)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "NeuroChat",
        ) {
            App()
        }
    }
}
