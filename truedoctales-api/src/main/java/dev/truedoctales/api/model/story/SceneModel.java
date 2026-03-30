package dev.truedoctales.api.model.story;

import java.util.List;
import org.jspecify.annotations.NonNull;

/// Domain model representing a Scene within a story.
///
/// A Scene is equivalent to a test method in JUnit and groups related steps that should be
/// executed together as a logical test unit. Scenes call methods from Plot classes via @Step
/// annotations.
///
/// Scenes correspond to "## Scene:" sections in markdown files and may include a description in
/// markdown format that appears before the steps. Steps can be either StepTask (executable) or
/// StepDescription (markdown documentation).
public record SceneModel(
    @NonNull String title, @NonNull Integer startLineNumber, @NonNull List<StepTask> steps) {
  public SceneModel {
    if (startLineNumber < 1) {
      throw new IllegalArgumentException("Start line number must be positive");
    }
  }
}
