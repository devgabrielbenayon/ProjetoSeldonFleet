package com.gabbe.seldonfleet

import android.app.Application

// Cria o container uma vez por processo; Activities e ViewModels o pegam daqui.
class SeldonFleetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
