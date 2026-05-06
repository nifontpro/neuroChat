package ru.nb.neurochat.domain.model

/**
 * Ключ-значение из стратегии Sticky Facts.
 * [key] — короткий идентификатор (например, "цель", "имя"), [value] — содержимое.
 */
data class Fact(
    val key: String,
    val value: String,
)
