package dev.truedoctales.api.model.execution;

import java.util.List;

/// Represents the binding between a binding pattern and its execution method.
///
/// Associates a plot name and binding pattern with the Java method that implements the binding.
public record StepBinding(
    String plot, String pattern, InputType inputType, String description, List<String> headers) {

  /// Creates a step binding without a description and headers (backward-compatible constructor).
  public StepBinding(String plot, String pattern, InputType inputType) {
    this(plot, pattern, inputType, "", List.of());
  }

  /// Creates a step binding without headers (backward-compatible constructor).
  public StepBinding(String plot, String pattern, InputType inputType, String description) {
    this(plot, pattern, inputType, description, List.of());
  }
}
