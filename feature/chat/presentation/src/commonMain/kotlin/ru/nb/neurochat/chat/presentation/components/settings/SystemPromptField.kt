package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.hint_system_prompt
import ru.nb.neurochat.chat.presentation.generated.resources.label_system_prompt

// Полностью stateless: значение поля приходит из ChatState, каждое изменение летит в VM.
// Без локального remember — иначе внешний reset/другой источник правды рассинхронизируется с UI.
@Composable
internal fun SystemPromptField(
    value: String?,
    onChange: (String?) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.label_system_prompt),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value ?: "",
            onValueChange = { onChange(it.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            placeholder = { Text(stringResource(Res.string.hint_system_prompt)) },
        )
    }
}
