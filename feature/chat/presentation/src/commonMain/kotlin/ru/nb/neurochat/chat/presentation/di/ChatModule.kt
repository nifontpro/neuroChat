package ru.nb.neurochat.chat.presentation.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.nb.neurochat.chat.presentation.ChatViewModel
import ru.nb.neurochat.domain.model.ApiSettings

fun chatModule(settings: ApiSettings) = module {
    viewModel { ChatViewModel(repository = get(), baseSettings = settings, connectivityObserver = get()) }
}
