package com.omarea.common.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Theo dõi Activity đang ở trạng thái resumed (foreground) của toàn bộ ứng dụng.
 * Đăng ký 1 lần trong Application.onCreate() bằng registerActivityLifecycleCallbacks(this).
 * Dùng để BannerNotificationManager biết cần addView banner vào Activity nào.
 */
object CurrentActivityHolder : Application.ActivityLifecycleCallbacks {
    private var current: WeakReference<Activity>? = null

    fun get(): Activity? = current?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (current?.get() == activity) {
            current = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
