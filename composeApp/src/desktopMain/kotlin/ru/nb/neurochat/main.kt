package ru.nb.neurochat

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.nb.neurochat.data.connectivity.ConnectivityObserver
import ru.nb.neurochat.data.preferences.createDataStore
import ru.nb.neurochat.di.initKoin

fun main() {
    initKoin(
        platformModules = {
            single { ConnectivityObserver() }
            single { createDataStore() }
        }
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "NeuroChat",
        ) {
            App()
        }
    }
}
