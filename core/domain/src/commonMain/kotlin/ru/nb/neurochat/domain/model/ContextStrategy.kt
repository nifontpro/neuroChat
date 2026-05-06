package ru.nb.neurochat.domain.model

/**
 * Стратегия управления контекстом, передаваемым в LLM.
 * — [SLIDING_WINDOW]: только последние N сообщений + опциональная сводка из /compact;
 * — [STICKY_FACTS]: блок «фактов» (ключ-значение) + последние N сообщений;
 * — [BRANCHING]: история текущей ветки целиком, без сжатия и фактов.
 */
enum class ContextStrategy(val key: String) {
    SLIDING_WINDOW("sliding_window"),
    STICKY_FACTS("sticky_facts"),
    BRANCHING("branching");

    companion object {
        val Default: ContextStrategy = SLIDING_WINDOW
        fun fromKey(key: String?): ContextStrategy =
            entries.firstOrNull { it.key == key } ?: Default
    }
}
