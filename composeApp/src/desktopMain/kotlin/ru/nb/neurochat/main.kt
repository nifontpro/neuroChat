package ru.nb.neurochat

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.nb.neurochat.data.connectivity.ConnectivityObserver
import ru.nb.neurochat.data.db.DatabaseFactory
import ru.nb.neurochat.data.preferences.createDataStore
import ru.nb.neurochat.di.initKoin

// Точка входа Desktop-приложения.
// Ответственности: инициализация Koin с platform-specific зависимостями, восстановление размера
// и положения окна, оформление заголовка окна для macOS (сливаемый с контентом).
fun main() {
    // Platform-specific зависимости — доступны только в desktopMain, собираются здесь, а в
    // общих модулях получаются через Koin (см. dataModule, chatModule).
    initKoin(
        platformModules = {
            single { ConnectivityObserver() }
            single { createDataStore() }
            single { DatabaseFactory() }
        }
    )

    // Восстанавливаем сохранённую геометрию окна, если есть; иначе — центр экрана, 900x700.
    val windowStateStorage = WindowStateStorage()
    val savedBounds = windowStateStorage.load()
    val isMac = System.getProperty("os.name").lowercase().contains("mac")

    application {
        val windowState = rememberWindowState(
            size = if (savedBounds != null)
                DpSize(savedBounds.width.dp, savedBounds.height.dp)
            else
                DpSize(900.dp, 700.dp),
            position = if (savedBounds != null)
                WindowPosition.Absolute(savedBounds.x.dp, savedBounds.y.dp)
            else
                WindowPosition.Aligned(Alignment.Center),
            placement = if (savedBounds?.isMaximized == true)
                WindowPlacement.Maximized
            else
                WindowPlacement.Floating,
        )

        // Сохраняем bounds окна перед выходом. Maximized-окно сохраняется без координат —
        // при следующем запуске откроется снова maximized.
        val onClose: () -> Unit = {
            val pos = windowState.position
            windowStateStorage.save(
                WindowStateStorage.WindowBounds(
                    x = if (pos is WindowPosition.Absolute) pos.x.value else 0f,
                    y = if (pos is WindowPosition.Absolute) pos.y.value else 0f,
                    width = windowState.size.width.value,
                    height = windowState.size.height.value,
                    isMaximized = windowState.placement == WindowPlacement.Maximized,
                )
            )
            exitApplication()
        }

        Window(
            onCloseRequest = onClose,
            state = windowState,
            title = "NeuroChat",
        ) {
            // macOS: объединяем заголовок окна с содержимым (borderless-look). На Windows/Linux
            // эти свойства не применяются — там остаётся стандартный заголовок платформы.
            if (isMac) {
                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            }
            DesktopApp(isMac = isMac)
        }
    }
}
