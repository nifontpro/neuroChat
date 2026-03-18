package ru.nb.neurochat.network.di

import org.koin.dsl.module
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.repository.IChatRepository
import ru.nb.neurochat.network.OpenAiClient
import ru.nb.neurochat.network.repository.ChatRepository

fun networkModule(settings: ApiSettings) = module {
    single { OpenAiClient(settings) }
    single<IChatRepository> { ChatRepository(get()) }
}
