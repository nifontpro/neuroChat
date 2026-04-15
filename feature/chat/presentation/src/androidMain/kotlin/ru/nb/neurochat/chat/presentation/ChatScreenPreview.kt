package ru.nb.neurochat.chat.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import ru.nb.neurochat.designsystem.theme.NeuroChatTheme
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole

@Preview
@Composable
fun ChatScreenPreview() {
    NeuroChatTheme {
        ChatContent(
            state = ChatState(
                messages = listOf(
                    ChatMessage(role = ChatRole.User, content = "Привет! Как дела?"),
                    ChatMessage(
                        role = ChatRole.Assistant,
                        content = "Привет! Всё отлично, спасибо. Чем могу помочь?"
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = "Расскажи про Kotlin Multiplatform"
                    ),
                ),
                inputText = "Напиши пример кода",
                currentModel = "gpt-4o",
                currentTemperature = 0.7,
                isConnected = true,
            ),
            onAction = {},
            showSettingsButton = true,
            showTopBarInfo = true,
            showClearButton = true,
            showTopBar = true,
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
