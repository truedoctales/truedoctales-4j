package dev.truedoctales.api.model.execution;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/// Execution model for the entire story book.
///
/// Contains the book path, title, intro chapter, and all story chapters ready for execution.
public record StoryBookExecution(
    Path path, String title, ChapterExecution prequelChapter, List<ChapterExecution> chapters) {

  /// Loads a story by its path from the book structure.
  ///
  /// @param path the path to the story
  /// @return the story execution
  /// @throws IllegalArgumentException if the story is not found
  public StoryExecution loadStory(Path path) {
    Stream<ChapterExecution> allChapters = chapters.stream();
    if (prequelChapter != null) {
      allChapters = Stream.concat(chapters.stream(), Stream.of(prequelChapter));
    }
    return allChapters
        .flatMap(chapter -> chapter.stories().stream())
        .filter(story -> story.path().equals(path))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Story not found: " + path));
  }

  /// Finds the chapter containing a given story.
  ///
  /// @param story the story execution
  /// @return Optional containing the chapter, or empty if not found
  public Optional<ChapterExecution> findChapterForStory(StoryExecution story) {
    Path chapterPath = story.path().getParent();
    Stream<ChapterExecution> allChapters = chapters.stream();
    if (prequelChapter != null) {
      allChapters = Stream.concat(chapters.stream(), Stream.of(prequelChapter));
    }
    return allChapters.filter(chapter -> chapter.path().equals(chapterPath)).findFirst();
  }
}
