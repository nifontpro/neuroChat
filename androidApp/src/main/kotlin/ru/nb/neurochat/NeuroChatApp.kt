package ru.nb.neurochat

import android.app.Application
import org.koin.android.ext.koin.androidContext
import ru.nb.neurochat.data.connectivity.ConnectivityObserver
import ru.nb.neurochat.di.initKoin

class NeuroChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            platformModules = {
                single { ConnectivityObserver(androidContext()) }
            }
        ) {
            androidContext(this@NeuroChatApp)
        }
    }
}
