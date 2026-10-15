import 'package:flutter/services.dart';

import '../models/debug_entry.dart';
import '../models/service_status.dart';

/// Typed wrapper around the platform channels registered in MainActivity.kt.
class NativeBridge {
  NativeBridge._();

  static const _methods = MethodChannel('shorts_counter/methods');
  static const _debugEvents = EventChannel('shorts_counter/debug');

  static Future<ServiceStatus> getServiceStatus() async {
    final map = await _methods.invokeMapMethod<String, dynamic>('getServiceStatus');
    return ServiceStatus.fromMap(map!);
  }

  static Future<void> openAccessibilitySettings() =>
      _methods.invokeMethod<void>('openAccessibilitySettings');

  static Future<void> openAppDetails() => _methods.invokeMethod<void>('openAppDetails');

  static Future<bool> getDebugMode() async =>
      await _methods.invokeMethod<bool>('getDebugMode') ?? false;

  static Future<void> setDebugMode(bool enabled) =>
      _methods.invokeMethod<void>('setDebugMode', {'enabled': enabled});

  static Future<List<DebugEntry>> getDebugLogs() async {
    final list = await _methods.invokeListMethod<Map<dynamic, dynamic>>('getDebugLogs');
    return [for (final map in list ?? const []) DebugEntry.fromMap(map)];
  }

  static Future<void> clearDebugLogs() => _methods.invokeMethod<void>('clearDebugLogs');

  /// New and updated (merged repeat) log entries, while debug mode is on.
  static Stream<DebugEntry> debugEntries() => _debugEvents
      .receiveBroadcastStream()
      .map((event) => DebugEntry.fromMap(event as Map<dynamic, dynamic>));
}
