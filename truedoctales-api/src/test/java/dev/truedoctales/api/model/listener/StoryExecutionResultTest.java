package dev.truedoctales.api.model.listener;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.execution.StoryExecution;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StoryExecutionResult")
class StoryExecutionResultTest {

  private static final Path STORY_PATH = Path.of("chapters", "chapter-01", "story.md");

  @Nested
  @DisplayName("constructor(StoryExecution)")
  class SingleParameterConstructor {

    @Test
    @DisplayName("should initialize with story execution")
    void shouldInitializeWithStoryExecution() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", "Story summary");

      // Act
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Assert
      assertNotNull(result);
      assertEquals(execution, result.execution());
      assertNotNull(result.prequelResults());
      assertTrue(result.prequelResults().isEmpty());
      assertNotNull(result.sceneResults());
      assertTrue(result.sceneResults().isEmpty());
    }

    @Test
    @DisplayName("should initialize with empty prequel and scene lists")
    void shouldInitializeWithEmptyPrequelAndSceneLists() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);

      // Act
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Assert
      assertEquals(0, result.prequelResults().size());
      assertEquals(0, result.sceneResults().size());
    }
  }

  @Nested
  @DisplayName(
      "constructor(StoryExecution, List<StoryExecutionResult>, List<SceneExecutionResult>)")
  class ThreeParameterConstructor {

    @Test
    @DisplayName("should initialize with all parameters")
    void shouldInitializeWithAllParameters() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", "Story summary");
      StoryExecutionResult prequel1 = createStoryResult("Prequel 1");
      StoryExecutionResult prequel2 = createStoryResult("Prequel 2");
      List<StoryExecutionResult> prequels = List.of(prequel1, prequel2);

      SceneExecutionResult scene1 = createSceneWithStatus(ExecutionStatus.SUCCESS);
      SceneExecutionResult scene2 = createSceneWithStatus(ExecutionStatus.SUCCESS);
      List<SceneExecutionResult> scenes = List.of(scene1, scene2);

      // Act
      StoryExecutionResult result = new StoryExecutionResult(execution, prequels, scenes);

      // Assert
      assertNotNull(result);
      assertEquals(execution, result.execution());
      assertEquals(2, result.prequelResults().size());
      assertEquals(prequel1, result.prequelResults().get(0));
      assertEquals(prequel2, result.prequelResults().get(1));
      assertEquals(2, result.sceneResults().size());
      assertEquals(scene1, result.sceneResults().get(0));
      assertEquals(scene2, result.sceneResults().get(1));
    }

    @Test
    @DisplayName("should handle null prequel results list")
    void shouldHandleNullPrequelResultsList() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      SceneExecutionResult scene = createSceneWithStatus(ExecutionStatus.SUCCESS);

      // Act
      StoryExecutionResult result = new StoryExecutionResult(execution, null, List.of(scene));

      // Assert
      assertNotNull(result.prequelResults());
      assertTrue(result.prequelResults().isEmpty());
    }

    @Test
    @DisplayName("should handle null scene results list")
    void shouldHandleNullSceneResultsList() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult prequel = createStoryResult("Prequel");

      // Act
      StoryExecutionResult result = new StoryExecutionResult(execution, List.of(prequel), null);

      // Assert
      assertNotNull(result.sceneResults());
      assertTrue(result.sceneResults().isEmpty());
    }

    @Test
    @DisplayName("should handle both null lists")
    void shouldHandleBothNullLists() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);

      // Act
      StoryExecutionResult result = new StoryExecutionResult(execution, null, null);

      // Assert
      assertNotNull(result.prequelResults());
      assertTrue(result.prequelResults().isEmpty());
      assertNotNull(result.sceneResults());
      assertTrue(result.sceneResults().isEmpty());
    }
  }

  @Nested
  @DisplayName("addPrequelResult()")
  class AddPrequelResult {

    @Test
    @DisplayName("should add prequel result to empty list")
    void shouldAddPrequelResultToEmptyList() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      StoryExecutionResult prequel = createStoryResult("Prequel");

      // Act
      result.addPrequelResult(prequel);

      // Assert
      assertEquals(1, result.prequelResults().size());
      assertEquals(prequel, result.prequelResults().get(0));
    }

    @Test
    @DisplayName("should add multiple prequel results")
    void shouldAddMultiplePrequelResults() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      StoryExecutionResult prequel1 = createStoryResult("Prequel 1");
      StoryExecutionResult prequel2 = createStoryResult("Prequel 2");
      StoryExecutionResult prequel3 = createStoryResult("Prequel 3");

      // Act
      result.addPrequelResult(prequel1);
      result.addPrequelResult(prequel2);
      result.addPrequelResult(prequel3);

      // Assert
      assertEquals(3, result.prequelResults().size());
      assertEquals(prequel1, result.prequelResults().get(0));
      assertEquals(prequel2, result.prequelResults().get(1));
      assertEquals(prequel3, result.prequelResults().get(2));
    }

    @Test
    @DisplayName("should maintain order of added prequels")
    void shouldMaintainOrderOfAddedPrequels() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Act
      for (int i = 1; i <= 5; i++) {
        result.addPrequelResult(createStoryResult("Prequel " + i));
      }

      // Assert
      assertEquals(5, result.prequelResults().size());
      for (int i = 0; i < 5; i++) {
        assertEquals("Prequel " + (i + 1), result.prequelResults().get(i).execution().title());
      }
    }
  }

  @Nested
  @DisplayName("addSceneResult()")
  class AddSceneResult {

    @Test
    @DisplayName("should add scene result to empty list")
    void shouldAddSceneResultToEmptyList() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      SceneExecutionResult scene = createSceneWithStatus(ExecutionStatus.SUCCESS);

      // Act
      result.addSceneResult(scene);

      // Assert
      assertEquals(1, result.sceneResults().size());
      assertEquals(scene, result.sceneResults().get(0));
    }

    @Test
    @DisplayName("should add multiple scene results")
    void shouldAddMultipleSceneResults() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      SceneExecutionResult scene1 = createSceneWithStatus(ExecutionStatus.SUCCESS);
      SceneExecutionResult scene2 = createSceneWithStatus(ExecutionStatus.SUCCESS);
      SceneExecutionResult scene3 = createSceneWithStatus(ExecutionStatus.SUCCESS);

      // Act
      result.addSceneResult(scene1);
      result.addSceneResult(scene2);
      result.addSceneResult(scene3);

      // Assert
      assertEquals(3, result.sceneResults().size());
      assertEquals(scene1, result.sceneResults().get(0));
      assertEquals(scene2, result.sceneResults().get(1));
      assertEquals(scene3, result.sceneResults().get(2));
    }

    @Test
    @DisplayName("should maintain order of added scenes")
    void shouldMaintainOrderOfAddedScenes() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Act
      for (int i = 1; i <= 5; i++) {
        result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));
      }

      // Assert
      assertEquals(5, result.sceneResults().size());
    }
  }

  @Nested
  @DisplayName("status()")
  class Status {

    @Test
    @DisplayName("should return SUCCESS when no scenes")
    void shouldReturnSuccessWhenNoScenes() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should return SUCCESS when all scenes are SUCCESS")
    void shouldReturnSuccessWhenAllScenesAreSuccess() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should return FAILURE when one scene is FAILURE")
    void shouldReturnFailureWhenOneSceneIsFailure() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.FAILURE));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.FAILURE, status);
    }

    @Test
    @DisplayName("should return ERROR when one scene is ERROR")
    void shouldReturnErrorWhenOneSceneIsError() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.ERROR));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should prioritize ERROR over FAILURE")
    void shouldPrioritizeErrorOverFailure() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.FAILURE));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.ERROR));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should delegate to ExecutionStatusCalculator")
    void shouldDelegateToExecutionStatusCalculator() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.FAILURE));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.ERROR));
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert - ExecutionStatusCalculator should prioritize ERROR
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should compute status based on scenes not prequels")
    void shouldComputeStatusBasedOnScenesNotPrequels() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", null);
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Add prequel with ERROR status (should be ignored)
      StoryExecutionResult prequelWithError = createStoryResult("Prequel");
      prequelWithError.addSceneResult(createSceneWithStatus(ExecutionStatus.ERROR));
      result.addPrequelResult(prequelWithError);

      // Add scene with SUCCESS status
      result.addSceneResult(createSceneWithStatus(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert - Status should be SUCCESS, ignoring prequel ERROR
      assertEquals(ExecutionStatus.SUCCESS, status);
    }
  }

  @Nested
  @DisplayName("execution() accessor")
  class ExecutionAccessor {

    @Test
    @DisplayName("should return the story execution")
    void shouldReturnTheStoryExecution() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", "Summary");
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Act
      StoryExecution retrievedExecution = result.execution();

      // Assert
      assertNotNull(retrievedExecution);
      assertEquals(execution, retrievedExecution);
      assertEquals("Test Story", retrievedExecution.title());
      assertEquals("Summary", retrievedExecution.summary());
    }

    @Test
    @DisplayName("should return same execution instance")
    void shouldReturnSameExecutionInstance() {
      // Arrange
      StoryExecution execution = createStoryExecution("Test Story", "Summary");
      StoryExecutionResult result = new StoryExecutionResult(execution);

      // Act
      StoryExecution first = result.execution();
      StoryExecution second = result.execution();

      // Assert
      assertSame(first, second);
    }
  }

  private StoryExecution createStoryExecution(String title, String summary) {
    return new StoryExecution(STORY_PATH, title, summary, List.of(), List.of());
  }

  private StoryExecutionResult createStoryResult(String title) {
    StoryExecution execution = createStoryExecution(title, "Summary");
    return new StoryExecutionResult(execution);
  }

  private SceneExecutionResult createSceneWithStatus(ExecutionStatus status) {
    StepExecutionResult step = new StepExecutionResult(null, status, null, null);
    return new SceneExecutionResult(null, List.of(step));
  }
}
