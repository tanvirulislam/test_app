class DebugEntry {
  const DebugEntry({
    required this.id,
    required this.time,
    required this.kind,
    required this.message,
    required this.repeat,
  });

  factory DebugEntry.fromMap(Map<dynamic, dynamic> map) => DebugEntry(
        id: map['id'] as int,
        time: DateTime.fromMillisecondsSinceEpoch(map['time'] as int),
        kind: map['kind'] as String,
        message: map['message'] as String,
        repeat: map['repeat'] as int,
      );

  final int id;
  final DateTime time;
  final String kind;
  final String message;
  final int repeat;

  String get timeLabel {
    String two(int v) => v.toString().padLeft(2, '0');
    final ms = time.millisecond.toString().padLeft(3, '0');
    return '${two(time.hour)}:${two(time.minute)}:${two(time.second)}.$ms';
  }

  String get header => repeat > 1 ? '$timeLabel $kind ×$repeat' : '$timeLabel $kind';

  @override
  String toString() => '[$header]\n$message';
}
