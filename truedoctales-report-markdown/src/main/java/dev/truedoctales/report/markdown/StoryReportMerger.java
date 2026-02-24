package dev.truedoctales.report.markdown;

import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

/// Merges story execution results into the original markdown content.
///
/// For each step declaration line in the markdown, this merger appends a status badge
/// showing whether the step succeeded, failed, or encountered an error. For failures
/// and errors, an additional detail line is inserted with the error message.
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
        merged.append(line).append(" ").append(statusEmoji(stepResult.status())).append("\n");

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
