package ru.nb.neurochat

import androidx.compose.runtime.Composable
import ru.nb.neurochat.chat.presentation.ChatScreenRoot
import ru.nb.neurochat.designsystem.theme.NeuroChatTheme

// Общий KMP-корень UI. Вызывается из Android (NeuroChatApp/MainActivity), Desktop (DesktopApp),
// и iOS (MainViewController). Тему NeuroChatTheme применяем здесь, чтобы все платформы были одинаково.
@Composable
fun App() {
    NeuroChatTheme {
        ChatScreenRoot()
    }
}
