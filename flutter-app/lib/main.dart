import "package:flutter/material.dart";
import "package:google_fonts/google_fonts.dart";

import "screens/splash_screen.dart";

void main() {
  runApp(const SentryApp());
}

class SentryApp extends StatelessWidget {
  const SentryApp({super.key});

  @override
  Widget build(BuildContext context) {
    const sentryNavy = Color(0xFF0B1028);
    const sentryYellow = Color(0xFFF5C400);
    const sentrySurface = Color(0xFFF7F8FB);
    const sentryRed = Color(0xFFDC2626);

    final scheme = ColorScheme.fromSeed(
      seedColor: sentryYellow,
      brightness: Brightness.light,
    ).copyWith(
      primary: sentryNavy,
      secondary: sentryYellow,
      surface: Colors.white,
      error: sentryRed,
      onPrimary: Colors.white,
      onSecondary: Colors.black,
      onSurface: Colors.black,
    );

    return MaterialApp(
      title: "Project Sentry",
      theme: ThemeData(
        colorScheme: scheme,
        useMaterial3: true,
        scaffoldBackgroundColor: sentrySurface,
        textTheme: GoogleFonts.spaceGroteskTextTheme().copyWith(
          titleLarge: GoogleFonts.spaceGrotesk(
            fontSize: 22,
            fontWeight: FontWeight.w700,
          ),
          titleMedium: GoogleFonts.spaceGrotesk(
            fontSize: 18,
            fontWeight: FontWeight.w600,
          ),
          bodyLarge: GoogleFonts.spaceGrotesk(fontSize: 16),
          bodyMedium: GoogleFonts.spaceGrotesk(fontSize: 14),
        ),
        cardTheme: CardThemeData(
          elevation: 0.6,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
        listTileTheme: const ListTileThemeData(
          iconColor: sentryNavy,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: sentryNavy,
          foregroundColor: sentryYellow,
        ),
        bottomNavigationBarTheme: const BottomNavigationBarThemeData(
          backgroundColor: sentryNavy,
          selectedItemColor: sentryYellow,
          unselectedItemColor: Colors.white70,
          type: BottomNavigationBarType.fixed,
        ),
      ),
      home: const SplashScreen(),
    );
  }
}
