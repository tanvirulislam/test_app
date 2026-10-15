class ServiceStatus {
  const ServiceStatus({required this.enabled, required this.running});

  factory ServiceStatus.fromMap(Map<dynamic, dynamic> map) => ServiceStatus(
        enabled: map['enabled'] as bool,
        running: map['running'] as bool,
      );

  /// Turned on in Android's Accessibility settings.
  final bool enabled;

  /// Currently bound and receiving events from YouTube.
  final bool running;
}
