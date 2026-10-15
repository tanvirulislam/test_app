import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Shows a failed native call with its details so it can be copied and reported.
class ErrorCard extends StatelessWidget {
  const ErrorCard({super.key, required this.error, required this.onRetry});

  final Object error;
  final VoidCallback onRetry;

  String get _message => switch (error) {
        TimeoutException() => 'The Android side did not answer in time.',
        PlatformException(:final message, :final details) => [message, details].whereType<String>().join('\n\n'),
        _ => error.toString(),
      };

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      color: theme.colorScheme.errorContainer,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Something went wrong', style: theme.textTheme.titleMedium),
            const SizedBox(height: 8),
            ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 240),
              child: SingleChildScrollView(
                child: SelectableText(
                  _message,
                  style: theme.textTheme.bodySmall?.copyWith(fontFamily: 'monospace'),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                FilledButton.tonal(onPressed: onRetry, child: const Text('Retry')),
                const SizedBox(width: 8),
                TextButton(
                  onPressed: () => Clipboard.setData(ClipboardData(text: _message)),
                  child: const Text('Copy'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
