package dev.truedoctales.api.model.listener;

import java.util.ArrayList;
import java.util.List;

/// Represents the result of executing an entire story book.
///
/// Contains the book model and the results of all chapters within the book.
public class StoryBookExecutionResult {
  private String title;
  private List<ChapterExecutionResult> chapters;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<ChapterExecutionResult> getChapters() {
    return chapters;
  }

  public void setChapters(List<ChapterExecutionResult> chapters) {
    this.chapters = chapters;
  }

  public void addChapterResult(ChapterExecutionResult chapterResult) {
    if (chapters == null) {
      chapters = new ArrayList<>();
    }
    chapters.add(chapterResult);
  }
}
