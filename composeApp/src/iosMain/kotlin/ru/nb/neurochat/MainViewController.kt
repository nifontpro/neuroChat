package ru.nb.neurochat

import androidx.compose.ui.window.ComposeUIViewController
import ru.nb.neurochat.data.connectivity.ConnectivityObserver
import ru.nb.neurochat.data.db.DatabaseFactory
import ru.nb.neurochat.data.preferences.createDataStore
import ru.nb.neurochat.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin(
            platformModules = {
                single { ConnectivityObserver() }
                single { createDataStore() }
                single { DatabaseFactory() }
            }
        )
    }
) {
    App()
}
