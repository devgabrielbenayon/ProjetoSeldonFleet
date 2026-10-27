package com.gabbe.seldonfleet

import android.app.Application
import com.gabbe.seldonfleet.work.FleetRefreshScheduler

// Cria o container uma vez por processo; Activities, ViewModels e o Worker o pegam daqui.
class SeldonFleetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        FleetRefreshScheduler.schedule(this)
    }
}
