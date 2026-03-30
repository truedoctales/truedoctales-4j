package dev.truedoctales.api.execute;

import dev.truedoctales.api.model.execution.SceneExecution;
import dev.truedoctales.api.model.execution.StoryExecution;
import dev.truedoctales.api.model.listener.ChapterExecutionResult;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StoryBookExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.util.ArrayList;
import java.util.List;

/// Story execution listener that persists execution results.
///
/// Extends LoggingStoryExecutionListener to also build a complete tree of execution results
/// for the entire book, chapters, stories, scenes, and steps.
public class PersistStoryListener extends LoggingStoryExecutionListener
    implements StoryExecutionListener {

  private StoryBookExecutionResult book;
  private final List<StoryExecutionResult> stories = new ArrayList<>();

  /// Creates a new persist story listener.
  public PersistStoryListener() {}

  @Override
  public void startBook(StoryBookModel storyBookModel) {
    book = new StoryBookExecutionResult();
    book.setTitle(storyBookModel.title());
  }

  /// Returns the complete book execution result.
  ///
  /// @return the book execution result
  public StoryBookExecutionResult getBookResult() {
    return book;
  }

  @Override
  public void startChapter(ChapterModel execution) {
    ChapterExecutionResult chapterResult = new ChapterExecutionResult();
    chapterResult.setNumber(execution.number());
    chapterResult.setPath(execution.path().toString());
    chapterResult.setTitle(execution.title());
    book.addChapterResult(chapterResult);
  }

  @Override
  public void startStory(StoryExecution execution) {

    var currentStory = new StoryExecutionResult();
    currentStory.setNumber(execution.number());
    currentStory.setPath(execution.path().toString());
    currentStory.setTitle(execution.title());
    if (stories.isEmpty()) {
      book.getChapters().getLast().addStoryResult(currentStory);
    } else {
      stories.getLast().addPrequelResult(currentStory);
    }
    stories.add(currentStory);
  }

  @Override
  public void endStory(StoryExecution result) {
    stories.removeLast();
  }

  @Override
  public void startScene(SceneExecution scene) {}

  @Override
  public void endScene(SceneExecutionResult result) {
    stories.getLast().addSceneResult(result);
  }
}
