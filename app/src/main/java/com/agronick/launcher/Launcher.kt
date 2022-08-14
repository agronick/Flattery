package com.agronick.launcher

import android.app.Application
import android.content.Context

class Launcher : Application() {

    override fun onCreate() {
        super.onCreate()
        Launcher.appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }
}