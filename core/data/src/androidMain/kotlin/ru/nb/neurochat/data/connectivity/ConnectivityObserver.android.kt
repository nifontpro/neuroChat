package ru.nb.neurochat.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/** Permission ACCESS_NETWORK_STATE декларирован в core/data/src/androidMain/AndroidManifest.xml
 *  и через manifest merger попадает в финальный манифест приложения. */
actual class ConnectivityObserver(
    context: Context,
) {
    private val log = Logger.withTag("ConnectivityObserver")
    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    actual val isConnected: Flow<Boolean> = connectivityManager?.observe()
        ?: run {
            log.w { "ConnectivityManager unavailable — assuming offline" }
            flowOf(false)
        }

    /** callbackFlow привязывает время жизни NetworkCallback к подписчику:
     *  awaitClose гарантирует unregister при отмене корутины. */
    private fun ConnectivityManager.observe(): Flow<Boolean> = callbackFlow {
        // NetworkCallback не стреляет по уже подключённой сети — опрашиваем состояние вручную при старте.
        val initiallyConnected = activeNetwork?.let { network ->
            getNetworkCapabilities(network)?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
        } ?: false

        send(initiallyConnected)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(false)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(false)
            }

            /** NET_CAPABILITY_VALIDATED может сняться после onAvailable (captive portal);
             *  NET_CAPABILITY_INTERNET недостаточен — он не гарантирует реальный доступ. */
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val connected = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                trySend(connected)
            }
        }

        registerDefaultNetworkCallback(callback)

        awaitClose {
            unregisterNetworkCallback(callback)
        }
    }
}
