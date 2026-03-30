package dev.truedoctales.api.model.plot;

import dev.truedoctales.api.model.execution.InputType;
import java.util.List;

/// Represents the binding between a step pattern and its execution method.
///
/// Associates a plot name, step pattern, input type, and descriptive metadata with the
/// Java method that implements the step. The {@code inplaceVariables} list contains the
/// {@link VariableBinding}s extracted from {@code @Variable}-annotated parameters, and
/// the {@code tableVariables} list contains the table column metadata from {@code @Table}.
///
/// Serialised into {@code plot-registry.json} by the JSON story listener and consumed
/// by the plot glossary generator to produce documentation.
public record StepBinding(
    String plot,
    String pattern,
    InputType inputType,
    String description,
    List<VariableBinding> inplaceVariables,
    List<VariableBinding> tableVariables) {

  public StepBinding(String plot, String pattern, InputType inputType) {
    this(plot, pattern, inputType, null, List.of(), List.of());
  }

  public StepBinding(String plot, String pattern, InputType inputType, String description) {
    this(plot, pattern, inputType, description, List.of(), List.of());
  }
}
