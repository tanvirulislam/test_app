package com.personal.shortscounter.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.personal.shortscounter.data.CounterStore
import com.personal.shortscounter.debug.DebugLog
import com.personal.shortscounter.debug.NodeDump
import com.personal.shortscounter.debug.oneLine
import com.personal.shortscounter.detection.DetectionConfig

class ShortsAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val evaluateRunnable = Runnable { evaluate() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // DetectionConfig is the source of truth for the package filter.
        serviceInfo = serviceInfo.apply { packageNames = arrayOf(DetectionConfig.YOUTUBE_PACKAGE) }
        DebugLog.enabled = CounterStore(this).debugMode
        isRunning = true
        DebugLog.log("service", "Connected, listening to ${DetectionConfig.YOUTUBE_PACKAGE}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName?.toString() != DetectionConfig.YOUTUBE_PACKAGE) return
        // Only logging so far, so there is nothing to do while Debug mode is off.
        if (!DebugLog.enabled) return
        DebugLog.log("event", describe(event))
        // Trailing debounce: inspect the screen once, after the UI settles.
        handler.removeCallbacks(evaluateRunnable)
        handler.postDelayed(evaluateRunnable, DetectionConfig.SETTLE_DELAY_MS)
    }

    private fun evaluate() {
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != DetectionConfig.YOUTUBE_PACKAGE) return
        val matchedId = DetectionConfig.SHORTS_CONTAINER_IDS.firstOrNull { id ->
            root.findAccessibilityNodeInfosByViewId(DetectionConfig.fullId(id)).any { it.isVisibleToUser }
        }
        val header = if (matchedId != null) "inShorts=true (via $matchedId)" else "inShorts=false"
        DebugLog.log("screen", header + "\n" + NodeDump.dump(root))
    }

    private fun describe(event: AccessibilityEvent): String = buildString {
        append(AccessibilityEvent.eventTypeToString(event.eventType).removePrefix("TYPE_"))
        append(" cls=").append(event.className?.toString()?.substringAfterLast('.') ?: "?")
        event.source?.viewIdResourceName?.let { append(" id=").append(it.substringAfter(":id/")) }
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                append(" from=").append(event.fromIndex)
                append(" to=").append(event.toIndex)
                append(" items=").append(event.itemCount)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    append(" dy=").append(event.scrollDeltaY)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                append(" changes=").append(event.contentChangeTypes)
        }
        val text = event.text.joinToString(" ").ifBlank { event.contentDescription?.toString().orEmpty() }
        if (text.isNotBlank()) append(" \"").append(text.oneLine(60)).append('"')
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        super.onDestroy()
    }

    companion object {
        /** True while the system has the service bound and connected. */
        @Volatile
        var isRunning = false
            private set
    }
}
