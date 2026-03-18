package ru.nb.neurochat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ru.nb.neurochat.chat.ChatScreen

@Composable
fun App() {
    MaterialTheme {
        ChatScreen()
    }
}
