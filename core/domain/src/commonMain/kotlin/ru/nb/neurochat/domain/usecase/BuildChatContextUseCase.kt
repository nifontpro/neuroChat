package ru.nb.neurochat.domain.usecase

import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole

/**
 * Собирает список сообщений для отправки в LLM:
 * — системный промпт первой строкой (если задан);
 * — сводка предыдущего диалога второй строкой (если есть, после /compact);
 * — обрезает историю по [maxContextMessages] (0 = без лимита);
 * — возвращает готовый плоский список.
 */
class BuildChatContextUseCase {
    operator fun invoke(
        history: List<ChatMessage>,
        systemPrompt: String?,
        maxContextMessages: Int,
        conversationSummary: String? = null,
    ): List<ChatMessage> {
        val trimmed = if (maxContextMessages > 0 && history.size > maxContextMessages) {
            history.takeLast(maxContextMessages)
        } else {
            history
        }
        return buildList {
            systemPrompt?.let { add(ChatMessage(ChatRole.System, it)) }
            conversationSummary?.let {
                add(ChatMessage(ChatRole.System, "Summary of previous conversation:\n$it"))
            }
            addAll(trimmed)
        }
    }
}
