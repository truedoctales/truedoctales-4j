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
/// @param throwable optional exception if the binding encountered an error
/// @param description optional markdown description from the {@code @Step} annotation
public record StepExecutionResult(
    int lineNumber,
    String plot,
    String pattern,
    InputType inputType,
    Map<String, String> variables,
    List<Map<String, String>> stepData,
    ExecutionStatus status,
    @Nullable String errorMessage,
    @Nullable Throwable throwable,
    String description)
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
        step.variables(),
        step.stepData(),
        ExecutionStatus.SUCCESS,
        null,
        null,
        step.binding().description());
  }

  public StepExecutionResult(StepExecution step, Throwable throwable) {

    this(
        step.lineNumber(),
        step.binding().plot(),
        step.binding().pattern(),
        step.binding().inputType(),
        step.variables(),
        step.stepData(),
        ExecutionStatus.ERROR,
        throwable.getMessage(),
        throwable,
        step.binding().description());
  }

  /// Backward-compatible constructor without description (description defaults to {@code ""}).
  public StepExecutionResult(
      int lineNumber,
      String plot,
      String pattern,
      InputType inputType,
      Map<String, String> variables,
      List<Map<String, String>> stepData,
      ExecutionStatus status,
      @Nullable String errorMessage,
      @Nullable Throwable throwable) {
    this(
        lineNumber,
        plot,
        pattern,
        inputType,
        variables,
        stepData,
        status,
        errorMessage,
        throwable,
        "");
  }
}
