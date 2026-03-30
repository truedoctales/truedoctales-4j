package dev.truedoctales.api.internal.parsing;

import dev.truedoctales.api.model.story.StoryBookModel;
import java.io.IOException;

/// Parser for parsing entire book structures from directory hierarchies.
///
/// A BookParser reads a directory structure containing chapters (subdirectories) and stories
/// (markdown files) and creates a BookModel representing the complete book structure.
///
/// The parser recognizes special naming conventions:
/// - Folders starting with "00_" are treated as prequel chapters
/// - Folder names like "01_chapter-title" are parsed to extract chapter names
/// - Each markdown file in a chapter directory represents a story
public interface StoryBookParser {

  /// Parses a book directory structure and returns a complete BookModel.
  ///
  /// @return a BookModel containing all chapters and their stories
  /// @throws IOException if an I/O error occurs while reading files
  /// @throws IllegalArgumentException if the directory structure is invalid
  StoryBookModel parse() throws IOException;
}
