package ru.nb.neurochat

import android.app.Application
import org.koin.android.ext.koin.androidContext
import ru.nb.neurochat.di.initKoin
import ru.nb.neurochat.domain.model.ApiSettings

class NeuroChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            settings = ApiSettings(
                baseUrl = BuildConfig.BASE_URL,
                apiKey = BuildConfig.API_KEY,
                model = BuildConfig.MODEL,
                timeoutSeconds = BuildConfig.TIMEOUT_SECONDS,
                systemPrompt = "Ты умный и дружелюбный ассистент.",
                temperature = 0.7,
            )
        ) {
            androidContext(this@NeuroChatApp)
        }
    }
}
