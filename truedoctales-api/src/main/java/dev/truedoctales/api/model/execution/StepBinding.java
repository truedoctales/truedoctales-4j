package dev.truedoctales.api.model.execution;

import java.util.List;

/// Represents the binding between a binding pattern and its execution method.
///
/// Associates a plot name and binding pattern with the Java method that implements the binding.
public record StepBinding(
    String plot,
    String pattern,
    InputType inputType,
    String description,
    List<String> headers,
    List<String> variableDescriptions) {

  /// Creates a step binding without a description, headers and variable descriptions
  /// (backward-compatible constructor).
  public StepBinding(String plot, String pattern, InputType inputType) {
    this(plot, pattern, inputType, "", List.of(), List.of());
  }

  /// Creates a step binding without headers and variable descriptions (backward-compatible
  /// constructor).
  public StepBinding(String plot, String pattern, InputType inputType, String description) {
    this(plot, pattern, inputType, description, List.of(), List.of());
  }

  /// Creates a step binding without variable descriptions (backward-compatible constructor).
  public StepBinding(
      String plot, String pattern, InputType inputType, String description, List<String> headers) {
    this(plot, pattern, inputType, description, headers, List.of());
  }
}
