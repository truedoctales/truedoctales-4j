package dev.truedoctales.api.model.listener;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.truedoctales.api.model.execution.SceneExecution;
import java.util.List;

/// Represents the result of executing a scene (a group of steps).
///
/// @param stepResults the results of all steps within this scene
/// @param status the overall execution status of the scene
public record SceneExecutionResult(
    String title,
    Integer lineNumber,
    List<StepExecutionResult> stepResults,
    @JsonProperty("status") ExecutionStatus status)
    implements HasExecutionStatus {

  /// Creates a scene execution result with computed status.
  ///
  /// @param scene the scene execution detales
  /// @param stepResults the results of all steps within this scene
  public SceneExecutionResult(SceneExecution scene, List<StepExecutionResult> stepResults) {
    this(
        scene.title(),
        scene.lineNumber(),
        stepResults,
        ExecutionStatusCalculator.computeStatus(stepResults));
  }
}
