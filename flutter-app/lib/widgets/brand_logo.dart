import "package:flutter/material.dart";

class BrandLogo extends StatelessWidget {
  const BrandLogo({super.key, this.size = 28});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 12),
      child: Image.asset(
        "assets/logo.png",
        width: size,
        height: size,
        fit: BoxFit.contain,
      ),
    );
  }
}
