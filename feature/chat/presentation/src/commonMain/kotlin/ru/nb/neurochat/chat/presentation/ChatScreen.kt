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
import org.koin.compose.viewmodel.koinViewModel
import ru.nb.neurochat.chat.presentation.components.ChatInputBar
import ru.nb.neurochat.chat.presentation.components.ChatTopBar
import ru.nb.neurochat.chat.presentation.components.MessagesList
import ru.nb.neurochat.chat.presentation.components.SettingsPanel
import ru.nb.neurochat.presentation.util.ObserveAsEvents
import ru.nb.neurochat.presentation.util.isDesktop

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

// Адаптивная разметка:
//   - широкий экран (desktop/tablet landscape): SettingsPanel слева, чат справа
//   - узкий экран (mobile): только чат, настройки открываются ModalBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isWideScreen = windowSizeClass.minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND

    if (isWideScreen) {
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
                showTopBarInfo = !isDesktop,
                showClearButton = !isDesktop,
                showTopBar = !isDesktop,
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
}

// Собственно чат: TopBar + список сообщений + строка ввода. Используется как single-pane (mobile)
// или правая часть Row (wide screen). Разделён на internal, чтобы использовался в Preview (androidMain).
@OptIn(ExperimentalMaterial3Api::class)
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
            MessagesList(
                state = state,
                modifier = Modifier.weight(1f),
            )
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
