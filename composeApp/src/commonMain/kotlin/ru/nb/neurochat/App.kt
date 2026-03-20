package ru.nb.neurochat

import androidx.compose.runtime.Composable
import ru.nb.neurochat.chat.presentation.ChatScreenRoot
import ru.nb.neurochat.designsystem.theme.NeuroChatTheme

@Composable
fun App() {
    NeuroChatTheme {
        ChatScreenRoot()
    }
}
