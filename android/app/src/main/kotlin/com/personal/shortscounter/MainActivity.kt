package com.personal.shortscounter

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.personal.shortscounter.bridge.EventHub
import com.personal.shortscounter.data.CounterStore
import com.personal.shortscounter.debug.DebugEntry
import com.personal.shortscounter.debug.DebugLog
import com.personal.shortscounter.service.ShortsAccessibilityService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val store by lazy { CounterStore(this) }
    private var debugListener: ((DebugEntry) -> Unit)? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val messenger = flutterEngine.dartExecutor.binaryMessenger
        DebugLog.enabled = store.debugMode

        MethodChannel(messenger, METHODS_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getServiceStatus" -> result.success(
                    mapOf(
                        "enabled" to isServiceEnabled(),
                        "running" to ShortsAccessibilityService.isRunning,
                    )
                )
                "openAccessibilitySettings" -> {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    result.success(null)
                }
                "openAppDetails" -> {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null),
                        )
                    )
                    result.success(null)
                }
                "getDebugMode" -> result.success(store.debugMode)
                "setDebugMode" -> {
                    val enabled = call.argument<Boolean>("enabled") == true
                    store.debugMode = enabled
                    DebugLog.enabled = enabled
                    result.success(null)
                }
                "getDebugLogs" -> result.success(DebugLog.snapshot().map { it.toMap() })
                "clearDebugLogs" -> {
                    DebugLog.clear()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }

        EventChannel(messenger, DEBUG_EVENTS_CHANNEL).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                debugListener?.let(EventHub::removeDebugListener)
                val listener: (DebugEntry) -> Unit = { events.success(it.toMap()) }
                debugListener = listener
                EventHub.addDebugListener(listener)
            }

            override fun onCancel(arguments: Any?) {
                debugListener?.let(EventHub::removeDebugListener)
                debugListener = null
            }
        })
    }

    override fun onDestroy() {
        debugListener?.let(EventHub::removeDebugListener)
        debugListener = null
        super.onDestroy()
    }

    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(this, ShortsAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private companion object {
        const val METHODS_CHANNEL = "shorts_counter/methods"
        const val DEBUG_EVENTS_CHANNEL = "shorts_counter/debug"
    }
}
