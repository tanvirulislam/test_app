package com.personal.shortscounter.debug

import com.personal.shortscounter.bridge.EventHub

data class DebugEntry(
    val id: Long,
    val timeMillis: Long,
    val kind: String,
    val message: String,
    val repeat: Int = 1,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "time" to timeMillis,
        "kind" to kind,
        "message" to message,
        "repeat" to repeat,
    )
}

/**
 * In-memory ring buffer of what the accessibility service sees. Consecutive
 * identical entries are merged into one with a repeat count.
 */
object DebugLog {
    private const val CAPACITY = 500

    private val entries = ArrayDeque<DebugEntry>(CAPACITY)
    private var nextId = 1L

    @Volatile
    var enabled = false

    fun log(kind: String, message: String) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        val entry = synchronized(this) {
            val last = entries.lastOrNull()
            val entry = if (last != null && last.kind == kind && last.message == message) {
                entries.removeLast()
                last.copy(timeMillis = now, repeat = last.repeat + 1)
            } else {
                if (entries.size == CAPACITY) entries.removeFirst()
                DebugEntry(nextId++, now, kind, message)
            }
            entries.addLast(entry)
            entry
        }
        EventHub.emitDebug(entry)
    }

    fun snapshot(): List<DebugEntry> = synchronized(this) { entries.toList() }

    fun clear() = synchronized(this) { entries.clear() }
}
