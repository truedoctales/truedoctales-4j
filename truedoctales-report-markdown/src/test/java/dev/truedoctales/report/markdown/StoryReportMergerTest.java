package dev.truedoctales.report.markdown;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoryReportMergerTest {

  private StoryReportMerger merger;

  @BeforeEach
  void setUp() {
    merger = new StoryReportMerger();
  }

  @Test
  void merge_shouldAnnotateStepWithSuccess() {
    String markdown =
        """
        # My Story

        ## Scene: First scene

        > **Greeting** Say Hello

        Some description.
        """;

    StoryExecutionResult result = buildStoryResult(List.of(buildStep(ExecutionStatus.SUCCESS)));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Greeting** Say Hello ✅"));
    assertTrue(merged.contains("Some description."));
  }

  @Test
  void merge_shouldAnnotateStepWithFailure() {
    String markdown =
        """
        # My Story

        ## Scene: First scene

        > **Quest** Status is

        """;

    StoryExecutionResult result =
        buildStoryResult(
            List.of(buildStepWithError(ExecutionStatus.FAILURE, "Expected COMPLETED")));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Quest** Status is ❌"));
    assertTrue(merged.contains("Expected COMPLETED"));
  }

  @Test
  void merge_shouldAnnotateStepWithError() {
    String markdown =
        """
        # My Story

        ## Scene: Test

        > **Hero** Create hero

        """;

    StoryExecutionResult result =
        buildStoryResult(List.of(buildStepWithError(ExecutionStatus.ERROR, "Hero already exists")));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Hero** Create hero ⚠️"));
    assertTrue(merged.contains("Hero already exists"));
  }

  @Test
  void merge_shouldAnnotateStepWithSkipped() {
    String markdown =
        """
        # My Story

        ## Scene: Test

        > **Monster** Monster is dead

        """;

    StoryExecutionResult result = buildStoryResult(List.of(buildStep(ExecutionStatus.SKIPPED)));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Monster** Monster is dead ⏭️"));
  }

  @Test
  void merge_shouldHandleMultipleSteps() {
    String markdown =
        """
        # My Story

        ## Scene: First scene

        > **Hero** Create hero

        > **Quest** Create quest

        > **Fight** Attack fails

        """;

    StoryExecutionResult result =
        buildStoryResult(
            List.of(
                buildStep(ExecutionStatus.SUCCESS),
                buildStep(ExecutionStatus.SUCCESS),
                buildStepWithError(ExecutionStatus.FAILURE, "Attack should have failed")));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Hero** Create hero ✅"));
    assertTrue(merged.contains("> **Quest** Create quest ✅"));
    assertTrue(merged.contains("> **Fight** Attack fails ❌"));
    assertTrue(merged.contains("Attack should have failed"));
  }

  @Test
  void merge_shouldPreserveNonStepLinesUnchanged() {
    String markdown =
        """
        # My Story

        This is a description paragraph.

        ## Scene: Test scene

        Some scene description here.

        > **Greeting** Say Hello

        More text after.
        """;

    StoryExecutionResult result = buildStoryResult(List.of(buildStep(ExecutionStatus.SUCCESS)));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("This is a description paragraph."));
    assertTrue(merged.contains("Some scene description here."));
    assertTrue(merged.contains("More text after."));
  }

  @Test
  void merge_shouldReturnOriginalWhenNoStepResults() {
    String markdown =
        """
        # My Story

        > **Greeting** Say Hello

        """;

    StoryExecutionResult result = new StoryExecutionResult();
    result.setPath("test.md");
    result.setTitle("Test");
    String merged = merger.merge(markdown, result);

    assertEquals(markdown, merged);
  }

  @Test
  void merge_shouldHandleStepWithTableData() {
    String markdown =
        """
        # My Story

        ## Scene: Test

        > **Quest** Create quest
        >
        > | id | name          |
        > |----|---------------|
        > | 1  | Defeat Giants |

        """;

    StoryExecutionResult result = buildStoryResult(List.of(buildStep(ExecutionStatus.SUCCESS)));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Quest** Create quest ✅"));
    assertTrue(merged.contains("| id | name          |"));
  }

  @Test
  void merge_shouldNotAddErrorDetailForSuccessStatus() {
    String markdown =
        """
        > **Greeting** Say Hello
        """;

    StoryExecutionResult result =
        buildStoryResult(List.of(buildStepWithError(ExecutionStatus.SUCCESS, "some message")));
    String merged = merger.merge(markdown, result);

    assertFalse(merged.contains("some message"));
  }

  // Helper methods

  private StoryExecutionResult buildStoryResult(List<StepExecutionResult> steps) {
    StoryExecutionResult result = new StoryExecutionResult();
    result.setPath("test.md");
    result.setTitle("Test Story");
    SceneExecutionResult scene =
        new SceneExecutionResult("Scene Title", 1, steps, ExecutionStatus.SUCCESS);
    result.addSceneResult(scene);
    return result;
  }

  private StepExecutionResult buildStep(ExecutionStatus status) {
    return new StepExecutionResult(1, "Plot", "pattern", Map.of(), List.of(), status, null, null);
  }

  private StepExecutionResult buildStepWithError(ExecutionStatus status, String errorMessage) {
    return new StepExecutionResult(
        1, "Plot", "pattern", Map.of(), List.of(), status, errorMessage, null);
  }
}
