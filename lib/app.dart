import 'package:flutter/material.dart';

import 'models/service_status.dart';
import 'screens/home_screen.dart';
import 'screens/onboarding_screen.dart';
import 'services/native_bridge.dart';

class ShortsCounterApp extends StatelessWidget {
  const ShortsCounterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Shorts Counter',
      theme: _theme(Brightness.light),
      darkTheme: _theme(Brightness.dark),
      home: const _StartGate(),
    );
  }

  static ThemeData _theme(Brightness brightness) => ThemeData(
        colorSchemeSeed: const Color(0xFFE53935),
        brightness: brightness,
      );
}

/// Shows onboarding until the accessibility service is turned on.
class _StartGate extends StatelessWidget {
  const _StartGate();

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<ServiceStatus>(
      future: NativeBridge.getServiceStatus(),
      builder: (context, snapshot) {
        final status = snapshot.data;
        if (status == null) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        return status.enabled ? const HomeScreen() : const OnboardingScreen();
      },
    );
  }
}
