package com.personal.shortscounter.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.personal.shortscounter.bridge.EventHub
import com.personal.shortscounter.data.CounterStore
import com.personal.shortscounter.debug.DebugLog
import com.personal.shortscounter.debug.NodeDump
import com.personal.shortscounter.debug.oneLine
import com.personal.shortscounter.detection.DetectionConfig
import com.personal.shortscounter.detection.Fingerprint
import com.personal.shortscounter.detection.ShortsDetector
import com.personal.shortscounter.detection.ShortsDetector.Decision

class ShortsAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val evaluateRunnable = Runnable { evaluate() }
    private val detector = ShortsDetector(DetectionConfig.RECENT_SHORTS_MEMORY)
    private lateinit var store: CounterStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        // DetectionConfig is the source of truth for the package filter.
        serviceInfo = serviceInfo.apply { packageNames = arrayOf(DetectionConfig.YOUTUBE_PACKAGE) }
        store = CounterStore(this)
        DebugLog.enabled = store.debugMode
        isRunning = true
        DebugLog.log("service", "Connected, listening to ${DetectionConfig.YOUTUBE_PACKAGE}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName?.toString() != DetectionConfig.YOUTUBE_PACKAGE) return
        if (DebugLog.enabled) DebugLog.log("event", describe(event))
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && isSwipe(event)) {
            detector.onSwipe()
        }
        // Trailing debounce: inspect the screen once, after the UI settles.
        handler.removeCallbacks(evaluateRunnable)
        handler.postDelayed(evaluateRunnable, DetectionConfig.SETTLE_DELAY_MS)
    }

    private fun isSwipe(event: AccessibilityEvent): Boolean {
        val id = event.source?.viewIdResourceName?.substringAfter(":id/") ?: return false
        return id in DetectionConfig.SWIPE_SOURCE_IDS
    }

    private fun evaluate() {
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != DetectionConfig.YOUTUBE_PACKAGE) return

        val container = findShortsContainer(root)
        val fingerprint = container?.let { Fingerprint.from(Fingerprint.collectLabels(it)) }
        val decision = if (container == null) detector.onLeftShorts() else detector.onShortVisible(fingerprint)

        if (decision == Decision.COUNTED) {
            EventHub.count.emit(store.increment())
        }
        if (DebugLog.enabled) {
            val detail = if (container == null) {
                NodeDump.dump(root)
            } else {
                "fingerprint: ${fingerprint?.oneLine(300) ?: "-"}\n" + NodeDump.dump(container)
            }
            DebugLog.log("screen", "$decision\n$detail")
        }
    }

    private fun findShortsContainer(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        DetectionConfig.SHORTS_CONTAINER_IDS.firstNotNullOfOrNull { id ->
            root.findAccessibilityNodeInfosByViewId(DetectionConfig.fullId(id)).firstOrNull { it.isVisibleToUser }
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
