package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.status_offline
import ru.nb.neurochat.chat.presentation.generated.resources.status_online

/** Индикатор подключения. Цвет: primary — онлайн, error — оффлайн.
 * @param isConnected true — есть сеть, false — офлайн
 */
@Composable
internal fun ConnectivityStatusText(isConnected: Boolean) {
    Text(
        text = stringResource(
            if (isConnected) Res.string.status_online else Res.string.status_offline,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isConnected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.error,
    )
}
