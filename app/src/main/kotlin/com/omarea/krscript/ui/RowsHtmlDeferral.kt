package com.omarea.krscript.ui

import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import java.lang.ref.WeakReference

object RowsHtmlDeferral {
    private const val MAX_DEFER_MS = 8000L
    private const val START_DELAY_MS = 150L

    private class Pending(val container: WeakReference<FrameLayout>, val action: () -> Unit)

    private val handler = Handler(Looper.getMainLooper())
    private val pending = ArrayList<Pending>()
    private val timeout = Runnable { end() }

    var deferring = false
        private set

    fun begin() {
        deferring = true
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, MAX_DEFER_MS)
    }

    fun end() {
        handler.removeCallbacks(timeout)
        deferring = false
        if (pending.isEmpty()) return
        val list = ArrayList(pending)
        pending.clear()
        for (item in list) {
            if (item.container.get() != null) handler.postDelayed(Runnable { item.action() }, START_DELAY_MS)
        }
    }

    fun defer(container: FrameLayout, action: () -> Unit): Boolean {
        if (!deferring) return false
        pending.removeAll { it.container.get().let { c -> c == null || c === container } }
        pending.add(Pending(WeakReference(container), action))
        return true
    }
}
