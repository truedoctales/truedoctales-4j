package dev.truedoctales.api.model.listener;

import java.util.ArrayList;
import java.util.List;

/// Represents the result of executing a story.
///
/// Contains the story execution, prequel results, and scene results. The status is computed
/// based on the scene results.
public class StoryExecutionResult implements HasExecutionStatus {
  private String path;
  private String title;
  private List<StoryExecutionResult> prequelResults;
  private List<SceneExecutionResult> sceneResults;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<StoryExecutionResult> getPrequelResults() {
    return prequelResults;
  }

  public void setPrequelResults(List<StoryExecutionResult> prequelResults) {
    this.prequelResults = prequelResults;
  }

  public List<SceneExecutionResult> getSceneResults() {
    return sceneResults;
  }

  public void setSceneResults(List<SceneExecutionResult> sceneResults) {
    this.sceneResults = sceneResults;
  }

  @Override
  public ExecutionStatus status() {
    return ExecutionStatusCalculator.computeStatus(sceneResults);
  }

  public void addPrequelResult(StoryExecutionResult currentStory) {
    if (prequelResults == null) {
      prequelResults = new ArrayList<>();
    }
    prequelResults.add(currentStory);
  }

  public void addSceneResult(SceneExecutionResult result) {
    if (sceneResults == null) {
      sceneResults = new ArrayList<>();
    }
    sceneResults.add(result);
  }
}
