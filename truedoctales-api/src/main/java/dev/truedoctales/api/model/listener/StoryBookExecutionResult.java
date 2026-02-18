package dev.truedoctales.api.model.listener;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.util.ArrayList;
import java.util.List;

/// Represents the result of executing an entire story book.
///
/// Contains the book model and the results of all chapters within the book.
public class StoryBookExecutionResult {
  private final StoryBookModel book;
  private final List<ChapterExecutionResult> chapters = new ArrayList<>();

  /// Creates a story book execution result.
  ///
  /// @param book the story book model
  public StoryBookExecutionResult(StoryBookModel book) {
    this.book = book;
  }

  /// JSON deserialization constructor.
  ///
  /// @param book the story book model
  /// @param chapters the list of chapter execution results
  @JsonCreator
  public StoryBookExecutionResult(
      @JsonProperty("book") StoryBookModel book,
      @JsonProperty("chapters") List<ChapterExecutionResult> chapters) {
    this.book = book;
    if (chapters != null) {
      this.chapters.addAll(chapters);
    }
  }

  /// Returns the story book model.
  ///
  /// @return the story book model
  public StoryBookModel book() {
    return book;
  }

  /// Returns the results of all chapters in this book.
  ///
  /// @return list of chapter execution results
  public List<ChapterExecutionResult> chapterResults() {
    return chapters;
  }

  /// Adds a chapter execution result.
  ///
  /// @param chapterResult the chapter result to add
  public void addChapterResult(ChapterExecutionResult chapterResult) {
    chapters.add(chapterResult);
  }
}
