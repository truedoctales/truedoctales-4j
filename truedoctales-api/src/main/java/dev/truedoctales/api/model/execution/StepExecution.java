package dev.truedoctales.api.model.execution;

import dev.truedoctales.api.model.story.StepCall;
import java.util.List;
import java.util.Map;

/// Represents a binding execution with all necessary data for invocation.
///
/// Contains the binding binding, original value from markdown, table data, line number, optional
/// description, and extracted variables for method parameter binding.
public record StepExecution(
    StepBinding binding,
    StepCall call,
    List<Map<String, String>> stepData,
    int lineNumber,
    Map<String, String> variables) {

  /// Creates a binding execution without variables.
  ///
  /// @param step the binding binding
  /// @param stepCall the original binding call from markdown
  /// @param stepData the table data rows
  /// @param lineNumber the line number in the markdown file
  public StepExecution(
      StepBinding step, StepCall stepCall, List<Map<String, String>> stepData, int lineNumber) {
    this(step, stepCall, stepData, lineNumber, Map.of());
  }
}
