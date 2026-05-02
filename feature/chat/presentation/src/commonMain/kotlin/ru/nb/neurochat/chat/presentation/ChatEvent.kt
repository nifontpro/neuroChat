package ru.nb.neurochat.chat.presentation

import ru.nb.neurochat.domain.util.DataError

sealed interface ChatEvent {
    data class OnError(val error: DataError) : ChatEvent
}
