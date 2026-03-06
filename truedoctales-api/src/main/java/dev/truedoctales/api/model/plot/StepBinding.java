package dev.truedoctales.api.model.plot;

import dev.truedoctales.api.model.execution.InputType;
import java.util.List;

/// Represents the binding between a binding pattern and its execution method.
///
/// Associates a plot name and binding pattern with the Java method that implements the binding.
public record StepBinding(
    String plot,
    String pattern,
    InputType inputType,
    String description,
    List<VariableBinding> variables,
    List<VariableBinding> headers) {

  public StepBinding(String plot, String pattern, InputType inputType) {
    this(plot, pattern, inputType, null, List.of(), List.of());
  }

  public StepBinding(String plot, String pattern, InputType inputType, String description) {
    this(plot, pattern, inputType, description, List.of(), List.of());
  }
}
