// ignore_for_file: avoid_web_libraries_in_flutter, deprecated_member_use

import "dart:html" as html;

Future<String?> saveTextFile({
  required String fileName,
  required String content,
}) async {
  final blob = html.Blob([content], "text/plain;charset=utf-8");
  final url = html.Url.createObjectUrlFromBlob(blob);
  final anchor = html.AnchorElement(href: url)
    ..download = fileName
    ..style.display = "none";
  html.document.body?.append(anchor);
  anchor.click();
  anchor.remove();
  html.Url.revokeObjectUrl(url);
  return fileName;
}

Future<String?> saveBinaryFile({
  required String fileName,
  required List<int> bytes,
  String mimeType = "application/octet-stream",
}) async {
  final blob = html.Blob([bytes], mimeType);
  final url = html.Url.createObjectUrlFromBlob(blob);
  final anchor = html.AnchorElement(href: url)
    ..download = fileName
    ..style.display = "none";
  html.document.body?.append(anchor);
  anchor.click();
  anchor.remove();
  html.Url.revokeObjectUrl(url);
  return fileName;
}
