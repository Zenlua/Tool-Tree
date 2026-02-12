package com.tool.tree

import android.app.Application

class PIO : Application() {

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(this)
        )
    }
}