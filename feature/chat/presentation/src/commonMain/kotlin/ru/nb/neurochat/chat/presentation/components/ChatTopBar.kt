package ru.nb.neurochat.chat.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.ChatAction
import ru.nb.neurochat.chat.presentation.ChatState
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.clear
import ru.nb.neurochat.chat.presentation.generated.resources.settings
import ru.nb.neurochat.chat.presentation.generated.resources.title_app

// Верхняя панель чата: заголовок + краткая сводка (модель, температура, thinking, offline)
// и кнопки настроек / очистки истории. Сводка и кнопки могут скрываться (desktop, адаптивная разметка).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    showTopBarInfo: Boolean,
    showSettingsButton: Boolean,
    showClearButton: Boolean,
) {
    TopAppBar(
        title = {
            if (showTopBarInfo) {
                Column {
                    Text(stringResource(Res.string.title_app))
                    Text(
                        text = buildString {
                            append(state.currentModel)
                            state.currentTemperature?.let { append("  t=$it") }
                            if (state.thinkingEnabled) append("  thinking")
                            if (!state.isConnected) append("  [offline]")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isConnected)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        actions = {
            if (showSettingsButton) {
                IconButton(onClick = { onAction(ChatAction.OnSettingsClick) }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(Res.string.settings),
                    )
                }
            }
            if (showClearButton) {
                TextButton(onClick = { onAction(ChatAction.OnClearHistory) }) {
                    Text(stringResource(Res.string.clear))
                }
            }
        },
    )
}
