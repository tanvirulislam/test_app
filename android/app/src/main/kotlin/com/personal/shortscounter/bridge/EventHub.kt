package com.personal.shortscounter.bridge

import android.os.Handler
import android.os.Looper
import com.personal.shortscounter.debug.DebugEntry
import java.util.concurrent.CopyOnWriteArraySet

/**
 * In-process hub so the accessibility service can push updates to the
 * activity's EventChannels when the Flutter UI is open. Listeners are called
 * on the main thread.
 */
object EventHub {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val debugListeners = CopyOnWriteArraySet<(DebugEntry) -> Unit>()

    fun addDebugListener(listener: (DebugEntry) -> Unit) = debugListeners.add(listener)

    fun removeDebugListener(listener: (DebugEntry) -> Unit) = debugListeners.remove(listener)

    fun emitDebug(entry: DebugEntry) {
        if (debugListeners.isEmpty()) return
        mainHandler.post { debugListeners.forEach { it(entry) } }
    }
}
