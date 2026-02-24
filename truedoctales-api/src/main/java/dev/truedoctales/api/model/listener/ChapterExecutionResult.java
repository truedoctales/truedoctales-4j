package dev.truedoctales.api.model.listener;

import java.util.List;

/// Represents the result of executing a chapter.
///
/// Contains the chapter model and the results of all stories within the chapter. The status is
/// computed based on the story results.
public class ChapterExecutionResult implements HasExecutionStatus {
  private String path;
  private String title;
  private List<StoryExecutionResult> stories;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<StoryExecutionResult> getStories() {
    return stories;
  }

  public void setStories(List<StoryExecutionResult> stories) {
    this.stories = stories;
  }

  public void addStoryResult(StoryExecutionResult currentStory) {
    if (stories == null) {
      stories = new java.util.ArrayList<>();
    }
    stories.add(currentStory);
  }

  @Override
  public ExecutionStatus status() {
    return ExecutionStatusCalculator.computeStatus(stories);
  }
}
