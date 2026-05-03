package ru.nb.neurochat.chat.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.ChatAction
import ru.nb.neurochat.chat.presentation.ChatState
import ru.nb.neurochat.chat.presentation.components.settings.ConnectivityStatusText
import ru.nb.neurochat.chat.presentation.components.settings.ContextWindowSlider
import ru.nb.neurochat.chat.presentation.components.settings.MaxTokensSlider
import ru.nb.neurochat.chat.presentation.components.settings.ModelSelector
import ru.nb.neurochat.chat.presentation.components.settings.SwitchRow
import ru.nb.neurochat.chat.presentation.components.settings.SystemPromptField
import ru.nb.neurochat.chat.presentation.components.settings.TemperatureSlider
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_reset_settings
import ru.nb.neurochat.chat.presentation.generated.resources.label_show_statistics
import ru.nb.neurochat.chat.presentation.generated.resources.label_thinking_mode
import ru.nb.neurochat.chat.presentation.generated.resources.settings

/** Панель настроек чата. Рендерится двумя способами (см. ChatScreen):
 *  - широкий экран — колонка слева
 *  - узкий экран — ModalBottomSheet
 * @param state текущее состояние чата
 * @param onAction обработчик действий UI
 */
@Composable
fun SettingsPanel(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark)
        MaterialTheme.colorScheme.surfaceContainerLowest
    else
        MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        modifier = modifier,
        color = panelColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsHeader()
            ConnectivityStatusText(isConnected = state.isConnected)

            if (state.showStatistics) {
                StatisticsSection(state = state)
                Divider()
            }

            ModelSelector(
                currentModel = state.currentModel,
                onSelect = { onAction(ChatAction.OnSelectModel(it)) },
            )
            Divider()

            TemperatureSlider(
                value = state.currentTemperature,
                onChange = { onAction(ChatAction.OnTemperatureChange(it)) },
            )
            Divider()

            ContextWindowSlider(
                value = state.maxContextMessages,
                onChange = { onAction(ChatAction.OnMaxContextChange(it)) },
            )
            Divider()

            MaxTokensSlider(
                value = state.maxTokens,
                onChange = { onAction(ChatAction.OnMaxTokensChange(it)) },
            )
            Divider()

            SwitchRow(
                label = stringResource(Res.string.label_thinking_mode),
                checked = state.thinkingEnabled,
                onCheckedChange = { onAction(ChatAction.OnThinkingToggle(it)) },
            )
            Divider()

            SwitchRow(
                label = stringResource(Res.string.label_show_statistics),
                checked = state.showStatistics,
                onCheckedChange = { onAction(ChatAction.OnToggleStatistics(it)) },
            )
            Divider()

            SystemPromptField(
                value = state.systemPrompt,
                onChange = { onAction(ChatAction.OnSystemPromptChange(it)) },
            )
            Divider()

            ResetSettingsButton(onClick = { onAction(ChatAction.OnResetSettings) })
        }
    }
}

@Composable
private fun SettingsHeader() {
    Text(
        text = stringResource(Res.string.settings),
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ResetSettingsButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(text = stringResource(Res.string.label_reset_settings))
    }
}
