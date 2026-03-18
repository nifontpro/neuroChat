package ru.nb.neurochat.chat.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.nb.neurochat.chat.ChatViewModel
import ru.nb.neurochat.domain.model.ApiSettings

fun chatModule(settings: ApiSettings) = module {
    viewModel { ChatViewModel(repository = get(), baseSettings = settings) }
}
