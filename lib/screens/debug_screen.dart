import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models/debug_entry.dart';
import '../services/native_bridge.dart';

class DebugScreen extends StatefulWidget {
  const DebugScreen({super.key});

  @override
  State<DebugScreen> createState() => _DebugScreenState();
}

class _DebugScreenState extends State<DebugScreen> {
  /// The Android clipboard fails on very large text, so copying is capped.
  static const _maxCopyChars = 150000;
  static const _maxEntries = 500;

  final List<DebugEntry> _entries = [];
  StreamSubscription<DebugEntry>? _subscription;
  bool _enabled = false;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  Future<void> _load() async {
    final enabled = await NativeBridge.getDebugMode();
    final entries = await NativeBridge.getDebugLogs();
    if (!mounted) return;
    setState(() {
      _enabled = enabled;
      _entries
        ..clear()
        ..addAll(entries);
      _loading = false;
    });
    _subscription = NativeBridge.debugEntries().listen(_onEntry);
  }

  void _onEntry(DebugEntry entry) {
    setState(() {
      final lastId = _entries.isEmpty ? 0 : _entries.last.id;
      if (entry.id == lastId) {
        _entries[_entries.length - 1] = entry;
      } else if (entry.id > lastId) {
        _entries.add(entry);
        if (_entries.length > _maxEntries) _entries.removeAt(0);
      }
    });
  }

  Future<void> _setEnabled(bool enabled) async {
    await NativeBridge.setDebugMode(enabled);
    if (mounted) setState(() => _enabled = enabled);
  }

  Future<void> _clear() async {
    await NativeBridge.clearDebugLogs();
    if (mounted) setState(_entries.clear);
  }

  Future<void> _resetToday() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Reset today's count?"),
        content: const Text("Sets today's Shorts count back to 0. History for other days is kept."),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Reset')),
        ],
      ),
    );
    if (confirmed != true) return;
    await NativeBridge.resetToday();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("Today's count reset to 0")),
    );
  }

  Future<void> _copyAll() async {
    // Newest entries first until the cap, then put back in time order.
    final picked = <String>[];
    var size = 0;
    for (final entry in _entries.reversed) {
      final text = entry.toString();
      if (size + text.length > _maxCopyChars) break;
      picked.add(text);
      size += text.length + 2;
    }
    await Clipboard.setData(ClipboardData(text: picked.reversed.join('\n\n')));
    if (!mounted) return;
    final note = picked.length < _entries.length ? ' (latest ${picked.length} of ${_entries.length})' : '';
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Copied ${picked.length} entries$note')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final mono = theme.textTheme.bodySmall?.copyWith(fontFamily: 'monospace', fontSize: 11);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Debug log'),
        actions: [
          IconButton(
            tooltip: 'Copy all',
            icon: const Icon(Icons.copy_all),
            onPressed: _entries.isEmpty ? null : _copyAll,
          ),
          IconButton(
            tooltip: 'Clear log',
            icon: const Icon(Icons.delete_outline),
            onPressed: _entries.isEmpty ? null : _clear,
          ),
          IconButton(
            tooltip: "Reset today's count",
            icon: const Icon(Icons.restart_alt),
            onPressed: _resetToday,
          ),
        ],
      ),
      body: Column(
        children: [
          SwitchListTile(
            title: const Text('Debug logging'),
            subtitle: const Text('Logs what the service sees inside YouTube. Uses a bit more battery, so turn it off when done.'),
            value: _enabled,
            onChanged: _loading ? null : _setEnabled,
          ),
          const Divider(height: 1),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _entries.isEmpty
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Text(
                            _enabled
                                ? 'No entries yet. Open YouTube and use it for a bit.'
                                : 'Turn on Debug logging, then open YouTube.',
                            textAlign: TextAlign.center,
                          ),
                        ),
                      )
                    : ListView.separated(
                        itemCount: _entries.length,
                        separatorBuilder: (_, _) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          // Newest first.
                          final entry = _entries[_entries.length - 1 - index];
                          return Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(entry.header, style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.primary)),
                                const SizedBox(height: 2),
                                SelectableText(entry.message, style: mono),
                              ],
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
