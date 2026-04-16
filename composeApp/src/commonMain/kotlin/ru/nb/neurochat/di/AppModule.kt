package ru.nb.neurochat.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import ru.nb.neurochat.chat.presentation.di.chatModule
import ru.nb.neurochat.data.defaultApiSettings
import ru.nb.neurochat.data.di.dataModule

// Запуск Koin. Вызывается ровно один раз на старте приложения:
//   - Android: в NeuroChatApp.onCreate
//   - Desktop: в main()
//   - iOS: в MainViewController
// platformModules — platform-specific зависимости (ConnectivityObserver, DatabaseFactory,
// createDataStore), которые собираются вызывающим и передаются сюда.
fun initKoin(
    platformModules: Module.() -> Unit = {},
    appDeclaration: KoinAppDeclaration = {},
) {
    // ApiSettings из BuildKonfig (litellm.properties) — дефолты, пользователь перезаписывает их через UI.
    val settings = defaultApiSettings()
    startKoin {
        appDeclaration()
        modules(
            module { platformModules() },
            dataModule(settings),
            chatModule(settings),
        )
    }
}
