package dev.truedoctales.report.markdown;

import dev.truedoctales.api.model.execution.InputType;
import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/// Merges story execution results into the original markdown content.
///
/// For each step declaration line in the markdown, this merger:
/// <ul>
///   <li>Expands {@code ${variable}} placeholders with the extracted variable values</li>
///   <li>Appends a status badge (✅ / ❌ / ⚠️ / ⏭️)</li>
///   <li>For BATCH steps, appends the table column names as a compact annotation</li>
///   <li>Inserts the {@code @Step} description (if any) as a blockquote italic line</li>
///   <li>For failures and errors, inserts an additional detail line with the error message</li>
/// </ul>
public class StoryReportMerger {

  private static final Pattern STEP_DECLARATION =
      Pattern.compile("^>\\s*\\*\\*(.+?)\\*\\*\\s*(.+)$");

  /// Merges execution results into the original markdown content.
  ///
  /// @param originalMarkdown the original markdown content
  /// @param result the story execution result containing step statuses
  /// @return the merged markdown content with execution status annotations
  public String merge(String originalMarkdown, StoryExecutionResult result) {
    Deque<StepExecutionResult> stepQueue = buildStepQueue(result);

    if (stepQueue.isEmpty()) {
      return originalMarkdown;
    }

    String[] lines = originalMarkdown.split("\n", -1);
    StringBuilder merged = new StringBuilder();

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      boolean isLast = i == lines.length - 1;

      if (!stepQueue.isEmpty() && STEP_DECLARATION.matcher(line.trim()).matches()) {
        StepExecutionResult stepResult = stepQueue.poll();

        // Expand ${varName} placeholders with actual values
        String annotatedLine = expandVariables(line, stepResult.variables());

        // For BATCH steps with data, append the column names before the status emoji
        if (stepResult.inputType() == InputType.BATCH
            && stepResult.stepData() != null
            && !stepResult.stepData().isEmpty()) {
          List<String> columns = new ArrayList<>(stepResult.stepData().getFirst().keySet());
          annotatedLine = annotatedLine + " [" + String.join(", ", columns) + "]";
        }

        merged
            .append(annotatedLine)
            .append(" ")
            .append(statusEmoji(stepResult.status()))
            .append("\n");

        // Show description from @Step annotation (if provided) as a blockquote italic line
        if (stepResult.description() != null && !stepResult.description().isBlank()) {
          merged.append("> \n> _").append(stepResult.description()).append("_\n");
        }

        if (stepResult.status() != ExecutionStatus.SUCCESS
            && stepResult.errorMessage() != null
            && !stepResult.errorMessage().isBlank()) {
          merged
              .append("> ")
              .append(statusEmoji(stepResult.status()))
              .append(" **")
              .append(stepResult.status())
              .append(":** ")
              .append(stepResult.errorMessage())
              .append("\n");
        }
      } else {
        merged.append(line);
        if (!isLast) {
          merged.append("\n");
        }
      }
    }

    return merged.toString();
  }

  /// Replaces {@code ${varName}} placeholders in the step line with the extracted variable values.
  private String expandVariables(String line, Map<String, String> variables) {
    if (variables == null || variables.isEmpty()) {
      return line;
    }
    String result = line;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      result = result.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }

  private Deque<StepExecutionResult> buildStepQueue(StoryExecutionResult result) {
    Deque<StepExecutionResult> queue = new ArrayDeque<>();
    if (result.getSceneResults() != null) {
      for (SceneExecutionResult scene : result.getSceneResults()) {
        if (scene.stepResults() != null) {
          queue.addAll(scene.stepResults());
        }
      }
    }
    return queue;
  }

  private String statusEmoji(ExecutionStatus status) {
    return switch (status) {
      case SUCCESS -> "✅";
      case FAILURE -> "❌";
      case ERROR -> "⚠️";
      case SKIPPED -> "⏭️";
    };
  }
}
