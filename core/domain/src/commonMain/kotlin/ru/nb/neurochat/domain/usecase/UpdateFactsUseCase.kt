package ru.nb.neurochat.domain.usecase

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.model.Fact
import ru.nb.neurochat.domain.repository.IChatRepository
import ru.nb.neurochat.domain.util.Result

private const val UPDATE_FACTS_PROMPT = """You maintain a JSON list of key-value facts about the user and the ongoing conversation.

Below is the current list of facts (may be empty), then recent messages. Update the list:
- merge new facts from the messages, overwrite stale values, drop irrelevant ones;
- keep keys short (1-3 words, lowercase, English or original language);
- keep values concise (one sentence max);
- keep the list under 20 items, prioritising goals, constraints, preferences, decisions, agreements.

Output ONLY a valid JSON array of objects with fields "key" and "value", no preamble, no markdown.
Example: [{"key":"goal","value":"learn KMP"},{"key":"language","value":"Russian"}]"""

/**
 * Извлекает/обновляет блок фактов через отдельный LLM-вызов (стратегия Sticky Facts).
 * Без потокового режима: ждём весь ответ, парсим JSON. При любой ошибке возвращаем [existingFacts].
 *
 * Не делает thinking — служебный вызов.
 *
 * @param thinkingDisabledSettings те же [ApiSettings], но с thinkingBudget = null;
 *        вызывающий обязан подготовить настройки сам.
 */
class UpdateFactsUseCase(
    private val repository: IChatRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend operator fun invoke(
        existingFacts: List<Fact>,
        recentMessages: List<ChatMessage>,
        thinkingDisabledSettings: ApiSettings,
    ): List<Fact> {
        if (recentMessages.isEmpty()) return existingFacts

        val factsBlock = if (existingFacts.isEmpty()) "Current facts: []"
        else "Current facts:\n" + existingFacts.joinToString("\n") {
            "- ${it.key}: ${it.value}"
        }
        val historyBlock = "Recent messages:\n" + recentMessages.joinToString("\n") {
            "${it.role.value}: ${it.content}"
        }

        val request = listOf(
            ChatMessage(ChatRole.System, UPDATE_FACTS_PROMPT),
            ChatMessage(ChatRole.User, "$factsBlock\n\n$historyBlock"),
        )

        val response = StringBuilder()
        var failed = false

        repository.streamMessage(request, thinkingDisabledSettings).collect { result ->
            when (result) {
                is Result.Failure -> failed = true
                is Result.Success -> {
                    val token = result.data
                    if (!token.isThinking && token.text.isNotEmpty()) response.append(token.text)
                }
            }
        }

        if (failed || response.isBlank()) return existingFacts

        return parseFacts(response.toString()) ?: existingFacts
    }

    private fun parseFacts(raw: String): List<Fact>? {
        val cleaned = stripCodeFence(raw).trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) return null
        val payload = cleaned.substring(start, end + 1)
        return try {
            json.decodeFromString<List<FactDto>>(payload)
                .map { Fact(it.key.trim(), it.value.trim()) }
                .filter { it.key.isNotBlank() && it.value.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun stripCodeFence(s: String): String {
        val t = s.trim()
        if (!t.startsWith("```")) return t
        val firstNl = t.indexOf('\n')
        val withoutOpen = if (firstNl >= 0) t.substring(firstNl + 1) else t.removePrefix("```")
        return withoutOpen.removeSuffix("```").trim()
    }

    @Serializable
    private data class FactDto(val key: String, val value: String)
}
