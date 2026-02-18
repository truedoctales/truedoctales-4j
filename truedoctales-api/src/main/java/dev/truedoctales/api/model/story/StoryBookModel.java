package dev.truedoctales.api.model.story;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Domain model representing a Book - the complete test suite structure.
///
/// A Book represents the entire test book structure, typically corresponding to a directory
/// containing chapter subdirectories. Each chapter contains multiple story markdown files.
///
/// The book structure:
/// - bookPath: Root directory path of the book
/// - chapters: List of chapters in the book (folders with stories)
/// - prequelChapters: Special chapters marked with "00_" prefix that contain setup stories
///
/// A Book may optionally contain an intro.md file at the root level that provides a title and
/// summary for the book.
///
/// ### Example Structure
/// ```
/// book-of-stories/
///   intro.md             (book-level introduction - optional)
///   00_prequels/         (prequel chapter - not executed as stories)
///     setup-data.md
///   01_chapter-users/    (regular chapter)
///     intro.md           (chapter-level introduction - optional)
///     create-user.md
///     delete-user.md
///   02_chapter-orders/   (regular chapter)
///     create-order.md
/// ```
public record StoryBookModel(
    @NonNull Path path,
    @NonNull String title,
    @Nullable String summary,
    @Nullable ChapterModel intro,
    @NonNull List<ChapterModel> chapters) {

  /// Returns the optional intro chapter for this book.
  ///
  /// @return Optional containing the intro chapter, or empty if none exists
  public Optional<ChapterModel> getIntro() {
    return Optional.ofNullable(intro);
  }
}
