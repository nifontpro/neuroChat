package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_context_all
import ru.nb.neurochat.chat.presentation.generated.resources.label_context_window

// Ограничение контекста: сколько последних сообщений отправлять в запрос.
// 0 = все (без отсечения). Системный промпт добавляется независимо во VM.
@Composable
internal fun ContextWindowSlider(
    value: Int,
    onChange: (Int) -> Unit,
) {
    Column {
        val label = if (value == 0)
            stringResource(Res.string.label_context_all)
        else
            value.toString()
        Text(
            text = stringResource(Res.string.label_context_window, label),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..50f,
            steps = 49,
        )
    }
}
