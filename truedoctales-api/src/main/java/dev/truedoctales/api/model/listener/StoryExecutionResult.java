package dev.truedoctales.api.model.listener;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.truedoctales.api.model.execution.StoryExecution;
import java.util.ArrayList;
import java.util.List;

/// Represents the result of executing a story.
///
/// Contains the story execution, prequel results, and scene results. The status is computed
/// based on the scene results.
public class StoryExecutionResult implements HasExecutionStatus {
  private final StoryExecution execution;
  private final List<StoryExecutionResult> prequelResults = new ArrayList<>();
  private final List<SceneExecutionResult> sceneResults = new ArrayList<>();

  /// Creates a story execution result.
  ///
  /// @param execution the story execution detales
  public StoryExecutionResult(StoryExecution execution) {
    this.execution = execution;
  }

  /// Creates a story execution result with prequels and scenes (for Jackson deserialization).
  ///
  /// @param execution the story execution detales
  /// @param prequelResults the prequel results
  /// @param sceneResults the scene results
  @JsonCreator
  public StoryExecutionResult(
      @JsonProperty("execution") StoryExecution execution,
      @JsonProperty("prequelResults") List<StoryExecutionResult> prequelResults,
      @JsonProperty("sceneResults") List<SceneExecutionResult> sceneResults) {
    this.execution = execution;
    if (prequelResults != null) {
      this.prequelResults.addAll(prequelResults);
    }
    if (sceneResults != null) {
      this.sceneResults.addAll(sceneResults);
    }
  }

  /// Returns the story execution detales.
  ///
  /// @return the story execution
  public StoryExecution execution() {
    return execution;
  }

  /// Returns the results of prequel stories.
  ///
  /// @return list of prequel execution results
  public List<StoryExecutionResult> prequelResults() {
    return prequelResults;
  }

  /// Returns the results of all scenes in this story.
  ///
  /// @return list of scene execution results
  public List<SceneExecutionResult> sceneResults() {
    return sceneResults;
  }

  /// Adds a prequel execution result.
  ///
  /// @param prequelResult the prequel result to add
  public void addPrequelResult(StoryExecutionResult prequelResult) {
    prequelResults.add(prequelResult);
  }

  /// Adds a scene execution result.
  ///
  /// @param sceneResult the scene result to add
  public void addSceneResult(SceneExecutionResult sceneResult) {
    sceneResults.add(sceneResult);
  }

  @Override
  public ExecutionStatus status() {
    return ExecutionStatusCalculator.computeStatus(sceneResults);
  }
}
