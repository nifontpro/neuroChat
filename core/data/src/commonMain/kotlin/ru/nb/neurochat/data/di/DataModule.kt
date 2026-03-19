package ru.nb.neurochat.data.di

import org.koin.dsl.module
import ru.nb.neurochat.data.network.ChatRepository
import ru.nb.neurochat.data.network.OpenAiClient
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.repository.IChatRepository

fun dataModule(settings: ApiSettings) = module {
    single { OpenAiClient(settings) }
    single<IChatRepository> { ChatRepository(get()) }
}
