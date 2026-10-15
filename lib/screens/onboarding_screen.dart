import 'package:flutter/material.dart';

import '../models/service_status.dart';
import '../services/native_bridge.dart';
import 'home_screen.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  late final AppLifecycleListener _lifecycle;
  ServiceStatus? _status;

  @override
  void initState() {
    super.initState();
    // Re-check when coming back from the Settings app.
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

  void _continue() {
    final navigator = Navigator.of(context);
    if (navigator.canPop()) {
      navigator.pop();
      return;
    }
    navigator.pushReplacement(
      MaterialPageRoute<void>(builder: (_) => const HomeScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final enabled = _status?.enabled ?? false;

    return Scaffold(
      appBar: AppBar(title: const Text('Set up Shorts Counter')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'Shorts Counter uses an Accessibility service to notice when you swipe '
            'to a new Short in the YouTube app. It only listens to YouTube, and '
            'nothing leaves your phone.',
            style: theme.textTheme.bodyLarge,
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text('1. Accessibility service', style: theme.textTheme.titleMedium),
                      ),
                      _StatusChip(enabled: enabled, loading: _status == null),
                    ],
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Open Accessibility settings, find "Shorts Counter" (often under '
                    '"Downloaded apps" or "Installed apps"), and turn it on.',
                  ),
                  const SizedBox(height: 12),
                  FilledButton.tonalIcon(
                    onPressed: NativeBridge.openAccessibilitySettings,
                    icon: const Icon(Icons.accessibility_new),
                    label: const Text('Open Accessibility settings'),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Switch greyed out? Android 13+ blocks this for sideloaded apps. '
                    'Open App info, tap the ⋮ menu (top right), choose "Allow restricted '
                    'settings", then try again.',
                    style: theme.textTheme.bodySmall,
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: NativeBridge.openAppDetails,
                    icon: const Icon(Icons.info_outline),
                    label: const Text('Open App info'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton(
            onPressed: enabled ? _continue : null,
            child: const Text('Continue'),
          ),
        ),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.enabled, required this.loading});

  final bool enabled;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    if (loading) return const SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2));
    return Chip(
      avatar: Icon(enabled ? Icons.check_circle : Icons.cancel, color: enabled ? scheme.primary : scheme.error),
      label: Text(enabled ? 'On' : 'Off'),
    );
  }
}
