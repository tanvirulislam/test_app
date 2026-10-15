import 'package:flutter/material.dart';

import 'models/service_status.dart';
import 'screens/home_screen.dart';
import 'screens/onboarding_screen.dart';
import 'services/native_bridge.dart';
import 'widgets/error_card.dart';

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
class _StartGate extends StatefulWidget {
  const _StartGate();

  @override
  State<_StartGate> createState() => _StartGateState();
}

class _StartGateState extends State<_StartGate> {
  late Future<ServiceStatus> _status = _load();

  Future<ServiceStatus> _load() => NativeBridge.getServiceStatus().timeout(NativeBridge.callTimeout);

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<ServiceStatus>(
      future: _status,
      builder: (context, snapshot) {
        final status = snapshot.data;
        if (snapshot.hasError) {
          return Scaffold(
            appBar: AppBar(title: const Text('Shorts Counter')),
            body: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                ErrorCard(
                  error: snapshot.error!,
                  onRetry: () => setState(() => _status = _load()),
                ),
              ],
            ),
          );
        }
        if (status == null) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        return status.enabled ? const HomeScreen() : const OnboardingScreen();
      },
    );
  }
}
