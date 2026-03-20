package ru.nb.neurochat.chat.presentation

sealed interface ChatEvent {
    data class OnError(val message: String) : ChatEvent
}
