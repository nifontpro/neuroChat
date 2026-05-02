package ru.nb.neurochat.chat.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import ru.nb.neurochat.chat.presentation.components.ChatInputBar
import ru.nb.neurochat.chat.presentation.components.ChatTopBar
import ru.nb.neurochat.chat.presentation.components.MessagesList
import ru.nb.neurochat.chat.presentation.components.SettingsPanel
import ru.nb.neurochat.chat.presentation.util.toMessageRes
import ru.nb.neurochat.presentation.util.ObserveAsEvents
import ru.nb.neurochat.presentation.util.isDesktop

private val SETTINGS_PANEL_MAX_WIDTH = 320.dp

// Точка входа фичи «Чат». Подписывается на state/events ViewModel и передаёт в stateless ChatScreen.
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
                    val message = getString(event.error.toMessageRes())
                    snackbarHostState.showSnackbar(message = message)
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

// Адаптивная разметка: широкий экран -> Row(SettingsPanel | Chat), узкий -> Chat + ModalBottomSheet.
@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isWideScreen = windowSizeClass.minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND

    if (isWideScreen) {
        WideLayout(state, onAction, snackbarHostState)
    } else {
        NarrowLayout(state, onAction, snackbarHostState)
    }
}

@Composable
private fun WideLayout(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        SettingsPanel(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = SETTINGS_PANEL_MAX_WIDTH)
                .weight(0.3f),
        )
        VerticalDivider()
        ChatContent(
            state = state,
            onAction = onAction,
            showSettingsButton = false,
            showTopBarInfo = !isDesktop,
            showClearButton = !isDesktop,
            showTopBar = !isDesktop,
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NarrowLayout(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    ChatContent(
        state = state,
        onAction = onAction,
        showSettingsButton = !isDesktop,
        showTopBarInfo = !isDesktop,
        showClearButton = !isDesktop,
        showTopBar = !isDesktop,
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

// Собственно чат: TopBar + список сообщений + строка ввода. Используется как single-pane (mobile)
// или правая часть Row (wide screen). internal — чтобы был доступен в Preview (androidMain).
@Composable
internal fun ChatContent(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    showSettingsButton: Boolean,
    showTopBarInfo: Boolean,
    showClearButton: Boolean,
    showTopBar: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                ChatTopBar(
                    state = state,
                    onAction = onAction,
                    showTopBarInfo = showTopBarInfo,
                    showSettingsButton = showSettingsButton,
                    showClearButton = showClearButton,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            MessagesList(state = state, modifier = Modifier.weight(1f))
            ChatInputBar(
                text = state.inputText,
                isLoading = state.isLoading,
                onTextChange = { onAction(ChatAction.OnInputChange(it)) },
                onSend = { onAction(ChatAction.OnSendMessage) },
                onStop = { onAction(ChatAction.OnStopStreaming) },
            )
        }
    }
}
