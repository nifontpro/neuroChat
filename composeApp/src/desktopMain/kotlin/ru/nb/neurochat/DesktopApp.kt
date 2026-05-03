package ru.nb.neurochat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ru.nb.neurochat.chat.presentation.ChatAction
import ru.nb.neurochat.chat.presentation.ChatViewModel
import ru.nb.neurochat.composeapp.generated.resources.Res
import ru.nb.neurochat.composeapp.generated.resources.desktop_clear
import ru.nb.neurochat.composeapp.generated.resources.desktop_settings_icon
import ru.nb.neurochat.composeapp.generated.resources.desktop_title_format
import ru.nb.neurochat.designsystem.theme.NeuroChatTheme

/** Корень Desktop UI. На macOS поверх содержимого добавляется свой «сливающийся» TitleBar
 * (см. настройки apple.awt.* в main.kt). На Windows/Linux используется системный заголовок.
 * @param isMac true, если приложение запущено на macOS
 */
@Composable
fun DesktopApp(isMac: Boolean) {
    NeuroChatTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMac) {
                MacTitleBar()
            }
            App()
        }
    }
}

@Composable
private fun MacTitleBar() {
    val viewModel = koinViewModel<ChatViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isWideScreen =
        currentWindowAdaptiveInfo().windowSizeClass.minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Text(
            text = stringResource(Res.string.desktop_title_format, state.currentModel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center),
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isWideScreen) {
                IconButton(
                    onClick = { viewModel.onAction(ChatAction.OnSettingsClick) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.desktop_settings_icon),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(
                onClick = { viewModel.onAction(ChatAction.OnClearHistory) },
                modifier = Modifier.height(28.dp),
            ) {
                Text(
                    text = stringResource(Res.string.desktop_clear),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
