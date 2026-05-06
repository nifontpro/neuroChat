package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_branch_create
import ru.nb.neurochat.chat.presentation.generated.resources.label_branch_delete
import ru.nb.neurochat.chat.presentation.generated.resources.label_branch_new_placeholder
import ru.nb.neurochat.chat.presentation.generated.resources.label_branch_switch
import ru.nb.neurochat.chat.presentation.generated.resources.label_branches_current
import ru.nb.neurochat.chat.presentation.generated.resources.label_branches_section
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource.Companion.MAIN_BRANCH_ID
import ru.nb.neurochat.domain.model.Branch

/** Секция управления ветками диалога (только для BRANCHING). */
@Composable
fun BranchesSection(
    branches: List<Branch>,
    currentBranchId: Long,
    onCreate: (String) -> Unit,
    onSwitch: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentName = branches.firstOrNull { it.id == currentBranchId }?.name ?: "—"
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.label_branches_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.label_branches_current, currentName),
            style = MaterialTheme.typography.bodyMedium,
        )

        branches.forEach { branch ->
            BranchRow(
                branch = branch,
                isCurrent = branch.id == currentBranchId,
                onSwitch = { onSwitch(branch.id) },
                onDelete = { onDelete(branch.id) },
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = { Text(text = stringResource(Res.string.label_branch_new_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val name = newName.trim()
                if (name.isNotBlank()) {
                    onCreate(name)
                    newName = ""
                }
            },
            enabled = newName.trim().isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.label_branch_create))
        }
    }
}

@Composable
private fun BranchRow(
    branch: Branch,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val marker = if (isCurrent) "→" else "  "
        Text(
            text = "$marker ${branch.id}: ${branch.name}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (!isCurrent) {
            TextButton(onClick = onSwitch) {
                Text(text = stringResource(Res.string.label_branch_switch))
            }
        }
        if (branch.id != MAIN_BRANCH_ID) {
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(Res.string.label_branch_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
