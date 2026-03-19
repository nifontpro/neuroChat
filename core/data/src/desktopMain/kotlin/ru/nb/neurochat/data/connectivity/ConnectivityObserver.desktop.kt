package ru.nb.neurochat.data.connectivity

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.coroutines.coroutineContext

actual class ConnectivityObserver {

    private val log = Logger.withTag("ConnectivityObserver")

    actual val isConnected: Flow<Boolean> = flow {
        while (true) {
            val connected = isConnected()
            log.i { "Desktop connectivity: $connected" }
            emit(connected)
            delay(5_000L)
        }
    }

    private val targets = listOf(
        InetSocketAddress("8.8.8.8", 53),
        InetSocketAddress("1.1.1.1", 53),
        InetSocketAddress("208.67.222.222", 53),
    )

    private suspend fun isConnected(): Boolean {
        val hasInterface = try {
	        withContext(Dispatchers.IO) {
		        NetworkInterface.getNetworkInterfaces()
	        }
                .asSequence()
                .any { ni -> !ni.isLoopback && ni.isUp && ni.inetAddresses.hasMoreElements() }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            false
        }

        if (!hasInterface) return false

        return withContext(Dispatchers.IO) {
            targets.any { target ->
                try {
                    Socket().use {
                        it.soTimeout = 3_000
                        it.connect(target)
                        true
                    }
                } catch (_: Exception) {
                    coroutineContext.ensureActive()
                    false
                }
            }
        }
    }
}
