package dev.truedoctales.execution.execute;

import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import java.util.logging.Level;
import java.util.logging.Logger;

/// Executes a single test binding by invoking the bound method.
public record StepExecutor(PlotRegistry plotRegistry, StoryExecutionListener executionListener) {

  private static final Logger LOGGER = Logger.getLogger(StepExecutor.class.getName());

  /// Executes a binding with the matched method and returns the result.
  ///
  /// @param execution the binding execution to perform
  /// @return the execution result
  public StepExecutionResult execute(StepExecution execution) {
    executionListener.startStep(execution);
    StepExecutionResult result;
    try {
      plotRegistry.invoke(execution);
      result = new StepExecutionResult(execution);
    } catch (SequenceRowFailureException sre) {
      result = createSequenceRowResult(execution, sre);
    } catch (Throwable e) {
      result = createErrorResult(execution, e);
    }
    executionListener.endStep(result);
    return result;
  }

  private StepExecutionResult createSequenceRowResult(
      StepExecution step, SequenceRowFailureException sre) {
    String lineInfo = step.lineNumber() > 0 ? " at line " + step.lineNumber() : "";
    String error =
        "Error executing binding '" + step.call() + "'" + lineInfo + " (partial row failure)";
    LOGGER.log(Level.WARNING, error, sre);
    executionListener.recordFailure(step, sre.getCause());
    return new StepExecutionResult(
        step, sre.getCause(), ExecutionStatus.FAILURE, sre.rowStatuses());
  }

  private StepExecutionResult createErrorResult(StepExecution step, Throwable e) {
    String lineInfo = step.lineNumber() > 0 ? " at line " + step.lineNumber() : "";
    String error = "Error executing binding '" + step.call() + "'" + lineInfo;
    LOGGER.log(Level.SEVERE, error, e);
    executionListener.recordFailure(step, e);
    return new StepExecutionResult(step, e);
  }
}
