package ru.nb.neurochat.chat.presentation.components.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val STREAMING_CURSOR = "▋"

/** Пузырёк сообщения: справа для user, слева для assistant.
 * @param text содержимое сообщения
 * @param isUser true — пузырёк пользователя (правый), false — ассистента (левый)
 * @param showCursor true — добавить мигающий курсор в конец текста (во время стриминга)
 */
@Composable
internal fun MessageBubble(
    text: String,
    isUser: Boolean,
    showCursor: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isUser)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = bubbleColor,
        ) {
            Text(
                text = when {
                    showCursor -> "$text$STREAMING_CURSOR"
                    text.isEmpty() -> STREAMING_CURSOR
                    else -> text
                },
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
