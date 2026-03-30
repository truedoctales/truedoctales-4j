package dev.truedoctales.api.model.execution;

import dev.truedoctales.api.model.plot.StepBinding;
import dev.truedoctales.api.model.story.StepCall;
import java.util.List;
import java.util.Map;

/// Represents a binding execution with all necessary data for invocation.
///
/// Contains the binding binding, original value from markdown, table data, line number, optional
/// description, and extracted inplaceVariables for method parameter binding.
public record StepExecution(
    int lineNumber,
    StepBinding binding,
    StepCall call,
    Map<String, String> inplaceVariables,
    List<Map<String, String>> tableData) {

  public static StepExecution simplCall(int lineNumber, StepBinding binding, StepCall call) {
    return new StepExecution(lineNumber, binding, call, Map.of(), List.of());
  }

  public static StepExecution inplace(
      int lineNumber, StepBinding binding, StepCall call, Map<String, String> inplaceVariables) {
    return new StepExecution(lineNumber, binding, call, inplaceVariables, List.of());
  }

  public static StepExecution table(
      int lineNumber,
      StepBinding binding,
      StepCall call,
      List<Map<String, String>> tableVariables) {
    return new StepExecution(lineNumber, binding, call, Map.of(), tableVariables);
  }
}
