package dev.truedoctales.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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

    Optional<String> title =
        Files.lines(introFilePath)
            .filter(l -> l.startsWith("# "))
            .map(l -> l.substring(2).trim())
            .findFirst();

    return new IntroContent(title.orElse(null));
  }

  /// Record containing the parsed intro content.
  public record IntroContent(String title) {}
}
