package dev.truedoctales.api.model.listener;

/// Interface for objects that have an execution status.
///
/// This allows generic status computation across different result types.
public interface HasExecutionStatus {
  /// Returns the execution status of this object.
  ///
  /// @return the execution status
  ExecutionStatus status();
}
