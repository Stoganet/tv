package com.stoganet.tv

import android.app.Application
import com.stoganet.tv.di.ServiceLocator
import timber.log.Timber

class StoganetApp : Application() {

    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        services = ServiceLocator(this)
    }
}
