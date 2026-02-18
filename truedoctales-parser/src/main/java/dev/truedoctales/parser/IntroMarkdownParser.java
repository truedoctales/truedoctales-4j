package dev.truedoctales.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// Parser for intro.md files that extracts title and summary.
///
/// ### Expected Format
/// ```markdown
/// # Title
///
/// Summary content in markdown format.
/// It can include:
/// - Lists
/// - **Bold** and *italic* text
/// - Code blocks
/// - Multiple paragraphs
/// - etc.
/// ```
///
/// The intro file is named 00_intro.md to ensure it appears first in alphabetical ordering.
public class IntroMarkdownParser {

  /// Parses an intro.md file and returns the extracted content.
  ///
  /// @param introFilePath Path to the 00_intro.md file
  /// @return IntroContent containing title and summary (as markdown), or null if file doesn't exist
  /// @throws IOException if an I/O error occurs while reading the file
  public IntroContent parse(Path introFilePath) throws IOException {
    if (!Files.exists(introFilePath)) {
      return null;
    }

    List<String> lines = Files.readAllLines(introFilePath);
    if (lines.isEmpty()) {
      return null;
    }

    String title = null;
    StringBuilder summary = new StringBuilder();
    boolean foundTitle = false;

    for (String line : lines) {
      String trimmedLine = line.trim();

      // Extract title from the first H1 heading
      if (trimmedLine.startsWith("# ") && title == null) {
        title = trimmedLine.substring(2).trim();
        foundTitle = true;
        continue;
      }

      // Collect all remaining lines as markdown summary (after title is found)
      if (foundTitle) {
        if (!summary.isEmpty()) {
          summary.append("\n");
        }
        summary.append(line); // Keep original line with indentation for markdown formatting
      }
    }

    // Trim leading/trailing whitespace from summary but preserve internal formatting
    String summaryText = summary.toString().trim();

    return new IntroContent(title, summaryText.isEmpty() ? null : summaryText);
  }

  /// Record containing the parsed intro content.
  public record IntroContent(String title, String summary) {}
}
