package ru.nb.neurochat.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first

/** Хранилище пользовательских настроек поверх DataStore Preferences (AndroidX).
 * DataStore создаётся platform-specific (createDataStore.android/desktop/ios.kt) и регистрируется в DI.
 * [SavedSettings] — снимок, который загружается при старте VM (см. loadSavedSettings).
 * @param dataStore экземпляр DataStore, предоставляется через DI
 */
class UserSettingsStorage(
    private val dataStore: DataStore<Preferences>,
) {
    private val log = Logger.withTag("UserSettings")
    private val keyModel = stringPreferencesKey("model")
    private val keyTemperature = doublePreferencesKey("temperature")
    private val keyThinkingEnabled = intPreferencesKey("thinking_enabled")
    private val keySystemPrompt = stringPreferencesKey("system_prompt")
    private val keyMaxContext = intPreferencesKey("max_context_messages")
    private val keyShowStatistics = intPreferencesKey("show_statistics")
    private val keyMaxTokens = intPreferencesKey("max_tokens")
    private val keyConversationSummary = stringPreferencesKey("conversation_summary")

    suspend fun saveModel(model: String) {
        dataStore.edit { it[keyModel] = model }
    }

    suspend fun saveTemperature(temperature: Double?) {
        dataStore.edit {
            if (temperature != null) it[keyTemperature] = temperature
            else it.remove(keyTemperature)
        }
    }

    suspend fun saveThinkingEnabled(enabled: Boolean) {
        dataStore.edit { it[keyThinkingEnabled] = if (enabled) 1 else 0 }
    }

    suspend fun saveSystemPrompt(prompt: String?) {
        dataStore.edit {
            if (prompt != null) it[keySystemPrompt] = prompt
            else it.remove(keySystemPrompt)
        }
    }

    suspend fun saveMaxContext(count: Int) {
        dataStore.edit { it[keyMaxContext] = count }
    }

    suspend fun saveShowStatistics(enabled: Boolean) {
        dataStore.edit { it[keyShowStatistics] = if (enabled) 1 else 0 }
    }

    suspend fun saveMaxTokens(maxTokens: Int?) {
        dataStore.edit {
            if (maxTokens != null) it[keyMaxTokens] = maxTokens
            else it.remove(keyMaxTokens)
        }
    }

    suspend fun saveConversationSummary(summary: String?) {
        dataStore.edit {
            if (summary != null) it[keyConversationSummary] = summary
            else it.remove(keyConversationSummary)
        }
    }

    suspend fun load(): SavedSettings? {
        val prefs = dataStore.data.first()
        val hasAny = prefs.asMap().isNotEmpty()
        if (!hasAny) return null

        return SavedSettings(
            model = prefs[keyModel],
            temperature = prefs[keyTemperature],
            thinkingEnabled = prefs[keyThinkingEnabled]?.let { it == 1 },
            systemPrompt = prefs[keySystemPrompt],
            maxContextMessages = prefs[keyMaxContext],
            showStatistics = prefs[keyShowStatistics]?.let { it == 1 },
            maxTokens = prefs[keyMaxTokens],
            conversationSummary = prefs[keyConversationSummary],
        )
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
        log.i { "user settings cleared" }
    }
}

data class SavedSettings(
    val model: String?,
    val temperature: Double?,
    val thinkingEnabled: Boolean?,
    val systemPrompt: String?,
    val maxContextMessages: Int?,
    val showStatistics: Boolean?,
    val maxTokens: Int?,
    val conversationSummary: String?,
)
