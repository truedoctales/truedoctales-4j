package dev.truedoctales.execution.execute;

import dev.truedoctales.api.model.listener.ExecutionStatus;
import java.util.List;

/// Exception thrown when one or more rows in a SEQUENCE step execution fail.
///
/// Carries per-row execution statuses so the caller can create a result
/// with detailed row-level outcomes.
public class SequenceRowFailureException extends Exception {

  private final List<ExecutionStatus> rowStatuses;

  /// Creates a new exception with per-row statuses and the first error encountered.
  ///
  /// @param rowStatuses per-row execution statuses
  /// @param firstError the first error encountered during row execution
  public SequenceRowFailureException(List<ExecutionStatus> rowStatuses, Throwable firstError) {
    super(firstError.getMessage(), firstError);
    this.rowStatuses = List.copyOf(rowStatuses);
  }

  /// Returns the per-row execution statuses.
  public List<ExecutionStatus> rowStatuses() {
    return rowStatuses;
  }
}
