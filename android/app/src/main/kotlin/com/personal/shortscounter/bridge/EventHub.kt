package com.personal.shortscounter.bridge

import android.os.Handler
import android.os.Looper
import com.personal.shortscounter.data.DayCount
import com.personal.shortscounter.debug.DebugEntry
import java.util.concurrent.CopyOnWriteArraySet

/**
 * In-process hub so the accessibility service can push updates to the
 * activity's EventChannels when the Flutter UI is open. Listeners are called
 * on the main thread.
 */
object EventHub {
    val debug = Topic<DebugEntry>()
    val count = Topic<DayCount>()

    class Topic<T> {
        private val mainHandler = Handler(Looper.getMainLooper())
        private val listeners = CopyOnWriteArraySet<(T) -> Unit>()

        fun add(listener: (T) -> Unit) = listeners.add(listener)

        fun remove(listener: (T) -> Unit) = listeners.remove(listener)

        fun emit(value: T) {
            if (listeners.isEmpty()) return
            mainHandler.post { listeners.forEach { it(value) } }
        }
    }
}
