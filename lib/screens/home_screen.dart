import 'package:flutter/material.dart';

import '../models/service_status.dart';
import '../services/native_bridge.dart';
import 'debug_screen.dart';
import 'onboarding_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late final AppLifecycleListener _lifecycle;
  ServiceStatus? _status;

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: _refresh);
    _refresh();
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    super.dispose();
  }

  Future<void> _refresh() async {
    final status = await NativeBridge.getServiceStatus();
    if (mounted) setState(() => _status = status);
  }

  void _open(Widget screen) {
    Navigator.of(context)
        .push(MaterialPageRoute<void>(builder: (_) => screen))
        .then((_) => _refresh());
  }

  @override
  Widget build(BuildContext context) {
    final status = _status;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Shorts Counter'),
        actions: [
          IconButton(
            tooltip: 'Debug log',
            icon: const Icon(Icons.bug_report_outlined),
            onPressed: () => _open(const DebugScreen()),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (status != null && !status.enabled)
            Card(
              color: Theme.of(context).colorScheme.errorContainer,
              child: ListTile(
                leading: const Icon(Icons.warning_amber),
                title: const Text('Accessibility service is off'),
                subtitle: const Text('Shorts are not being counted. Tap to fix.'),
                onTap: () => _open(const OnboardingScreen()),
              ),
            ),
          const SizedBox(height: 24),
          const Text(
            'Counting comes next. For now, open the Debug log (bug icon), turn on '
            'Debug logging, and use YouTube for a bit.',
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
