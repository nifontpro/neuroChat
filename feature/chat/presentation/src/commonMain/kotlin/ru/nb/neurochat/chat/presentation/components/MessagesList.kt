package ru.nb.neurochat.chat.presentation.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.ChatState
import ru.nb.neurochat.chat.presentation.components.messages.MessageBubble
import ru.nb.neurochat.chat.presentation.components.messages.SystemMessage
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.error_message
import ru.nb.neurochat.chat.presentation.util.toMessageRes
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.util.DataError

/** Список сообщений чата: пузырьки user/assistant, системные строки, индикатор загрузки и ошибка. */
@Composable
fun MessagesList(
    state: ChatState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Авто-прокрутка к последнему сообщению при стриминге или добавлении новых.
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
            listState.scrollBy(Float.MAX_VALUE)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.messages) { message ->
            MessageItem(
                message = message,
                isStreamingTail = state.isLoading &&
                    message === state.messages.last() &&
                    message.role == ChatRole.Assistant,
            )
        }
        if (state.isLoading && state.messages.lastOrNull()?.role != ChatRole.Assistant) {
            item { LoadingIndicatorRow() }
        }
        state.error?.let { error ->
            item { ErrorRow(error = error) }
        }
    }
}

@Composable
private fun MessageItem(message: ChatMessage, isStreamingTail: Boolean) {
    when (message.role) {
        ChatRole.System -> SystemMessage(text = message.content)
        else -> MessageBubble(
            text = message.content,
            isUser = message.role == ChatRole.User,
            showCursor = isStreamingTail,
        )
    }
}

@Composable
private fun LoadingIndicatorRow() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ErrorRow(error: DataError) {
    val errorText = stringResource(error.toMessageRes())
    Text(
        text = stringResource(Res.string.error_message, errorText),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(4.dp),
    )
}
