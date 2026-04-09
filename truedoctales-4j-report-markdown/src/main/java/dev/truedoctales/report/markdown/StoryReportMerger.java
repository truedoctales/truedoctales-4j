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
import java.util.regex.Pattern;

/// Merges story execution results into the original markdown content.
///
/// For each step declaration line in the markdown, this merger:
/// <ul>
///   <li>Appends a status badge (✅ / ❌ / ⚠️ / ⏭️)</li>
///   <li>For BATCH steps, appends the table column names as a compact annotation</li>
///   <li>Inserts the {@code @Step} description (if any) as a blockquote italic line</li>
///   <li>For failures and errors, inserts an additional detail line with the error message</li>
///   <li>For SEQUENCE steps with table data, adds per-row status indicators to table rows</li>
/// </ul>
///
/// <p>Variable values are expected to already be in italic format ({@code *value*} or
/// {@code *{name}*}) in the source markdown, so no variable expansion is performed by the
/// merger.
public class StoryReportMerger {

  private static final Pattern STEP_DECLARATION =
      Pattern.compile("^>\\s*\\*\\*(.+?)\\*\\*\\s*(.+)$");

  private static final Pattern TABLE_ROW = Pattern.compile("^>\\s*\\|.*\\|\\s*$");

  private static final Pattern TABLE_SEPARATOR = Pattern.compile("^>\\s*\\|[\\s:|-]+\\|\\s*$");

  private static final String CODE_FENCE = "```";
  private static final String BLOCKQUOTE_PREFIX = ">";

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

    // State for tracking SEQUENCE table rows
    List<ExecutionStatus> pendingRowStatuses = null;
    int tableRowIndex = 0;
    boolean seenTableHeader = false;
    boolean seenTableSeparator = false;
    boolean inCodeBlock = false;

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      boolean isLast = i == lines.length - 1;

      // Toggle code-block state on fenced code-fence lines so that step-like
      // lines inside a code block are never treated as executable steps.
      if (isFencedCodeBlockLine(line.trim())) {
        inCodeBlock = !inCodeBlock;
        merged.append(line);
        if (!isLast) {
          merged.append("\n");
        }
        continue;
      }

      if (!inCodeBlock && !stepQueue.isEmpty() && STEP_DECLARATION.matcher(line.trim()).matches()) {
        StepExecutionResult stepResult = stepQueue.poll();

        // The step line already has variable values in italic (*value* or *{name}*),
        // so no variable expansion is needed.
        String annotatedLine = line;

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

        // Set up per-row status tracking for SEQUENCE steps with table data
        if (stepResult.inputType() == InputType.SEQUENCE
            && stepResult.rowStatuses() != null
            && !stepResult.rowStatuses().isEmpty()) {
          pendingRowStatuses = stepResult.rowStatuses();
          tableRowIndex = 0;
          seenTableHeader = false;
          seenTableSeparator = false;
        } else {
          pendingRowStatuses = null;
        }
      } else if (pendingRowStatuses != null && TABLE_ROW.matcher(line).matches()) {
        // Inside a SEQUENCE step's table — add status column
        if (!seenTableHeader) {
          // Table header row — add empty status column header
          merged.append(line.replaceFirst("\\|\\s*$", "| |")).append("\n");
          seenTableHeader = true;
        } else if (!seenTableSeparator && TABLE_SEPARATOR.matcher(line).matches()) {
          // Table separator row — add separator column
          merged.append(line.replaceFirst("\\|\\s*$", "|---|")).append("\n");
          seenTableSeparator = true;
        } else if (seenTableSeparator && tableRowIndex < pendingRowStatuses.size()) {
          // Data row — add status emoji
          ExecutionStatus rowStatus = pendingRowStatuses.get(tableRowIndex);
          merged
              .append(line.replaceFirst("\\|\\s*$", "| " + statusEmoji(rowStatus) + " |"))
              .append("\n");
          tableRowIndex++;
          if (tableRowIndex >= pendingRowStatuses.size()) {
            pendingRowStatuses = null;
          }
        } else {
          merged.append(line);
          if (!isLast) {
            merged.append("\n");
          }
        }
      } else {
        // Non-table line after a step with pending row statuses — reset state
        if (pendingRowStatuses != null
            && seenTableSeparator
            && !line.trim().isEmpty()
            && !line.trim().equals(">")) {
          pendingRowStatuses = null;
        }
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

  /**
   * Returns {@code true} if {@code trimmedLine} is a fenced code-block delimiter — either a
   * standard triple-backtick line ({@code ```lang}) or a blockquote-wrapped one ({@code >
   * ```lang}).
   */
  private static boolean isFencedCodeBlockLine(String trimmedLine) {
    if (trimmedLine.startsWith(CODE_FENCE)) {
      return true;
    }
    if (trimmedLine.startsWith(BLOCKQUOTE_PREFIX)) {
      String content = trimmedLine.substring(BLOCKQUOTE_PREFIX.length()).trim();
      return content.startsWith(CODE_FENCE);
    }
    return false;
  }
}
