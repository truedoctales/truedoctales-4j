package dev.truedoctales.parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parser for markdown story header section.
 *
 * <p>Header section includes:
 *
 * <ul>
 *   <li>Story title ({@code # Title})
 *   <li>Summary/intro (content before {@code ## Intro:} or first scene)
 *   <li>Prequel references ({@code @Prequel})
 * </ul>
 *
 * <p>Header parsing ends at first {@code ## Scene:} marker.
 *
 * <p><strong>Modern Java 25 Features:</strong>
 *
 * <ul>
 *   <li>Optional for null-safe title handling
 *   <li>Pattern matching for link extraction
 *   <li>Immutable lists with List.copyOf()
 * </ul>
 */
final class HeaderParser {

  private static final String TITLE_PREFIX = "# ";
  private static final String PREQUEL_PREFIX = "@Prequel";
  private static final String PREQUELS_BLOCK_MARKER = "Prequels";
  private static final String BLOCKQUOTE_PREFIX = ">";
  private static final String SCENE_PREFIX = "##";
  private static final String STORY_MARKER = "## Story";
  private static final String CODE_FENCE_PREFIX = "```";

  private static final Pattern LINK_PATTERN = Pattern.compile("]\\(([^)]+)\\)");

  private final ParseState state = new ParseState();

  /**
   * Parses a single header line.
   *
   * @param line the line to parse
   * @return {@code true} if line consumed, {@code false} if scene starts (header complete)
   */
  boolean parseLine(String line) {
    state.lineNumber++;
    String trimmedLine = line.trim();

    // Toggle code-block state: a ## heading inside a fenced code block must
    // be treated as literal content, not as a scene-start marker.
    if (trimmedLine.startsWith(CODE_FENCE_PREFIX)) {
      state.inCodeBlock = !state.inCodeBlock;
      return true;
    }

    // While inside a code block skip everything (no title, scene, or prequel parsing).
    if (state.inCodeBlock) {
      return true;
    }

    // Parse title (only first H1)
    if (trimmedLine.startsWith(TITLE_PREFIX) && state.getTitle() == null) {
      state.setTitle(trimmedLine.substring(TITLE_PREFIX.length()).trim());
      state.parsingPhase = ParsingPhase.SUMMARY;
      return true;
    }

    // Explicit story marker signals end of summary
    if (trimmedLine.equalsIgnoreCase(STORY_MARKER)) {
      state.parsingPhase = ParsingPhase.POST_SUMMARY;
      return true;
    }

    // Scene marker ends header parsing
    if (isSceneHeader(trimmedLine)) {
      return false;
    }

    // Handle blockquote-based Prequels block
    if (trimmedLine.startsWith(BLOCKQUOTE_PREFIX)) {
      String blockquoteContent = trimmedLine.substring(BLOCKQUOTE_PREFIX.length()).trim();

      // Check if starting a Prequels block
      if (blockquoteContent.equals(PREQUELS_BLOCK_MARKER)) {
        state.inPrequelsBlock = true;
        return true;
      }

      // If inside Prequels block, parse prequel links
      if (state.inPrequelsBlock) {
        extractPrequelPathFromBlockquote(blockquoteContent).ifPresent(state.prequelPaths::add);
        return true;
      }

      // Otherwise, it's just normal blockquote content (not a Prequels block)
      return true;
    } else {
      // Non-blockquote line ends Prequels block
      state.inPrequelsBlock = false;
    }

    // Parse prequel references (can appear anywhere in header)
    if (trimmedLine.startsWith(PREQUEL_PREFIX)) {
      extractPrequelPath(trimmedLine).ifPresent(state.prequelPaths::add);
      return true;
    }

    // Accumulate summary content
    if (state.parsingPhase == ParsingPhase.SUMMARY && !trimmedLine.isEmpty()) {
      state.appendSummaryLine(line);
    }

    return true;
  }

  Optional<String> getTitle() {
    return Optional.ofNullable(state.title);
  }

  Optional<String> getSummary() {
    String summary = state.summaryBuilder.toString().trim();
    return summary.isEmpty() ? Optional.empty() : Optional.of(summary);
  }

  List<Path> getPrequelPaths() {
    return List.copyOf(state.prequelPaths);
  }

  int getLineNumber() {
    return state.lineNumber;
  }

  // ===== Private Implementation =====

  /**
   * Extracts prequel path from annotation line.
   *
   * <p>Supports two formats:
   *
   * <ul>
   *   <li>{@code @Prequel [label](path)} - Markdown link format
   *   <li>{@code @Prequel path} - Simple format
   * </ul>
   */
  private Optional<Path> extractPrequelPath(String line) {
    String remaining = line.substring(PREQUEL_PREFIX.length()).trim();

    // Try markdown link format first
    var matcher = LINK_PATTERN.matcher(remaining);
    if (matcher.find()) {
      String path = matcher.group(1).trim();
      return path.isEmpty() ? Optional.empty() : Optional.of(Path.of(path));
    }

    // Fallback to simple format
    return remaining.isEmpty() ? Optional.empty() : Optional.of(Path.of(remaining));
  }

  /**
   * Extracts prequel path from blockquote content.
   *
   * <p>Supports formats:
   *
   * <ul>
   *   <li>Markdown link with bullet: - [label](path)
   *   <li>Markdown link without bullet: [label](path)
   * </ul>
   */
  private Optional<Path> extractPrequelPathFromBlockquote(String content) {
    // Remove optional bullet/list marker
    String cleaned = content.startsWith("-") ? content.substring(1).trim() : content;

    // Try to extract markdown link
    var matcher = LINK_PATTERN.matcher(cleaned);
    if (matcher.find()) {
      String path = matcher.group(1).trim();
      return path.isEmpty() ? Optional.empty() : Optional.of(Path.of(path));
    }

    return Optional.empty();
  }

  /** Mutable parsing state. */
  private static final class ParseState {
    private String title;
    private final StringBuilder summaryBuilder = new StringBuilder();
    private final List<Path> prequelPaths = new ArrayList<>();
    private int lineNumber = 0;
    private ParsingPhase parsingPhase = ParsingPhase.PRE_TITLE;
    private boolean inPrequelsBlock = false;
    private boolean inCodeBlock = false;

    void appendSummaryLine(String line) {
      if (!summaryBuilder.isEmpty()) {
        summaryBuilder.append("\n");
      }
      summaryBuilder.append(line);
    }

    void setTitle(String title) {
      this.title = title;
    }

    String getTitle() {
      return title;
    }
  }

  /** Header parsing phase. */
  private enum ParsingPhase {
    PRE_TITLE,
    SUMMARY,
    POST_SUMMARY
  }

  private boolean isSceneHeader(String trimmedLine) {
    return trimmedLine.startsWith(SCENE_PREFIX) && !trimmedLine.equalsIgnoreCase(STORY_MARKER);
  }
}
