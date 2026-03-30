package dev.truedoctales.parser;

import dev.truedoctales.api.model.story.StoryModel;
import java.io.IOException;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;

/// Interface for parsing markdown story files.
///
/// Implementations parse markdown files into structured StoryModel domain objects.
public interface MarkdownStoryParser {

  /// Parses a markdown file and returns a structured domain model (metamodel).
  ///
  /// @param rootDir the root directory for resolving relative paths
  /// @param storyPath the path to the story file for reference
  /// @return a StoryModel containing all scenarios and plots as a metamodel
  /// @throws IOException if an I/O error occurs while reading the file
  /// @throws IllegalArgumentException if the Markdown format is invalid
  StoryModel parse(@NonNull Path rootDir, @NonNull Path storyPath) throws IOException;
}
