import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shorts_counter/screens/home_screen.dart';

void main() {
  const methods = MethodChannel('shorts_counter/methods');

  Future<void> pumpHome(WidgetTester tester, Future<Object?> Function(MethodCall) handler) async {
    final messenger = tester.binding.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(methods, handler);
    messenger.setMockStreamHandler(
      const EventChannel('shorts_counter/count'),
      MockStreamHandler.inline(onListen: (_, _) {}),
    );
    await tester.pumpWidget(const MaterialApp(home: HomeScreen()));
    await tester.pump();
    await tester.pump();
  }

  testWidgets('shows the count from the native side', (tester) async {
    await pumpHome(tester, (call) async => switch (call.method) {
          'getServiceStatus' => {'enabled': true, 'running': true},
          'getToday' => {'date': '2026-10-16', 'count': 7},
          'getDailyLimit' => 30,
          _ => null,
        });

    expect(find.text('7'), findsOneWidget);
    expect(find.text('23 left'), findsOneWidget);
  });

  testWidgets('shows the native error instead of loading forever', (tester) async {
    await pumpHome(tester, (call) async {
      if (call.method == 'getToday') {
        throw PlatformException(code: 'native_error', message: 'getToday: boom');
      }
      return switch (call.method) {
        'getServiceStatus' => {'enabled': true, 'running': true},
        'getDailyLimit' => 30,
        _ => null,
      };
    });

    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.textContaining('getToday: boom'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });
}
