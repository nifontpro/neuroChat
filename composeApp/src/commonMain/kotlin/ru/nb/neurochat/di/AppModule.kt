package ru.nb.neurochat.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import ru.nb.neurochat.chat.di.chatModule
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.network.di.networkModule

fun initKoin(settings: ApiSettings, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            networkModule(settings),
            chatModule(settings),
        )
    }
}
