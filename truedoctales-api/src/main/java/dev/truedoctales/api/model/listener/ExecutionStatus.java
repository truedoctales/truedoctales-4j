package dev.truedoctales.api.model.listener;

/// Represents the execution status of a test scenario, act, story, or chapter.
public enum ExecutionStatus {
  /// The execution completed successfully without errors.
  SUCCESS,

  /// The execution failed due to an assertion failure or test logic error.
  FAILURE,

  /// The execution encountered an unexpected error or exception.
  ERROR,

  /// The execution was skipped.
  SKIPPED
}
