package ru.nb.neurochat.domain.usecase

import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.model.ContextStrategy
import ru.nb.neurochat.domain.model.Fact

/**
 * Собирает плоский список сообщений для отправки в LLM согласно [ContextStrategy].
 *
 * Общее: системный промпт всегда первый (если задан).
 *
 * SLIDING_WINDOW: после системного — сводка предыдущего диалога (если есть, после /compact),
 *                 затем последние [maxContextMessages] сообщений (0 = без лимита).
 * STICKY_FACTS:   после системного — блок фактов как System-сообщение,
 *                 затем последние [maxContextMessages] сообщений (0 = без лимита).
 * BRANCHING:      история ветки целиком, без сжатия и фактов; [maxContextMessages] игнорируется.
 */
class BuildChatContextUseCase {
    operator fun invoke(
        history: List<ChatMessage>,
        systemPrompt: String?,
        maxContextMessages: Int,
        conversationSummary: String? = null,
        strategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW,
        facts: List<Fact> = emptyList(),
    ): List<ChatMessage> {
        val trimmed = when (strategy) {
            ContextStrategy.BRANCHING -> history
            else -> if (maxContextMessages > 0 && history.size > maxContextMessages) {
                history.takeLast(maxContextMessages)
            } else {
                history
            }
        }
        return buildList {
            systemPrompt?.let { add(ChatMessage(ChatRole.System, it)) }
            when (strategy) {
                ContextStrategy.SLIDING_WINDOW -> {
                    conversationSummary?.let {
                        add(ChatMessage(ChatRole.System, "Summary of previous conversation:\n$it"))
                    }
                }
                ContextStrategy.STICKY_FACTS -> {
                    if (facts.isNotEmpty()) {
                        add(ChatMessage(ChatRole.System, formatFacts(facts)))
                    }
                }
                ContextStrategy.BRANCHING -> Unit
            }
            addAll(trimmed)
        }
    }

    private fun formatFacts(facts: List<Fact>): String = buildString {
        append("Known facts about the user and the conversation (key: value):\n")
        facts.forEach { append("- ").append(it.key).append(": ").append(it.value).append('\n') }
    }
}
