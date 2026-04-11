import "package:flutter/services.dart";

class AssetLoader {
  const AssetLoader();

  Future<String?> loadOptional(String path) async {
    try {
      return await rootBundle.loadString(path);
    } catch (_) {
      return null;
    }
  }
}
