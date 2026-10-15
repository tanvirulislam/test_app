class DayCount {
  const DayCount({required this.date, required this.count});

  factory DayCount.fromMap(Map<dynamic, dynamic> map) => DayCount(
        date: DateTime.parse(map['date'] as String),
        count: map['count'] as int,
      );

  /// Calendar day in Asia/Dhaka (time part is midnight, no zone meaning).
  final DateTime date;
  final int count;
}
