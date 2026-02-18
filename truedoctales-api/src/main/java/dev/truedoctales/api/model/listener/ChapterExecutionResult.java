package dev.truedoctales.api.model.listener;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.truedoctales.api.model.story.ChapterModel;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;

/// Represents the result of executing a chapter.
///
/// Contains the chapter model and the results of all stories within the chapter. The status is
/// computed based on the story results.
public class ChapterExecutionResult implements HasExecutionStatus {
  private final ChapterModel chapter;
  private final List<StoryExecutionResult> stories = new ArrayList<>();

  /// Creates a chapter execution result.
  ///
  /// @param chapter the chapter model
  public ChapterExecutionResult(@NonNull ChapterModel chapter) {
    this.chapter = chapter;
  }

  /// Creates a chapter execution result with stories (for Jackson deserialization).
  ///
  /// @param chapter the chapter model
  /// @param stories the story results
  @JsonCreator
  public ChapterExecutionResult(
      @JsonProperty("chapter") ChapterModel chapter,
      @JsonProperty("stories") List<StoryExecutionResult> stories) {
    this.chapter = chapter;
    if (stories != null) {
      this.stories.addAll(stories);
    }
  }

  /// Returns the chapter model.
  ///
  /// @return the chapter model
  public @NonNull ChapterModel chapter() {
    return chapter;
  }

  /// Returns the results of all stories in this chapter.
  ///
  /// @return list of story execution results
  public @NonNull List<StoryExecutionResult> storyResults() {
    return stories;
  }

  /// Adds a story execution result.
  ///
  /// @param storyResult the story result to add
  public void addStoryResult(@NonNull StoryExecutionResult storyResult) {
    stories.add(storyResult);
  }

  @Override
  public ExecutionStatus status() {
    return ExecutionStatusCalculator.computeStatus(stories);
  }
}
