package dev.truedoctales.parser;

import dev.truedoctales.api.model.story.SceneModel;
import dev.truedoctales.api.model.story.StepTask;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser for a single scene within a Markdown story.
 *
 * <p>A scene:
 *
 * <ul>
 *   <li>Starts with {@code ## Scene: Title}
 *   <li>Contains optional description before first step
 *   <li>Contains steps (executable and markdown descriptions)
 *   <li>Ends at next scene marker or EOF
 * </ul>
 *
 * <p><strong>Modern Java 25 Features:</strong>
 *
 * <ul>
 *   <li>Sealed parsing states
 *   <li>Pattern matching for state transitions
 *   <li>Enhanced encapsulation with private state classes
 * </ul>
 */
final class SceneParser {

  private static final String SCENE_PREFIX = "##";
  private static final String STEP_PREFIX = ">";
  private static final Pattern STEP_DECLARATION_PATTERN = Pattern.compile("^>\\s*\\*\\*.+");

  private final String title;
  private final int startLineNumber;
  private final ParseContext context;

  SceneParser(String sceneTitle, int lineNumber) {
    this.title = sceneTitle;
    this.startLineNumber = lineNumber;
    this.context = new ParseContext(lineNumber);
  }

  /**
   * Parses a single line within the scene.
   *
   * @param line the line to parse
   * @return {@code true} if line consumed, {@code false} if new scene starts (parsing complete)
   */
  boolean parseLine(String line) {
    context.incrementLine();
    String trimmedLine = line.trim();

    // Check for next scene marker
    if (trimmedLine.startsWith(SCENE_PREFIX)) {
      finishCurrentStep();
      return false; // Scene parsing complete
    }

    // If we encounter a new step declaration while currently parsing a step, close the current step
    if (context.currentStepParser != null && isStepDeclarationLine(trimmedLine)) {
      finishCurrentStep();
      context.transitionToStepParsing();
      context.currentStepParser = new StepParser(trimmedLine, context.lineNumber);
      return true;
    }

    // Delegate to current step parser if active
    if (context.currentStepParser != null) {
      if (!context.currentStepParser.parseLine(line, context.lineNumber)) {
        // Step parser finished
        finishCurrentStep();
        // Continue processing this line
      } else {
        return true;
      }
    }

    // Check if line starts new step
    if (trimmedLine.startsWith(STEP_PREFIX)) {
      context.transitionToStepParsing();
      context.currentStepParser = new StepParser(trimmedLine, context.lineNumber);
      return true;
    }

    return true;
  }

  /**
   * Builds the final SceneModel from accumulated state.
   *
   * @return the constructed SceneModel
   */
  SceneModel build() {
    finishCurrentStep();

    return new SceneModel(title, startLineNumber, List.copyOf(context.steps));
  }

  int getLineNumber() {
    return context.lineNumber;
  }

  // ===== Private Implementation =====

  private void finishCurrentStep() {
    if (context.currentStepParser != null) {
      StepTask step = context.currentStepParser.build();
      context.steps.add(step);
      context.currentStepParser = null;
    }
  }

  /** Mutable parsing context holding all scene state. */
  private static final class ParseContext {
    private final List<StepTask> steps = new ArrayList<>();

    private int lineNumber;
    private StepParser currentStepParser;

    ParseContext(int startLine) {
      this.lineNumber = startLine;
    }

    void incrementLine() {
      lineNumber++;
    }

    void transitionToStepParsing() {}
  }

  private boolean isStepDeclarationLine(String trimmedLine) {
    return STEP_DECLARATION_PATTERN.matcher(trimmedLine).matches();
  }
}
