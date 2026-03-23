package ru.nb.neurochat.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class UserSettingsStorage(
    private val dataStore: DataStore<Preferences>,
) {
    private val keyModel = stringPreferencesKey("model")
    private val keyTemperature = doublePreferencesKey("temperature")
    private val keyThinkingEnabled = intPreferencesKey("thinking_enabled")
    private val keySystemPrompt = stringPreferencesKey("system_prompt")
    private val keyMaxContext = intPreferencesKey("max_context_messages")
    private val keyShowStatistics = intPreferencesKey("show_statistics")

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
        )
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

data class SavedSettings(
    val model: String?,
    val temperature: Double?,
    val thinkingEnabled: Boolean?,
    val systemPrompt: String?,
    val maxContextMessages: Int?,
    val showStatistics: Boolean?,
)
