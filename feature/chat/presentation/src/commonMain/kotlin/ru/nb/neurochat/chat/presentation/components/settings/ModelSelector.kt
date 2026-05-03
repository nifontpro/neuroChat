package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_model
import ru.nb.neurochat.domain.model.AVAILABLE_MODELS

/** Выпадающий список моделей из [AVAILABLE_MODELS]. Текущая выделяется цветом primary.
 * @param currentModel выбранная модель
 * @param onSelect callback при выборе модели
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelSelector(
    currentModel: String,
    onSelect: (String) -> Unit,
) {
    Text(
        text = stringResource(Res.string.label_model),
        style = MaterialTheme.typography.titleSmall,
    )

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = currentModel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AVAILABLE_MODELS.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = model,
                            color = if (model == currentModel)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        onSelect(model)
                        expanded = false
                    },
                )
            }
        }
    }
}
