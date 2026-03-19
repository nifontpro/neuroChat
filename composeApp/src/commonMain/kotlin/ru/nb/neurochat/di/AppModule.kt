package ru.nb.neurochat.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import ru.nb.neurochat.chat.presentation.di.chatModule
import ru.nb.neurochat.data.defaultApiSettings
import ru.nb.neurochat.data.di.dataModule

fun initKoin(
    platformModules: Module.() -> Unit = {},
    appDeclaration: KoinAppDeclaration = {},
) {
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
