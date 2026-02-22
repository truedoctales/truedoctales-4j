package dev.truedoctales.parser;

import dev.truedoctales.api.model.story.StepCall;
import dev.truedoctales.api.model.story.StepTask;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for individual steps within a scene.
 *
 * <p>Handles two step types:
 *
 * <ul>
 *   <li><strong>StepTask</strong> - Executable steps starting with {@code > **PlotName**
 *       description}
 *   <li><strong>StepDescription</strong> - Markdown content between executable steps
 * </ul>
 *
 * <p><strong>Modern Java 25 Features:</strong>
 *
 * <ul>
 *   <li>Sealed parser state hierarchy
 *   <li>Pattern matching with switch expressions
 *   <li>Records for immutable data
 *   <li>Text blocks for regex patterns
 * </ul>
 */
final class StepParser {

  private static final Pattern TABLE_SEPARATOR = Pattern.compile("^:?-+:?$");
  private static final String BLOCKQUOTE_PREFIX = ">";

  private final int startLineNumber;
  private final TaskState state;

  StepParser(String firstLine, int lineNumber) {
    this.startLineNumber = lineNumber;
    this.state = parseStepTaskFormat(firstLine.trim().substring(1).trim());
  }

  /**
   * Parses the next line for this step.
   *
   * @param line the line to parse
   * @param lineNumber current line number (unused but kept for API compatibility)
   * @return `true` if line consumed, `false` if step parsing complete
   */
  boolean parseLine(String line, int lineNumber) {
    String trimmed = line.trim();

    return switch (state) {
      case TaskState task -> parseTaskLine(task, trimmed);
    };
  }

  /**
   * Builds the final Step from accumulated state.
   *
   * @return the constructed Step, or `null` if empty
   */
  StepTask build() {

    return new StepTask(
        startLineNumber,
        new StepCall(state.plot, state.stepValue),
        List.copyOf(state.rows),
        List.of());
  }

  private TaskState parseStepTaskFormat(String content) {
    if (!content.startsWith("**")) {
      return new TaskState("", content, new ArrayList<>(), null, TableState.NONE);
    }

    int endBold = content.indexOf("**", 2);
    if (endBold <= 2) {
      return new TaskState("", content, new ArrayList<>(), null, TableState.NONE);
    }

    String plot = content.substring(2, endBold).trim();
    String stepValue = content.substring(endBold + 2).trim();
    return new TaskState(plot, stepValue, new ArrayList<>(), null, TableState.NONE);
  }

  private boolean parseTaskLine(TaskState task, String trimmed) {
    // Empty lines reset table state but continue parsing
    if (trimmed.isEmpty()) {
      task.resetTable();
      return true;
    }

    // Non-blockquote line ends task parsing
    if (!trimmed.startsWith(BLOCKQUOTE_PREFIX)) {
      return false;
    }

    String content = trimmed.substring(1).trim();

    // Handle table rows
    if (content.startsWith("|")) {
      task.processTableRow(content);
      return true;
    }

    // Non-table content in task - continue parsing
    return true;
  }

  // ===== State Classes =====

  /** Sealed hierarchy representing parser state. */
  private sealed interface ParserState permits TaskState {}

  /** State for parsing executable step tasks with tables. */
  private static final class TaskState implements ParserState {
    private final String plot;
    private final String stepValue;
    private final List<Map<String, String>> rows;
    private List<String> headers;
    private TableState tableState;

    TaskState(
        String plot,
        String stepValue,
        List<Map<String, String>> rows,
        List<String> headers,
        TableState tableState) {
      this.plot = plot;
      this.stepValue = stepValue;
      this.rows = rows;
      this.headers = headers;
      this.tableState = tableState;
    }

    void resetTable() {
      headers = null;
      tableState = TableState.NONE;
    }

    void processTableRow(String line) {
      List<String> cells = parseTableCells(line);
      if (cells.isEmpty()) return;

      if (isSeparatorRow(cells)) {
        tableState = TableState.SAW_SEPARATOR;
        return;
      }

      if (headers == null) {
        headers = cells;
        tableState = TableState.HAS_HEADERS;
        return;
      }

      if (tableState == TableState.SAW_SEPARATOR || cells.size() == headers.size()) {
        rows.add(createDataRow(headers, cells));
        tableState = TableState.IN_DATA;
        return;
      }

      // Column count changed - new header row
      headers = cells;
      tableState = TableState.HAS_HEADERS;
    }

    private static List<String> parseTableCells(String line) {
      return Arrays.stream(line.split("\\|"))
          .map(String::trim)
          .filter(cell -> !cell.isEmpty())
          .toList();
    }

    private static boolean isSeparatorRow(List<String> cells) {
      return cells.stream().allMatch(cell -> TABLE_SEPARATOR.matcher(cell).matches());
    }

    private static Map<String, String> createDataRow(List<String> headers, List<String> values) {
      Map<String, String> row = new LinkedHashMap<>();
      for (int i = 0; i < Math.min(headers.size(), values.size()); i++) {
        row.put(headers.get(i), values.get(i));
      }
      return row;
    }
  }

  /** Table parsing state machine. */
  private enum TableState {
    NONE,
    HAS_HEADERS,
    SAW_SEPARATOR,
    IN_DATA
  }
}
