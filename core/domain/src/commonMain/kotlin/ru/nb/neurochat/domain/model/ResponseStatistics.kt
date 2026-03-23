package ru.nb.neurochat.domain.model

data class ResponseStatistics(
    val durationMs: Long,
    val tokenCount: Int,
    val charCount: Int,
) {
    val tokensPerSecond: Double
        get() = if (durationMs > 0) tokenCount * 1000.0 / durationMs else 0.0
}
