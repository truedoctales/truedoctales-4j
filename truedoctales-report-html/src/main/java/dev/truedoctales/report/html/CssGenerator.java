package dev.truedoctales.report.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/// Loads the CSS stylesheet for the Book of Truth report from resources.
/// This class reads the CSS file from the classpath resources instead of generating it
// programmatically.
final class CssGenerator {

  private static final String CSS_RESOURCE_PATH = "/ttt.css";

  private CssGenerator() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /// Loads the complete CSS stylesheet from resources.
  ///
  /// @return The complete CSS content as a string
  /// @throws RuntimeException if the CSS file cannot be loaded
  static String generateCss() {
    try (InputStream inputStream = CssGenerator.class.getResourceAsStream(CSS_RESOURCE_PATH)) {
      if (inputStream == null) {
        throw new RuntimeException("CSS resource not found: " + CSS_RESOURCE_PATH);
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load CSS resource: " + CSS_RESOURCE_PATH, e);
    }
  }
}
