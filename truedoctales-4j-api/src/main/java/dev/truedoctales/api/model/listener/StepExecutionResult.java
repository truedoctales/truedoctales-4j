package dev.truedoctales.api.model.listener;

import dev.truedoctales.api.model.execution.InputType;
import dev.truedoctales.api.model.execution.StepExecution;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Represents the result of executing a single binding.
///
/// @param status the execution status (SUCCESS, FAILURE, ERROR)
/// @param errorMessage optional error message if the binding failed
/// @param errorType optional class name of the exception that caused the error
/// @param description optional markdown description from the {@code @Step} annotation
/// @param rowStatuses per-row execution statuses for SEQUENCE steps with table data
public record StepExecutionResult(
    int lineNumber,
    String plot,
    String pattern,
    InputType inputType,
    Map<String, String> variables,
    List<Map<String, String>> stepData,
    ExecutionStatus status,
    @Nullable String errorMessage,
    @Nullable String errorType,
    String description,
    List<ExecutionStatus> rowStatuses)
    implements HasExecutionStatus {

  /// Creates a successful binding execution result.
  ///
  /// @param step the binding execution details
  public StepExecutionResult(StepExecution step) {

    this(
        step.lineNumber(),
        step.binding().plot(),
        step.binding().pattern(),
        step.binding().inputType(),
        step.inplaceVariables(),
        step.tableData(),
        ExecutionStatus.SUCCESS,
        null,
        null,
        step.binding().description(),
        buildAllSuccessRowStatuses(step.tableData()));
  }

  public StepExecutionResult(StepExecution step, Throwable throwable) {

    this(
        step.lineNumber(),
        step.binding().plot(),
        step.binding().pattern(),
        step.binding().inputType(),
        step.inplaceVariables(),
        step.tableData(),
        ExecutionStatus.ERROR,
        throwable.getMessage(),
        throwable.getClass().getName(),
        step.binding().description(),
        List.of());
  }

  /// Constructor with per-row statuses for SEQUENCE steps.
  public StepExecutionResult(
      StepExecution step,
      Throwable throwable,
      ExecutionStatus overallStatus,
      List<ExecutionStatus> rowStatuses) {
    this(
        step.lineNumber(),
        step.binding().plot(),
        step.binding().pattern(),
        step.binding().inputType(),
        step.inplaceVariables(),
        step.tableData(),
        overallStatus,
        throwable != null ? throwable.getMessage() : null,
        throwable != null ? throwable.getClass().getName() : null,
        step.binding().description(),
        rowStatuses);
  }

  /// Backward-compatible constructor without description and rowStatuses.
  public StepExecutionResult(
      int lineNumber,
      String plot,
      String pattern,
      InputType inputType,
      Map<String, String> variables,
      List<Map<String, String>> stepData,
      ExecutionStatus status,
      @Nullable String errorMessage,
      @Nullable String errorType) {
    this(
        lineNumber,
        plot,
        pattern,
        inputType,
        variables,
        stepData,
        status,
        errorMessage,
        errorType,
        "",
        List.of());
  }

  /// Backward-compatible constructor without rowStatuses.
  public StepExecutionResult(
      int lineNumber,
      String plot,
      String pattern,
      InputType inputType,
      Map<String, String> variables,
      List<Map<String, String>> stepData,
      ExecutionStatus status,
      @Nullable String errorMessage,
      @Nullable String errorType,
      String description) {
    this(
        lineNumber,
        plot,
        pattern,
        inputType,
        variables,
        stepData,
        status,
        errorMessage,
        errorType,
        description,
        List.of());
  }

  private static List<ExecutionStatus> buildAllSuccessRowStatuses(
      List<Map<String, String>> tableData) {
    if (tableData == null || tableData.isEmpty()) {
      return List.of();
    }
    return tableData.stream().map(_ -> ExecutionStatus.SUCCESS).toList();
  }
}
