package ru.nb.neurochat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.StreamToken
import ru.nb.neurochat.domain.util.DataError
import ru.nb.neurochat.domain.util.Result

interface IChatRepository {
    /**
     * Стрим токенов от LLM. Каждый элемент — Result.Success(token) с очередным куском контента
     * или Result.Failure(DataError) при сетевой/протокольной ошибке. После Failure стрим завершается.
     */
    fun streamMessage(
        history: List<ChatMessage>,
        settings: ApiSettings,
    ): Flow<Result<StreamToken, DataError>>

    /**
     * GET /models — отсортированный список доступных у провайдера моделей.
     * При сетевой/протокольной ошибке возвращает Result.Failure(DataError).
     */
    suspend fun listModels(): Result<List<String>, DataError>
}
