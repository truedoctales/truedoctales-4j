package dev.truedoctales.report.html;

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

/// Generates HTML content for the Book of Truth report.
/// This class is responsible for transforming execution results into HTML markup.
final class HtmlContentGenerator {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final Path outputPath;

  /// Creates a new HtmlContentGenerator.
  ///
  /// @param outputPath the directory path where the HTML report will be written
  HtmlContentGenerator(Path outputPath) {
    this.outputPath = outputPath;
  }

  /// Generates the HTML report and writes it to the output path.
  ///
  /// @param book the story book execution result to generate a report for
  /// @throws IOException if writing the report fails
  public void generateReport(StoryBookExecutionResult book) throws IOException {
    Files.createDirectories(outputPath);

    // Write main HTML file with inlined CSS and JavaScript
    Path reportFile = outputPath.resolve("index.html");
    String html = buildHtmlReport(book);
    Files.writeString(reportFile, html);
  }

  private String buildHtmlReport(StoryBookExecutionResult book) {
    StringBuilder html = new StringBuilder();

    // Build navigation from chapter and story structure
    List<HtmlTemplate.NavigationItem> navItems = buildNavigation(book);

    // Generate HTML structure
    String bookTitle = book.book().title();
    String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);

    html.append(HtmlTemplate.header(bookTitle));
    html.append(HtmlTemplate.navigation(navItems));
    html.append(HtmlTemplate.contentStart(timestamp));

    // Add book-level intro if present
    html.append(generateBookIntro(book.book().title(), book.book().summary()));

    // Add execution summary
    html.append(generateSummary(book));

    // Add chapters
    for (ChapterExecutionResult chapter : book.chapterResults()) {
      html.append(generateChapterSection(chapter));
    }

    html.append(HtmlTemplate.contentEnd());
    html.append(HtmlTemplate.footer());
    return html.toString();
  }

  private List<HtmlTemplate.NavigationItem> buildNavigation(StoryBookExecutionResult book) {
    List<HtmlTemplate.NavigationItem> navItems = new java.util.ArrayList<>();

    for (ChapterExecutionResult chapter : book.chapterResults()) {
      // Skip prequel chapters in navigation
      if (chapter.chapter().title().contains("prequel")) {
        continue;
      }

      List<String> storyTitles = new java.util.ArrayList<>();
      for (StoryExecutionResult story : chapter.storyResults()) {
        // Skip null stories (defensive)
        if (story == null || story.execution() == null) {
          continue;
        }
        String storyPath = story.execution().path().toString();
        if (!storyPath.contains("prequel")) {
          storyTitles.add(story.execution().title());
        }
      }

      // Only add chapter to navigation if it has stories
      if (!storyTitles.isEmpty()) {
        navItems.add(new HtmlTemplate.NavigationItem(chapter.chapter().title(), storyTitles));
      }
    }

    return navItems;
  }

  private String generateBookIntro(String title, String summary) {
    StringBuilder html = new StringBuilder();
    html.append("                <section class=\"book-intro\">\n");

    if (title != null) {
      html.append("                    <h2>").append(HtmlEscaper.escape(title)).append("</h2>\n");
    }

    if (summary != null && !summary.isBlank()) {
      html.append("                    <div class=\"intro-content\">\n");
      html.append("                        ").append(MarkdownRenderer.toHtml(summary)).append("\n");
      html.append("                    </div>\n");
    }

    html.append("                </section>\n");
    return html.toString();
  }

  private String generateSummary(StoryBookExecutionResult book) {
    ExecutionSummaryCalculator calculator = new ExecutionSummaryCalculator(book.chapterResults());
    return generateSummary(calculator);
  }

  String generateSummary(ExecutionSummaryCalculator calculator) {
    return """
                <section class="summary">
                    <h2>📊 Execution Summary</h2>
                    <div class="summary-grid">
                        <div class="summary-item">
                            <strong>Chapters</strong>
                            <span>"""
        + calculator.totalChapters()
        + """
                            </span>
                        </div>
                        <div class="summary-item">
                            <strong>Stories</strong>
                            <span>"""
        + calculator.totalStories()
        + """
                            </span>
                        </div>
                        <div class="summary-item">
                            <strong>Scenes</strong>
                            <span>"""
        + calculator.totalScenes()
        + """
                            </span>
                        </div>
                        <div class="summary-item">
                            <strong>Steps</strong>
                            <span>"""
        + calculator.totalSteps()
        + """
                            </span>
                        </div>
                        <div class="summary-item">
                            <strong>✅ Success</strong>
                            <span>"""
        + calculator.successfulSteps()
        + """
                            </span>
                        </div>
                        <div class="summary-item">
                            <strong>❌ Failed</strong>
                            <span>"""
        + calculator.failedSteps()
        + """
                            </span>
                        </div>
                    </div>
                </section>
        """;
  }

  String generateChapterSection(ChapterExecutionResult chapter) {
    // Skip the prequels chapter entirely - prequels are shown within stories that use them
    if (chapter.chapter().title().contains("prequel")) {
      return "";
    }

    String chapterId = HtmlTemplate.sanitizeId(chapter.chapter().title());
    StringBuilder html = new StringBuilder();

    html.append("                <section class=\"chapter\" id=\"")
        .append(chapterId)
        .append("\">\n");
    html.append("                    <detales>\n");
    html.append("                        <summary>\n");
    html.append("                            ")
        .append(HtmlEscaper.escape(chapter.chapter().title()))
        .append("\n");
    html.append("                            ")
        .append(generateStatusBadge(chapter.status().toString().toLowerCase()))
        .append("\n");
    html.append("                        </summary>\n");
    html.append("                        <div class=\"chapter-content\">\n");

    // Add chapter intro if present
    html.append(generateChapterIntro(chapter.chapter().title(), chapter.chapter().summary()));

    // Add stories
    for (StoryExecutionResult story : chapter.storyResults()) {
      // Skip null stories (defensive)
      if (story == null || story.execution() == null) {
        continue;
      }
      // Skip stories that are from the prequels folder (check the path)
      String storyPath = story.execution().path().toString();
      if (!storyPath.contains("prequel")) {
        html.append(generateStorySection(story, chapter.chapter().title()));
      }
    }

    html.append("                        </div>\n");
    html.append("                    </detales>\n");
    html.append("                </section>\n");
    return html.toString();
  }

  private String generateChapterIntro(String title, String summary) {
    StringBuilder html = new StringBuilder();
    html.append("                            <div class=\"chapter-intro\">\n");

    if (title != null) {
      html.append("                                <h3>")
          .append(HtmlEscaper.escape(title))
          .append("</h3>\n");
    }

    if (summary != null && !summary.isBlank()) {
      html.append("                                <div class=\"intro-content\">\n");
      html.append("                                    ")
          .append(MarkdownRenderer.toHtml(summary))
          .append("\n");
      html.append("                                </div>\n");
    }

    html.append("                            </div>\n");
    return html.toString();
  }

  private String generateStoryIntro(String title, String summary) {
    StringBuilder html = new StringBuilder();

    // Only render if there's a summary to show (title is already in the detales summary tag)
    if (summary != null && !summary.isBlank()) {
      html.append("                                        <div class=\"story-intro\">\n");
      html.append("                                            <div class=\"intro-content\">\n");
      html.append("                                                ")
          .append(MarkdownRenderer.toHtml(summary))
          .append("\n");
      html.append("                                            </div>\n");
      html.append("                                        </div>\n");
    }

    return html.toString();
  }

  private String generateStorySection(StoryExecutionResult story, String chapterTitle) {
    String storyId = HtmlTemplate.sanitizeId(chapterTitle + "-" + story.execution().title());
    StringBuilder html = new StringBuilder();

    html.append("                            <div class=\"story\" id=\"")
        .append(storyId)
        .append("\">\n");
    html.append("                                <detales>\n");
    html.append("                                    <summary>\n");
    html.append("                                        ")
        .append(HtmlEscaper.escape(story.execution().title()))
        .append("\n");
    html.append("                                        ")
        .append(generateStatusBadge(story.status().toString().toLowerCase()))
        .append("\n");
    html.append("                                    </summary>\n");
    html.append("                                    <div class=\"story-content\">\n");

    // Add story intro if present
    if (story.execution().title() != null || story.execution().summary() != null) {
      html.append(generateStoryIntro(story.execution().title(), story.execution().summary()));
    }

    // Add prequels section if present
    if (!story.prequelResults().isEmpty()) {
      html.append(generatePrequelsSection(story.prequelResults()));
    }

    for (SceneExecutionResult scene : story.sceneResults()) {
      html.append(generateSceneSection(scene));
    }

    html.append("                                    </div>\n");
    html.append("                                </detales>\n");
    html.append("                            </div>\n");
    return html.toString();
  }

  private String generatePrequelsSection(List<StoryExecutionResult> prequelResults) {
    StringBuilder html = new StringBuilder();
    html.append("                                        <div class=\"prequels-section\">\n");
    html.append("                                            <detales>\n");
    html.append("                                                <summary>Prequels</summary>\n");
    html.append(
        "                                                <div class=\"prequel-content\">\n");

    for (StoryExecutionResult prequel : prequelResults) {
      html.append(
          "                                                    <div class=\"prequel-story\">\n");
      html.append("                                                        <strong>")
          .append(HtmlEscaper.escape(prequel.execution().title()))
          .append("</strong> ")
          .append(generateStatusBadge(prequel.status().toString().toLowerCase()))
          .append("\n");

      // Show prequel scenes
      for (SceneExecutionResult scene : prequel.sceneResults()) {
        html.append(generateSceneSection(scene));
      }

      html.append("                                                    </div>\n");
    }

    html.append("                                                </div>\n");
    html.append("                                            </detales>\n");
    html.append("                                        </div>\n");
    return html.toString();
  }

  private String generateSceneSection(SceneExecutionResult scene) {
    StringBuilder html = new StringBuilder();
    html.append("                                                        <div class=\"scene\">\n");
    html.append("                                                            <h4>Scene: ")
        .append(HtmlEscaper.escape(scene.scene().title()))
        .append(" ")
        .append(generateStatusBadge(scene.status().toString().toLowerCase()))
        .append("</h4>\n");

    // Render scene description if present
    if (scene.scene().description() != null && !scene.scene().description().isBlank()) {
      html.append(
          "                                                            <div class=\"scene-description\">\n");
      html.append("                                                                ")
          .append(MarkdownRenderer.toHtml(scene.scene().description()))
          .append("\n");
      html.append("                                                            </div>\n");
    }

    // Render steps in order, including descriptions
    // Note: Scene description is included in originalSteps as StepDescription, no need to render
    // separately
    int stepExecutionIndex = 0;
    for (Step step : scene.scene().originalSteps()) {
      if (step instanceof StepDescription desc) {
        // Render binding description
        html.append(
            "                                                            <div class=\"binding-description\">\n");
        html.append("                                                                ")
            .append(MarkdownRenderer.toHtml(desc.markdown()))
            .append("\n");
        html.append("                                                            </div>\n");
      } else if (step instanceof StepTask) {
        // Render binding execution result
        if (stepExecutionIndex < scene.stepResults().size()) {
          html.append(generateStepSection(scene.stepResults().get(stepExecutionIndex)));
          stepExecutionIndex++;
        }
      }
    }

    // For code-based stories with no originalSteps, render stepResults directly
    if (scene.scene().originalSteps().isEmpty() && !scene.stepResults().isEmpty()) {
      for (StepExecutionResult stepResult : scene.stepResults()) {
        html.append(generateStepSection(stepResult));
      }
    }

    html.append("                                                        </div>\n");
    return html.toString();
  }

  private String generateStepSection(StepExecutionResult step) {
    StringBuilder html = new StringBuilder();
    String statusClass = step.status().toString().toLowerCase();

    html.append("                                                            <div class=\"binding ")
        .append(statusClass)
        .append("\">\n");
    html.append(
            "                                                                <strong>Step:</strong> ")
        .append(HtmlEscaper.escape(step.execution().call().stepValue()))
        .append(" ")
        .append(generateStatusBadge(statusClass))
        .append("\n");

    if (!step.execution().stepData().isEmpty()) {
      html.append(generateDataTable(step.execution().stepData()));
    }

    if (step.errorMessage() != null || step.throwable() != null) {
      html.append(generateErrorDetales(step));
    }

    html.append("                                                            </div>\n");
    return html.toString();
  }

  private String generateStatusBadge(String statusClass) {
    return "<span class=\"status-badge " + statusClass + "\"></span>";
  }

  private String generateDataTable(List<Map<String, String>> data) {
    if (data.isEmpty()) return "";

    StringBuilder html = new StringBuilder();
    html.append("                <table>\n");
    html.append("                    <thead><tr>\n");

    for (String key : data.getFirst().keySet()) {
      html.append("                        <th>").append(HtmlEscaper.escape(key)).append("</th>\n");
    }
    html.append("                    </tr></thead>\n");

    html.append("                    <tbody>\n");
    for (Map<String, String> row : data) {
      html.append("                    <tr>\n");
      for (String value : row.values()) {
        html.append("                        <td>")
            .append(HtmlEscaper.escape(value))
            .append("</td>\n");
      }
      html.append("                    </tr>\n");
    }
    html.append("                    </tbody>\n");
    html.append("                </table>\n");

    return html.toString();
  }

  private String generateErrorDetales(StepExecutionResult step) {
    StringBuilder html = new StringBuilder();
    html.append("                <div class=\"error-detales\">\n");
    if (step.errorMessage() != null) {
      html.append("                    <strong>Error:</strong> ")
          .append(HtmlEscaper.escape(step.errorMessage()))
          .append("\n");
    }
    if (step.throwable() != null) {
      html.append("                    <strong>Exception:</strong> ")
          .append(HtmlEscaper.escape(step.throwable().toString()))
          .append("\n");
    }
    html.append("                </div>\n");
    return html.toString();
  }
}
