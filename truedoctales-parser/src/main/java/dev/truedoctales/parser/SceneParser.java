package dev.truedoctales.parser;

import dev.truedoctales.api.model.story.SceneModel;
import dev.truedoctales.api.model.story.Step;
import dev.truedoctales.api.model.story.StepDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser for a single scene within a markdown story.
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
      finishBetweenStepsDescription();
      return false; // Scene parsing complete
    }

    // If we encounter a new step declaration while currently parsing a step, close the current step
    if (context.currentStepParser != null && isStepDeclarationLine(trimmedLine)) {
      finishCurrentStep();
      finishBetweenStepsDescription();
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
      finishBetweenStepsDescription();
      context.currentStepParser = new StepParser(trimmedLine, context.lineNumber);
      return true;
    }

    // Accumulate description markdown
    if (!trimmedLine.isEmpty()) {
      context.addDescriptionLine(line);
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
    finishBetweenStepsDescription();
    context.prependSceneDescription(startLineNumber);

    return new SceneModel(
        title, startLineNumber, context.getSceneDescription(), List.copyOf(context.steps));
  }

  int getLineNumber() {
    return context.lineNumber;
  }

  // ===== Private Implementation =====

  private void finishCurrentStep() {
    if (context.currentStepParser != null) {
      Step step = context.currentStepParser.build();
      if (step != null) {
        context.steps.add(step);
      }
      context.currentStepParser = null;
    }
  }

  private void finishBetweenStepsDescription() {
    String markdown = context.getAndClearIntermediateDescription();
    if (!markdown.isEmpty()) {
      context.steps.add(new StepDescription(markdown, context.lineNumber));
    }
  }

  /** Mutable parsing context holding all scene state. */
  private static final class ParseContext {
    private final StringBuilder sceneDescription = new StringBuilder();
    private final StringBuilder intermediateDescription = new StringBuilder();
    private final List<Step> steps = new ArrayList<>();

    private int lineNumber;
    private StepParser currentStepParser;
    private ParsingPhase phase = ParsingPhase.SCENE_DESCRIPTION;

    ParseContext(int startLine) {
      this.lineNumber = startLine;
    }

    void incrementLine() {
      lineNumber++;
    }

    void transitionToStepParsing() {
      phase = ParsingPhase.STEPS;
    }

    void addDescriptionLine(String line) {
      switch (phase) {
        case SCENE_DESCRIPTION -> {
          if (!sceneDescription.isEmpty()) {
            sceneDescription.append("\n");
          }
          sceneDescription.append(line);
        }
        case STEPS -> {
          if (!intermediateDescription.isEmpty()) {
            intermediateDescription.append("\n");
          }
          intermediateDescription.append(line);
        }
      }
    }

    String getSceneDescription() {
      String desc = sceneDescription.toString().trim();
      return desc.isEmpty() ? null : desc;
    }

    String getAndClearIntermediateDescription() {
      String markdown = intermediateDescription.toString().trim();
      intermediateDescription.setLength(0);
      return markdown;
    }

    void prependSceneDescription(int lineNumber) {
      String desc = sceneDescription.toString().trim();
      if (!desc.isEmpty()) {
        steps.addFirst(new StepDescription(desc, lineNumber));
      }
    }

    /** Parsing phase state. */
    private enum ParsingPhase {
      SCENE_DESCRIPTION,
      STEPS
    }
  }

  private boolean isStepDeclarationLine(String trimmedLine) {
    return STEP_DECLARATION_PATTERN.matcher(trimmedLine).matches();
  }
}
