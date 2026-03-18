package ru.nb.neurochat.domain.model

data class StreamToken(
    val text: String,
    val isThinking: Boolean = false,
)
