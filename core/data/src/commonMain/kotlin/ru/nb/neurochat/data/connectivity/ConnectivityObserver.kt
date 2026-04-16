package ru.nb.neurochat.data.connectivity

import kotlinx.coroutines.flow.Flow

// Наблюдатель сетевого подключения. expect/actual реализации:
//   - Android: ConnectivityManager.NetworkCallback
//   - iOS: nw_path_monitor
//   - Desktop (JVM): polling DNS (8.8.8.8, 1.1.1.1)
// В isConnected true = есть интернет, false = нет связи (не только отсутствие сети, но и loss of network).
expect class ConnectivityObserver {
    val isConnected: Flow<Boolean>
}
