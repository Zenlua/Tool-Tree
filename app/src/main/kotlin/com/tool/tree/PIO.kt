package com.tool.tree

import android.app.Application
import com.omarea.common.ui.CurrentActivityHolder

class PIO : Application() {

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(this)
        )

        registerActivityLifecycleCallbacks(CurrentActivityHolder)
    }
}