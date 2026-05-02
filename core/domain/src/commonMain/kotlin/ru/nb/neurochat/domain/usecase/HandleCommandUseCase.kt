package ru.nb.neurochat.domain.usecase

import ru.nb.neurochat.domain.model.ApiSettings

/**
 * Парсит и применяет slash-команду к [settings]. Возвращает обновлённые настройки и тип результата
 * для отображения в UI. Никаких ресурсов/строк здесь нет — локализацию делает ViewModel.
 */
class HandleCommandUseCase(
    private val defaultThinkingBudget: Int,
) {
    operator fun invoke(text: String, settings: ApiSettings): CommandResult {
        val parts = text.split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim() ?: ""

        return when (cmd) {
            "/system" -> handleSystem(arg, settings)
            "/t" -> handleTemperature(arg, settings)
            "/think" -> handleThinking(arg, settings)
            "/?" -> CommandResult.Help
            else -> CommandResult.UnknownCommand
        }
    }

    private fun handleSystem(arg: String, settings: ApiSettings): CommandResult {
        val prompt = arg.ifBlank { null }
        return CommandResult.SystemPromptChanged(
            settings = settings.copy(systemPrompt = prompt),
            prompt = prompt,
        )
    }

    private fun handleTemperature(arg: String, settings: ApiSettings): CommandResult {
        val temp = arg.toDoubleOrNull()
        return if (temp != null && temp in MIN_TEMPERATURE..MAX_TEMPERATURE) {
            CommandResult.TemperatureChanged(
                settings = settings.copy(temperature = temp),
                value = temp,
            )
        } else {
            CommandResult.TemperatureHint
        }
    }

    private fun handleThinking(arg: String, settings: ApiSettings): CommandResult = when (arg.lowercase()) {
        "on" -> CommandResult.ThinkingChanged(
            settings = settings.copy(thinkingBudget = defaultThinkingBudget),
            enabled = true,
            budget = defaultThinkingBudget,
        )
        "off" -> CommandResult.ThinkingChanged(
            settings = settings.copy(thinkingBudget = null),
            enabled = false,
            budget = null,
        )
        else -> CommandResult.ThinkingHint
    }

    private companion object {
        const val MIN_TEMPERATURE = 0.0
        const val MAX_TEMPERATURE = 2.0
    }
}

sealed interface CommandResult {
    data class SystemPromptChanged(val settings: ApiSettings, val prompt: String?) : CommandResult
    data class TemperatureChanged(val settings: ApiSettings, val value: Double) : CommandResult
    data class ThinkingChanged(
        val settings: ApiSettings,
        val enabled: Boolean,
        val budget: Int?,
    ) : CommandResult

    data object TemperatureHint : CommandResult
    data object ThinkingHint : CommandResult
    data object Help : CommandResult
    data object UnknownCommand : CommandResult
}
