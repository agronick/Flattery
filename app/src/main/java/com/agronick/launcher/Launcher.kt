package com.agronick.launcher

import android.app.Application
import android.content.Context
import timber.log.Timber

class Launcher : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        lateinit var appContext: Context
    }
}