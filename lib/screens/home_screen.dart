import 'dart:async';

import 'package:flutter/material.dart';

import '../models/day_count.dart';
import '../models/service_status.dart';
import '../services/native_bridge.dart';
import '../widgets/error_card.dart';
import 'debug_screen.dart';
import 'onboarding_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late final AppLifecycleListener _lifecycle;
  StreamSubscription<DayCount>? _countSubscription;
  ServiceStatus? _status;
  DayCount? _today;
  int _limit = 30;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: _refresh);
    _countSubscription = NativeBridge.countUpdates().listen((today) {
      if (mounted) setState(() => _today = today);
    });
    _refresh();
  }

  @override
  void dispose() {
    _countSubscription?.cancel();
    _lifecycle.dispose();
    super.dispose();
  }

  Future<void> _refresh() async {
    try {
      final results = await Future.wait([
        NativeBridge.getServiceStatus(),
        NativeBridge.getToday(),
        NativeBridge.getDailyLimit(),
      ]).timeout(NativeBridge.callTimeout);
      if (!mounted) return;
      setState(() {
        _status = results[0] as ServiceStatus;
        _today = results[1] as DayCount;
        _limit = results[2] as int;
        _error = null;
      });
    } catch (error) {
      if (mounted) setState(() => _error = error);
    }
  }

  void _open(Widget screen) {
    Navigator.of(context)
        .push(MaterialPageRoute<void>(builder: (_) => screen))
        .then((_) => _refresh());
  }

  @override
  Widget build(BuildContext context) {
    final status = _status;
    final today = _today;
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
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: ListView(
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
            if (_error != null) ErrorCard(error: _error!, onRetry: _refresh),
            if (today == null && _error == null)
              const Padding(
                padding: EdgeInsets.all(48),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (today != null)
              _TodayCard(count: today.count, limit: _limit),
          ],
        ),
      ),
    );
  }
}

class _TodayCard extends StatelessWidget {
  const _TodayCard({required this.count, required this.limit});

  final int count;
  final int limit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final over = count >= limit;
    final accent = over ? scheme.error : scheme.primary;
    final progress = limit <= 0 ? 1.0 : (count / limit).clamp(0.0, 1.0);
    final remaining = limit - count;

    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
        child: Column(
          children: [
            Text('Shorts watched today', style: theme.textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(
              '$count',
              style: theme.textTheme.displayLarge?.copyWith(
                fontSize: 96,
                fontWeight: FontWeight.w600,
                color: accent,
              ),
            ),
            Text('of $limit daily limit', style: theme.textTheme.bodyLarge),
            const SizedBox(height: 24),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                value: progress,
                minHeight: 12,
                color: accent,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              remaining > 0
                  ? '$remaining left'
                  : remaining == 0
                      ? 'Limit reached'
                      : '${-remaining} over the limit',
              style: theme.textTheme.bodyMedium?.copyWith(color: over ? scheme.error : null),
            ),
          ],
        ),
      ),
    );
  }
}
