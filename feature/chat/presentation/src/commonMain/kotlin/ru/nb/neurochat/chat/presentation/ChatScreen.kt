package ru.nb.neurochat.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import ru.nb.neurochat.chat.presentation.components.SettingsPanel
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.presentation.util.ObserveAsEvents
import ru.nb.neurochat.presentation.util.currentDeviceConfiguration

@Composable
fun ChatScreenRoot(
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ChatEvent.OnError -> {
                scope.launch {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    ChatScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val deviceConfig = currentDeviceConfiguration()

    if (deviceConfig.isWideScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            SettingsPanel(
                state = state,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 320.dp)
                    .weight(0.3f),
            )
            VerticalDivider()
            ChatContent(
                state = state,
                onAction = onAction,
                showSettingsButton = false,
                snackbarHostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.7f),
            )
        }
    } else {
        ChatContent(
            state = state,
            onAction = onAction,
            showSettingsButton = true,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isSettingsOpen) {
            ModalBottomSheet(
                onDismissRequest = { onAction(ChatAction.OnDismissSettings) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                SettingsPanel(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContent(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    showSettingsButton: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NeuroChat")
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
                },
                actions = {
                    if (showSettingsButton) {
                        IconButton(onClick = { onAction(ChatAction.OnSettingsClick) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                    TextButton(onClick = { onAction(ChatAction.OnClearHistory) }) {
                        Text("Очистить")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            MessagesList(
                state = state,
                modifier = Modifier.weight(1f),
            )
            InputBar(
                text = state.inputText,
                isLoading = state.isLoading,
                onTextChange = { onAction(ChatAction.OnInputChange(it)) },
                onSend = { onAction(ChatAction.OnSendMessage) },
                onStop = { onAction(ChatAction.OnStopStreaming) },
            )
        }
    }
}

@Composable
private fun MessagesList(
    state: ChatState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.messages) { message ->
            val isStreaming = state.isLoading &&
                message === state.messages.last() &&
                message.role == ChatRole.Assistant
            when (message.role) {
                ChatRole.System -> SystemMessage(text = message.content)
                else -> MessageBubble(
                    text = message.content,
                    isUser = message.role == ChatRole.User,
                    showCursor = isStreaming,
                )
            }
        }
        if (state.isLoading && state.messages.lastOrNull()?.role != ChatRole.Assistant) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        state.error?.let { error ->
            item {
                Text(
                    text = "Ошибка: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun SystemMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun MessageBubble(
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
        MaterialTheme.colorScheme.onSurfaceVariant

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
                    showCursor -> "$text▋"
                    text.isEmpty() -> "▋"
                    else -> text
                },
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun InputBar(
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
            modifier = Modifier.weight(1f),
            placeholder = { Text("Сообщение или /команда...") },
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
                Text("→", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
