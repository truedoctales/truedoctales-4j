package dev.truedoctales.report.html;

/// Utility class for escaping HTML special characters to prevent XSS vulnerabilities.
final class HtmlEscaper {

  private HtmlEscaper() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /// Escapes HTML special characters in the given text.
  ///
  /// - `&` is converted to `&amp;`
  /// - `<` is converted to `&lt;`
  /// - `>` is converted to `&gt;`
  /// - `"` is converted to `&quot;`
  /// - `'` is converted to `&#39;`
  ///
  /// @param text the text to escape, may be null
  /// @return the escaped text, or empty string if input is null
  static String escape(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
