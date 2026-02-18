package dev.truedoctales.report.markdown;

import dev.truedoctales.api.model.listener.*;
import dev.truedoctales.api.model.story.Step;
import dev.truedoctales.api.model.story.StepDescription;
import dev.truedoctales.api.model.story.StepTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/// Generates Markdown content for the Book of Truth report.
/// This class creates a folder structure with separate files for each chapter,
/// suitable for GitHub Wiki or other documentation systems.
final class MarkdownContentGenerator {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final Path outputPath;

  /// Creates a new MarkdownContentGenerator.
  ///
  /// @param outputPath the directory path where the Markdown report will be written
  MarkdownContentGenerator(Path outputPath) {
    this.outputPath = outputPath;
  }

  /// Generates the Markdown report and writes it to the output path.
  ///
  /// @param book the story book execution result to generate a report for
  /// @throws IOException if writing the report fails
  public void generateReport(StoryBookExecutionResult book) throws IOException {
    Files.createDirectories(outputPath);

    // Generate index file
    generateIndexFile(book);

    // Generate chapter files
    for (ChapterExecutionResult chapter : book.chapterResults()) {
      // Skip prequel chapters
      if (!chapter.chapter().title().contains("prequel")) {
        generateChapterFile(chapter, book);
      }
    }
  }

  private void generateIndexFile(StoryBookExecutionResult book) throws IOException {
    StringBuilder md = new StringBuilder();

    // Title and header
    md.append("# ").append(book.book().title()).append("\n\n");

    // Timestamp
    String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
    md.append("*Generated: ").append(timestamp).append("*\n\n");

    // Book summary/intro
    if (book.book().summary() != null && !book.book().summary().isBlank()) {
      md.append(book.book().summary()).append("\n\n");
    }

    // Execution summary
    md.append(generateExecutionSummary(book)).append("\n\n");

    // Table of contents
    md.append("## Table of Contents\n\n");
    for (ChapterExecutionResult chapter : book.chapterResults()) {
      if (!chapter.chapter().title().contains("prequel")) {
        String chapterFileName = sanitizeFileName(chapter.chapter().title());
        md.append("- [")
            .append(chapter.chapter().title())
            .append("](")
            .append(chapterFileName)
            .append(".md) ")
            .append(getStatusEmoji(chapter.status()))
            .append("\n");
      }
    }

    Files.writeString(outputPath.resolve("Home.md"), md.toString());
  }

  private String generateExecutionSummary(StoryBookExecutionResult book) {
    ExecutionSummaryCalculator calculator = new ExecutionSummaryCalculator(book.chapterResults());

    return """
        ## 📊 Execution Summary

        | Metric | Count |
        |--------|-------|
        | Chapters | %d |
        | Stories | %d |
        | Scenes | %d |
        | Steps | %d |
        | ✅ Success | %d |
        | ❌ Failed | %d |
        """
        .formatted(
            calculator.totalChapters(),
            calculator.totalStories(),
            calculator.totalScenes(),
            calculator.totalSteps(),
            calculator.successfulSteps(),
            calculator.failedSteps());
  }

  private void generateChapterFile(ChapterExecutionResult chapter, StoryBookExecutionResult book)
      throws IOException {
    StringBuilder md = new StringBuilder();

    // Chapter header
    md.append("# ").append(chapter.chapter().title()).append(" ");
    md.append(getStatusEmoji(chapter.status())).append("\n\n");

    // Navigation
    md.append("[← Back to Home](Home.md)\n\n");

    // Chapter intro
    if (chapter.chapter().summary() != null && !chapter.chapter().summary().isBlank()) {
      md.append(chapter.chapter().summary()).append("\n\n");
    }

    // Stories
    for (StoryExecutionResult story : chapter.storyResults()) {
      String storyPath = story.execution().path().toString();
      if (!storyPath.contains("prequel")) {
        md.append(generateStorySection(story)).append("\n\n");
      }
    }

    String chapterFileName = sanitizeFileName(chapter.chapter().title());
    Files.writeString(outputPath.resolve(chapterFileName + ".md"), md.toString());
  }

  private String generateStorySection(StoryExecutionResult story) {
    StringBuilder md = new StringBuilder();

    // Story header
    md.append("## ").append(story.execution().title()).append(" ");
    md.append(getStatusEmoji(story.status())).append("\n\n");

    // Story intro
    if (story.execution().summary() != null && !story.execution().summary().isBlank()) {
      md.append(story.execution().summary()).append("\n\n");
    }

    // Prequels section
    if (!story.prequelResults().isEmpty()) {
      md.append(generatePrequelsSection(story.prequelResults())).append("\n\n");
    }

    // Scenes
    for (SceneExecutionResult scene : story.sceneResults()) {
      md.append(generateSceneSection(scene)).append("\n\n");
    }

    return md.toString();
  }

  private String generatePrequelsSection(List<StoryExecutionResult> prequelResults) {
    StringBuilder md = new StringBuilder();
    md.append("### Prequels\n\n");

    for (StoryExecutionResult prequel : prequelResults) {
      md.append("**").append(prequel.execution().title()).append("** ");
      md.append(getStatusEmoji(prequel.status())).append("\n\n");

      for (SceneExecutionResult prequelScene : prequel.sceneResults()) {
        md.append(generateSceneSection(prequelScene)).append("\n\n");
      }
    }

    return md.toString();
  }

  private String generateSceneSection(SceneExecutionResult scene) {
    StringBuilder md = new StringBuilder();

    // Scene header
    md.append("### Scene: ").append(scene.scene().title()).append(" ");
    md.append(getStatusEmoji(scene.status())).append("\n\n");

    // Scene description
    if (scene.scene().description() != null && !scene.scene().description().isBlank()) {
      md.append(scene.scene().description()).append("\n\n");
    }

    // Render steps in order
    int stepExecutionIndex = 0;
    for (Step step : scene.scene().originalSteps()) {
      if (step instanceof StepDescription desc) {
        md.append(desc.markdown()).append("\n\n");
      } else if (step instanceof StepTask) {
        if (stepExecutionIndex < scene.stepResults().size()) {
          md.append(generateStepSection(scene.stepResults().get(stepExecutionIndex)))
              .append("\n\n");
          stepExecutionIndex++;
        }
      }
    }

    return md.toString();
  }

  private String generateStepSection(StepExecutionResult step) {
    StringBuilder md = new StringBuilder();

    // Step header
    md.append("**Step:** ").append(step.execution().call().stepValue()).append(" ");
    md.append(getStatusEmoji(step.status())).append("\n\n");

    // Step data table
    if (!step.execution().stepData().isEmpty()) {
      md.append(generateDataTable(step.execution().stepData())).append("\n\n");
    }

    // Error detales
    if (step.errorMessage() != null || step.throwable() != null) {
      md.append(generateErrorDetales(step)).append("\n\n");
    }

    return md.toString();
  }

  private String generateDataTable(List<Map<String, String>> data) {
    if (data.isEmpty()) return "";

    StringBuilder md = new StringBuilder();

    // Header row
    md.append("| ");
    for (String key : data.getFirst().keySet()) {
      md.append(key).append(" | ");
    }
    md.append("\n");

    // Separator row
    md.append("| ");
    for (int i = 0; i < data.getFirst().keySet().size(); i++) {
      md.append("--- | ");
    }
    md.append("\n");

    // Data rows
    for (Map<String, String> row : data) {
      md.append("| ");
      for (String value : row.values()) {
        md.append(escapeMarkdown(value)).append(" | ");
      }
      md.append("\n");
    }

    return md.toString();
  }

  private String generateErrorDetales(StepExecutionResult step) {
    StringBuilder md = new StringBuilder();
    md.append("```\n");
    if (step.errorMessage() != null) {
      md.append("Error: ").append(step.errorMessage()).append("\n");
    }
    if (step.throwable() != null) {
      md.append("Exception: ").append(step.throwable().toString()).append("\n");
    }
    md.append("```\n");
    return md.toString();
  }

  private String getStatusEmoji(ExecutionStatus status) {
    return switch (status) {
      case SUCCESS -> "✅";
      case FAILURE, ERROR -> "❌";
      case SKIPPED -> "⏭️";
    };
  }

  private String sanitizeFileName(String title) {
    return title.replaceAll("[^a-zA-Z0-9-_]", "-").replaceAll("-+", "-");
  }

  private String escapeMarkdown(String text) {
    if (text == null) return "";
    return text.replace("|", "\\|");
  }
}
