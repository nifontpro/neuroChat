package ru.nb.neurochat.domain.model

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val statistics: ResponseStatistics? = null,
)

enum class ChatRole(val value: String) {
    System("system"),
    User("user"),
    Assistant("assistant"),
}
