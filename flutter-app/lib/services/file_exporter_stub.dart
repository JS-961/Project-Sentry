import "dart:io";

import "package:path_provider/path_provider.dart";

Future<String?> saveTextFile({
  required String fileName,
  required String content,
}) async {
  final directory = await getApplicationDocumentsDirectory();
  final path = "${directory.path}${Platform.pathSeparator}$fileName";
  final file = File(path);
  await file.writeAsString(content, flush: true);
  return file.path;
}

Future<String?> saveBinaryFile({
  required String fileName,
  required List<int> bytes,
  String mimeType = "application/octet-stream",
}) async {
  final directory = await getApplicationDocumentsDirectory();
  final path = "${directory.path}${Platform.pathSeparator}$fileName";
  final file = File(path);
  await file.writeAsBytes(bytes, flush: true);
  return file.path;
}
