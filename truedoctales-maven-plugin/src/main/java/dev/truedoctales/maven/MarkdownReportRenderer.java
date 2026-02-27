package dev.truedoctales.maven;

import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.maven.ReportMojo.BookResult;
import dev.truedoctales.maven.ReportMojo.ChapterResult;
import java.util.List;

/// Renders a {@link BookResult} as a Markdown document.
class MarkdownReportRenderer {

  /// Renders the full report as a Markdown string.
  ///
  /// @param book the aggregated book execution result
  /// @return Markdown-formatted report
  String render(BookResult book) {
    var sb = new StringBuilder();

    sb.append("# ").append(book.title()).append('\n');
    sb.append('\n');

    renderSummaryTable(sb, book);

    for (ChapterResult chapter : book.chapters()) {
      renderChapter(sb, chapter);
    }

    return sb.toString();
  }

  // ---- private helpers ----

  private void renderSummaryTable(StringBuilder sb, BookResult book) {
    int total = 0;
    int passed = 0;
    int failed = 0;
    int errors = 0;
    int skipped = 0;

    for (ChapterResult chapter : book.chapters()) {
      for (StoryExecutionResult story : chapter.stories()) {
        total++;
        switch (story.status()) {
          case SUCCESS -> passed++;
          case FAILURE -> failed++;
          case ERROR -> errors++;
          case SKIPPED -> skipped++;
        }
      }
    }

    sb.append("## Summary\n\n");
    sb.append("| Metric | Count |\n");
    sb.append("|--------|------:|\n");
    sb.append("| Total stories | ").append(total).append(" |\n");
    sb.append("| ")
        .append(statusIcon(ExecutionStatus.SUCCESS))
        .append(" Passed | ")
        .append(passed)
        .append(" |\n");
    sb.append("| ")
        .append(statusIcon(ExecutionStatus.FAILURE))
        .append(" Failed | ")
        .append(failed)
        .append(" |\n");
    sb.append("| ")
        .append(statusIcon(ExecutionStatus.ERROR))
        .append(" Errors | ")
        .append(errors)
        .append(" |\n");
    sb.append("| ")
        .append(statusIcon(ExecutionStatus.SKIPPED))
        .append(" Skipped | ")
        .append(skipped)
        .append(" |\n");
    sb.append('\n');
  }

  private void renderChapter(StringBuilder sb, ChapterResult chapter) {
    sb.append("## ").append(chapter.title()).append('\n');
    sb.append('\n');

    for (StoryExecutionResult story : chapter.stories()) {
      renderStory(sb, story);
    }
  }

  private void renderStory(StringBuilder sb, StoryExecutionResult story) {
    sb.append("### ")
        .append(statusIcon(story.status()))
        .append(' ')
        .append(story.getTitle())
        .append('\n');
    sb.append('\n');

    List<SceneExecutionResult> scenes = story.getSceneResults();
    if (scenes != null) {
      for (SceneExecutionResult scene : scenes) {
        renderScene(sb, scene);
      }
    }

    List<StoryExecutionResult> prequels = story.getPrequelResults();
    if (prequels != null && !prequels.isEmpty()) {
      sb.append("**Prequels:**\n\n");
      for (StoryExecutionResult prequel : prequels) {
        renderStory(sb, prequel);
      }
    }
  }

  private void renderScene(StringBuilder sb, SceneExecutionResult scene) {
    sb.append("#### ")
        .append(statusIcon(scene.status()))
        .append(' ')
        .append(scene.title())
        .append('\n');
    sb.append('\n');

    if (scene.stepResults() != null) {
      sb.append("| Step | Status |\n");
      sb.append("|------|--------|\n");
      for (StepExecutionResult step : scene.stepResults()) {
        sb.append("| ").append(step.pattern()).append(" | ").append(statusIcon(step.status()));
        if (step.errorMessage() != null) {
          sb.append(" `").append(step.errorMessage()).append('`');
        }
        sb.append(" |\n");
      }
      sb.append('\n');
    }
  }

  private static String statusIcon(ExecutionStatus status) {
    return switch (status) {
      case SUCCESS -> "\u2705";
      case FAILURE -> "\u274C";
      case ERROR -> "\u26A0\uFE0F";
      case SKIPPED -> "\u23ED\uFE0F";
    };
  }
}
