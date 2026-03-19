package ru.nb.neurochat.data

import ru.nb.neurochat.core.data.BuildKonfig
import ru.nb.neurochat.domain.model.ApiSettings

fun defaultApiSettings() = ApiSettings(
    baseUrl = BuildKonfig.BASE_URL,
    apiKey = BuildKonfig.API_KEY,
    model = BuildKonfig.MODEL,
    timeoutSeconds = BuildKonfig.TIMEOUT_SECONDS.toLongOrNull() ?: 300L,
    systemPrompt = "Ты умный и дружелюбный ассистент.",
    temperature = 0.7,
)
