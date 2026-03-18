package ru.nb.neurochat.domain.model

data class ApiSettings(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val systemPrompt: String? = null,
    val temperature: Double? = null,
    val timeoutSeconds: Long = 300,
    val thinkingBudget: Int? = null,
)
