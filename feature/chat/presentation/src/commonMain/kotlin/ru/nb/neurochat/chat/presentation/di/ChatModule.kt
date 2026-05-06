package ru.nb.neurochat.chat.presentation.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.nb.neurochat.chat.presentation.ChatViewModel
import ru.nb.neurochat.chat.presentation.DEFAULT_THINKING_BUDGET_TOKENS
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.usecase.BuildChatContextUseCase
import ru.nb.neurochat.domain.usecase.HandleCommandUseCase
import ru.nb.neurochat.domain.usecase.UpdateFactsUseCase

fun chatModule(settings: ApiSettings) = module {
    single { HandleCommandUseCase(defaultThinkingBudget = DEFAULT_THINKING_BUDGET_TOKENS) }
    single { BuildChatContextUseCase() }
    single { UpdateFactsUseCase(get()) }

    viewModel {
        ChatViewModel(
            baseSettings = settings,
            repository = get(),
            connectivityObserver = get(),
            settingsStorage = get(),
            historyDataSource = get(),
            handleCommand = get(),
            buildChatContext = get(),
            updateFacts = get(),
        )
    }
}
