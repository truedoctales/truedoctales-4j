package dev.truedoctales.api.model.story;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/// Domain model representing an executable test binding with its annotation type and data.
///
/// A StepTask represents an individual action or assertion in the story, annotated with @Step.
/// Steps can include data tables for parameterized testing.
///
/// StepTasks are executable units within Scenes and correspond to @Step(Plot) or ### PlotName:
/// StepDescription directives in markdown files.
public record StepTask(int lineNumber, @NonNull StepCall call, List<Map<String, String>> inputRows)
    implements Step {

  public StepTask(int lineNumber, @NonNull StepCall call) {
    this(lineNumber, call, List.of());
  }

  public StepTask {
    if (inputRows == null) {
      inputRows = List.of();
    }
    if (lineNumber < 0) {
      lineNumber = 0; // Allow 0 for steps without line number tracking
    }
  }
}
