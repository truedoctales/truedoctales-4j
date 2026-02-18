package dev.truedoctales.api.model.story;

import org.jspecify.annotations.NonNull;

/// Domain model representing markdown documentation within a scene.
///
/// A StepDescription holds markdown text that appears between executable steps, providing context,
/// explanations, or commentary. This allows test scenarios to be self-documenting and more
/// readable.
///
/// StepDescriptions are not executed but are rendered in reports to provide narrative context.
public record StepDescription(@NonNull String markdown, int lineNumber) implements Step {
  public StepDescription {
    if (lineNumber < 0) {
      lineNumber = 0;
    }
  }
}
