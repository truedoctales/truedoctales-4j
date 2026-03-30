package dev.truedoctales.api.model.listener;

import java.util.List;

/// Utility class for calculating execution status from a list of results.
///
/// Implements the common logic for determining overall status based on child results.
public final class ExecutionStatusCalculator {

  private ExecutionStatusCalculator() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /// Computes the overall execution status from a list of child results.
  ///
  /// The status is determined by the most severe status in the list: ERROR > FAILURE > SUCCESS.
  ///
  /// @param results the list of results with status information
  /// @param <T> the type of result that has a status
  /// @return the computed overall status
  public static <T extends HasExecutionStatus> ExecutionStatus computeStatus(List<T> results) {
    boolean hasError = results.stream().anyMatch(r -> r.status() == ExecutionStatus.ERROR);
    boolean hasFailure = results.stream().anyMatch(r -> r.status() == ExecutionStatus.FAILURE);

    if (hasError) return ExecutionStatus.ERROR;
    if (hasFailure) return ExecutionStatus.FAILURE;
    return ExecutionStatus.SUCCESS;
  }
}
