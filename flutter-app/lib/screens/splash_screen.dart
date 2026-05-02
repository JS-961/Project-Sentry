import "dart:async";

import "package:flutter/material.dart";

import "app_shell.dart";

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key, this.duration = const Duration(milliseconds: 1200)});

  final Duration duration;

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _timer = Timer(widget.duration, _goNext);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: scheme.primary,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(
              "assets/logo.png",
              width: 140,
              height: 140,
            ),
            const SizedBox(height: 16),
            Text(
              "SENTRY",
              style: TextStyle(
                color: scheme.secondary,
                fontSize: 28,
                fontWeight: FontWeight.w800,
                letterSpacing: 2,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              "Safety that's always with you",
              style: TextStyle(
                color: scheme.secondary.withValues(alpha: 0.9),
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _goNext() {
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const AppShell()),
    );
  }
}
