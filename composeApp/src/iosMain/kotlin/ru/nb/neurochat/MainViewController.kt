package ru.nb.neurochat

import androidx.compose.ui.window.ComposeUIViewController
import ru.nb.neurochat.di.initKoin
import ru.nb.neurochat.domain.model.ApiSettings

fun MainViewController() = ComposeUIViewController {
    initKoin(
        ApiSettings(
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",  // TODO: читать из Info.plist
            model = "gpt-4o",
            systemPrompt = "Ты умный и дружелюбный ассистент.",
            temperature = 0.7,
        )
    )
    App()
}
