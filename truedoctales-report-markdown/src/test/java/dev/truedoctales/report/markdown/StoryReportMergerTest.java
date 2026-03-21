package dev.truedoctales.report.markdown;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.execution.InputType;
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

  @Test
  void merge_shouldExpandVariablesInStepLine() {
    String markdown =
        """
        # My Story

        ## Scene: Test

        > **Greeting** Greet ${name}

        """;

    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Greeting",
            "Greet ${name}",
            InputType.SEQUENCE,
            Map.of("name", "Alice"),
            List.of(),
            ExecutionStatus.SUCCESS,
            null,
            null);
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Greeting** Greet *Alice* ✅"), "Variable should be expanded");
    assertFalse(merged.contains("${name}"), "Placeholder should be replaced");
  }

  @Test
  void merge_shouldExpandMultipleVariablesInStepLine() {
    String markdown =
        """
        > **Greeting** Greet ${name} ${count} times
        """;

    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Greeting",
            "Greet ${name} ${count} times",
            InputType.SEQUENCE,
            Map.of("name", "John", "count", "3"),
            List.of(),
            ExecutionStatus.SUCCESS,
            null,
            null);
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Greeting** Greet *John* *3* times ✅"));
  }

  @Test
  void merge_shouldShowDescriptionAfterStepLine() {
    String markdown =
        """
        > **Hero** Create hero
        """;

    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Hero",
            "Create hero",
            InputType.SEQUENCE,
            Map.of(),
            List.of(),
            ExecutionStatus.SUCCESS,
            null,
            null,
            "Creates a new hero with the given attributes.");
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("> **Hero** Create hero ✅"));
    assertTrue(
        merged.contains("> _Creates a new hero with the given attributes._"),
        "Description should appear as blockquote italic after step line");
  }

  @Test
  void merge_shouldNotShowEmptyDescription() {
    String markdown =
        """
        > **Hero** Create hero
        """;

    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Hero",
            "Create hero",
            InputType.SEQUENCE,
            Map.of(),
            List.of(),
            ExecutionStatus.SUCCESS,
            null,
            null,
            "");
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertFalse(merged.contains("> _"), "Empty description should produce no italic blockquote");
  }

  @Test
  void merge_shouldShowBatchColumnNamesForBatchStep() {
    String markdown =
        """
        > **Hero** Create hero
        """;

    // LinkedHashMap ensures predictable column order in the test assertion
    Map<String, String> row1 = new java.util.LinkedHashMap<>();
    row1.put("id", "1");
    row1.put("name", "Tailor");
    row1.put("species", "Human");
    Map<String, String> row2 = new java.util.LinkedHashMap<>();
    row2.put("id", "2");
    row2.put("name", "Giant");
    row2.put("species", "Giant");
    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Hero",
            "Create hero",
            InputType.BATCH,
            Map.of(),
            List.of(row1, row2),
            ExecutionStatus.SUCCESS,
            null,
            null);
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertTrue(
        merged.contains("[id, name, species]"), "BATCH step should show column names in brackets");
    assertTrue(merged.contains("✅"));
  }

  @Test
  void merge_shouldNotShowBatchColumnsForSequenceStep() {
    String markdown =
        """
        > **Hero** Create hero
        """;

    List<Map<String, String>> stepData = List.of(Map.of("id", "1", "name", "Tailor"));
    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Hero",
            "Create hero",
            InputType.SEQUENCE,
            Map.of(),
            stepData,
            ExecutionStatus.SUCCESS,
            null,
            null);
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertFalse(
        merged.contains("[id, name]"), "SEQUENCE step should not show column bracket annotation");
  }

  @Test
  void merge_shouldItalicizeRemainingPlaceholdersForTableInput() {
    String markdown =
        """
        # My Story

        ## Scene: Test

        > **Greeting** Greet ${name}
        >
        > | name  |
        > |-------|
        > | Alice |

        """;

    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Greeting",
            "Greet ${name}",
            InputType.SEQUENCE,
            Map.of(),
            List.of(Map.of("name", "Alice")),
            ExecutionStatus.SUCCESS,
            null,
            null,
            "",
            List.of(ExecutionStatus.SUCCESS));
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertTrue(
        merged.contains("> **Greeting** Greet *${name}* ✅"),
        "Table-backed placeholders should be wrapped in italic");
  }

  @Test
  void merge_shouldShowPerRowStatusForSequenceTable() {
    String markdown =
        """
        > **Hero** Create hero
        >
        > | id | name   |
        > |----|--------|
        > | 1  | Tailor |
        > | 2  | Giant  |
        """;

    List<Map<String, String>> stepData = new java.util.ArrayList<>();
    Map<String, String> row1 = new java.util.LinkedHashMap<>();
    row1.put("id", "1");
    row1.put("name", "Tailor");
    Map<String, String> row2 = new java.util.LinkedHashMap<>();
    row2.put("id", "2");
    row2.put("name", "Giant");
    stepData.add(row1);
    stepData.add(row2);

    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Hero",
            "Create hero",
            InputType.SEQUENCE,
            Map.of(),
            stepData,
            ExecutionStatus.SUCCESS,
            null,
            null,
            "",
            List.of(ExecutionStatus.SUCCESS, ExecutionStatus.SUCCESS));
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("✅ |"), "Data rows should include per-row status emoji");
  }

  @Test
  void merge_shouldShowMixedPerRowStatusForSequenceTable() {
    String markdown =
        """
        > **Hero** Create hero
        >
        > | id | name   |
        > |----|--------|
        > | 1  | Tailor |
        > | 2  | Giant  |
        """;

    List<Map<String, String>> stepData = new java.util.ArrayList<>();
    Map<String, String> row1 = new java.util.LinkedHashMap<>();
    row1.put("id", "1");
    row1.put("name", "Tailor");
    Map<String, String> row2 = new java.util.LinkedHashMap<>();
    row2.put("id", "2");
    row2.put("name", "Giant");
    stepData.add(row1);
    stepData.add(row2);

    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Hero",
            "Create hero",
            InputType.SEQUENCE,
            Map.of(),
            stepData,
            ExecutionStatus.FAILURE,
            "Giant creation failed",
            null,
            "",
            List.of(ExecutionStatus.SUCCESS, ExecutionStatus.FAILURE));
    StoryExecutionResult result = buildStoryResult(List.of(step));
    String merged = merger.merge(markdown, result);

    assertTrue(merged.contains("✅ |"), "First row should be marked SUCCESS");
    assertTrue(merged.contains("❌ |"), "Second row should be marked FAILURE");
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
    return new StepExecutionResult(
        1, "Plot", "pattern", InputType.SEQUENCE, Map.of(), List.of(), status, null, null);
  }

  private StepExecutionResult buildStepWithError(ExecutionStatus status, String errorMessage) {
    return new StepExecutionResult(
        1, "Plot", "pattern", InputType.SEQUENCE, Map.of(), List.of(), status, errorMessage, null);
  }
}
