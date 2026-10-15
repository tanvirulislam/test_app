package com.personal.shortscounter

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.personal.shortscounter.bridge.EventHub
import com.personal.shortscounter.data.CounterStore
import com.personal.shortscounter.debug.DebugLog
import com.personal.shortscounter.service.ShortsAccessibilityService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val store by lazy { CounterStore(this) }
    private val unsubscribers = mutableListOf<() -> Unit>()

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val messenger = flutterEngine.dartExecutor.binaryMessenger
        DebugLog.enabled = store.debugMode

        MethodChannel(messenger, METHODS_CHANNEL).setMethodCallHandler { call, result ->
            try {
                handleMethod(call, result)
            } catch (e: Exception) {
                Log.e(TAG, "Method ${call.method} failed", e)
                result.error("native_error", "${call.method}: $e", e.stackTraceToString())
            }
        }

        bindTopic(messenger, COUNT_EVENTS_CHANNEL, EventHub.count) { it.toMap() }
        bindTopic(messenger, DEBUG_EVENTS_CHANNEL, EventHub.debug) { it.toMap() }
    }

    private fun handleMethod(call: MethodCall, result: MethodChannel.Result) {
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
            "getToday" -> result.success(store.today().toMap())
            "resetToday" -> {
                val today = store.resetToday()
                EventHub.count.emit(today)
                result.success(today.toMap())
            }
            "getDailyLimit" -> result.success(store.dailyLimit)
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

    /** Forwards an [EventHub] topic to an EventChannel while Flutter listens. */
    private fun <T> bindTopic(
        messenger: BinaryMessenger,
        channel: String,
        topic: EventHub.Topic<T>,
        encode: (T) -> Any,
    ) {
        var listener: ((T) -> Unit)? = null
        val unsubscribe = {
            listener?.let(topic::remove)
            listener = null
        }
        unsubscribers += unsubscribe
        EventChannel(messenger, channel).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                unsubscribe()
                val newListener: (T) -> Unit = { events.success(encode(it)) }
                listener = newListener
                topic.add(newListener)
            }

            override fun onCancel(arguments: Any?) = unsubscribe()
        })
    }

    override fun onDestroy() {
        unsubscribers.forEach { it() }
        unsubscribers.clear()
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
        const val TAG = "ShortsCounter"
        const val METHODS_CHANNEL = "shorts_counter/methods"
        const val COUNT_EVENTS_CHANNEL = "shorts_counter/count"
        const val DEBUG_EVENTS_CHANNEL = "shorts_counter/debug"
    }
}
