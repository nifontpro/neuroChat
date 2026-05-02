package ru.nb.neurochat.domain.usecase

import ru.nb.neurochat.domain.model.ApiSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HandleCommandUseCaseTest {

    private val baseSettings = ApiSettings(
        baseUrl = "https://example",
        apiKey = "k",
        model = "m",
    )
    private val useCase = HandleCommandUseCase(defaultThinkingBudget = 1234)

    @Test
    fun systemPromptSet_returnsChangedWithPrompt() {
        val result = useCase("/system Be helpful", baseSettings)
        assertTrue(result is CommandResult.SystemPromptChanged)
        assertEquals("Be helpful", result.prompt)
        assertEquals("Be helpful", result.settings.systemPrompt)
    }

    @Test
    fun systemPromptEmpty_resetsPromptToNull() {
        val result = useCase("/system", baseSettings.copy(systemPrompt = "old"))
        assertTrue(result is CommandResult.SystemPromptChanged)
        assertNull(result.prompt)
        assertNull(result.settings.systemPrompt)
    }

    @Test
    fun temperatureValid_returnsChanged() {
        val result = useCase("/t 0.5", baseSettings)
        assertTrue(result is CommandResult.TemperatureChanged)
        assertEquals(0.5, result.value)
        assertEquals(0.5, result.settings.temperature)
    }

    @Test
    fun temperatureOutOfRange_returnsHint() {
        assertEquals(CommandResult.TemperatureHint, useCase("/t 5", baseSettings))
        assertEquals(CommandResult.TemperatureHint, useCase("/t -1", baseSettings))
        assertEquals(CommandResult.TemperatureHint, useCase("/t abc", baseSettings))
    }

    @Test
    fun thinkOn_setsBudget() {
        val result = useCase("/think on", baseSettings)
        assertTrue(result is CommandResult.ThinkingChanged)
        assertTrue(result.enabled)
        assertEquals(1234, result.budget)
        assertEquals(1234, result.settings.thinkingBudget)
    }

    @Test
    fun thinkOff_clearsBudget() {
        val result = useCase("/think off", baseSettings.copy(thinkingBudget = 999))
        assertTrue(result is CommandResult.ThinkingChanged)
        assertEquals(false, result.enabled)
        assertNull(result.budget)
        assertNull(result.settings.thinkingBudget)
    }

    @Test
    fun thinkBadArg_returnsHint() {
        assertEquals(CommandResult.ThinkingHint, useCase("/think maybe", baseSettings))
    }

    @Test
    fun help_returnsHelp() {
        assertEquals(CommandResult.Help, useCase("/?", baseSettings))
    }

    @Test
    fun unknown_returnsUnknown() {
        assertEquals(CommandResult.UnknownCommand, useCase("/foo", baseSettings))
    }

    @Test
    fun caseInsensitive_command() {
        assertTrue(useCase("/SYSTEM hi", baseSettings) is CommandResult.SystemPromptChanged)
    }
}
