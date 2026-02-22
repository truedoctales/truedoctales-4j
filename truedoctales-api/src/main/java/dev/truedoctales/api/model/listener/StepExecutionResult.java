package dev.truedoctales.api.model.listener;

import dev.truedoctales.api.model.execution.StepExecution;
import org.jspecify.annotations.Nullable;

/// Represents the result of executing a single binding.
///
/// @param execution the binding execution detales
/// @param status the execution status (SUCCESS, FAILURE, ERROR)
/// @param errorMessage optional error message if the binding failed
/// @param throwable optional exception if the binding encountered an error
public record StepExecutionResult(
    StepExecution execution,
    ExecutionStatus status,
    @Nullable String errorMessage,
    @Nullable Throwable throwable)
    implements HasExecutionStatus {

  /// Creates a successful binding execution result.
  ///
  /// @param step the binding execution detales
  public StepExecutionResult(StepExecution step) {
    this(step, ExecutionStatus.SUCCESS, null, null);
  }

  /// Creates a binding execution result with an error.
  ///
  /// @param step the binding execution detales
  /// @param throwable the exception that occurred
  public StepExecutionResult(StepExecution step, Throwable throwable) {
    this(step, ExecutionStatus.ERROR, throwable.getMessage(), throwable);
  }

  /// Creates a binding execution result with a failure message.
  ///
  /// @param step the binding execution detales
  /// @param errorMessage the failure message
  public StepExecutionResult(StepExecution step, String errorMessage) {
    this(step, ExecutionStatus.FAILURE, errorMessage, null);
  }
}
