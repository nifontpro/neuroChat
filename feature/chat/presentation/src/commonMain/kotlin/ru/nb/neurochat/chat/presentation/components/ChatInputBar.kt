package ru.nb.neurochat.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.message_input_placeholder
import ru.nb.neurochat.chat.presentation.generated.resources.send_button

// Строка ввода сообщения. Enter — отправка, Shift+Enter — перенос строки.
// Пока идёт стриминг — вместо «отправить» показывается прогресс/кнопка остановки.
@Composable
fun ChatInputBar(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    // Обработка Enter: чистый Enter = отправка, Shift+Enter = перенос строки.
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                        if (event.isShiftPressed) {
                            false
                        } else {
                            if (!isLoading) onSend()
                            true
                        }
                    } else {
                        false
                    }
                },
            placeholder = { Text(stringResource(Res.string.message_input_placeholder)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (!isLoading) onSend() }),
        )
        if (isLoading) {
            IconButton(onClick = onStop) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
            ) {
                Text(
                    stringResource(Res.string.send_button),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
