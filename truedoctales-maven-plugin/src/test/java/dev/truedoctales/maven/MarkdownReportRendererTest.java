package dev.truedoctales.maven;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.maven.ReportMojo.BookResult;
import dev.truedoctales.maven.ReportMojo.ChapterResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarkdownReportRendererTest {

  private final MarkdownReportRenderer renderer = new MarkdownReportRenderer();

  @Test
  void renderEmptyBook() {
    var book = new BookResult("Empty Book", List.of());
    String md = renderer.render(book);

    assertTrue(md.startsWith("# Empty Book\n"));
    assertTrue(md.contains("| Total stories | 0 |"));
  }

  @Test
  void renderBookWithPassingStory() {
    var step =
        new StepExecutionResult(
            10, "Hero", "Create hero", Map.of(), List.of(), ExecutionStatus.SUCCESS, null, null);
    var scene = new SceneExecutionResult("The scene", 5, List.of(step), ExecutionStatus.SUCCESS);

    var story = new StoryExecutionResult();
    story.setTitle("A brave tale");
    story.setPath("ch1/tale.md");
    story.addSceneResult(scene);

    var chapter = new ChapterResult("Chapter 1", List.of(story));
    var book = new BookResult("My Book", List.of(chapter));

    String md = renderer.render(book);

    assertTrue(md.contains("# My Book"));
    assertTrue(md.contains("| Total stories | 1 |"));
    assertTrue(md.contains("\u2705 Passed | 1 |"));
    assertTrue(md.contains("## Chapter 1"));
    assertTrue(md.contains("### \u2705 A brave tale"));
    assertTrue(md.contains("#### \u2705 The scene"));
    assertTrue(md.contains("| Create hero | \u2705 |"));
  }

  @Test
  void renderBookWithFailedStep() {
    var step =
        new StepExecutionResult(
            10,
            "Quest",
            "Assign quest",
            Map.of(),
            List.of(),
            ExecutionStatus.ERROR,
            "NullPointerException",
            null);
    var scene = new SceneExecutionResult("Failing scene", 5, List.of(step), ExecutionStatus.ERROR);

    var story = new StoryExecutionResult();
    story.setTitle("Broken tale");
    story.setPath("ch1/broken.md");
    story.addSceneResult(scene);

    var chapter = new ChapterResult("Chapter 1", List.of(story));
    var book = new BookResult("Broken Book", List.of(chapter));

    String md = renderer.render(book);

    assertTrue(md.contains("\u26A0\uFE0F Errors | 1 |"));
    assertTrue(md.contains("| Assign quest | \u26A0\uFE0F `NullPointerException` |"));
  }

  @Test
  void renderBookWithMultipleChapters() {
    var step1 =
        new StepExecutionResult(
            1, "P", "Step A", Map.of(), List.of(), ExecutionStatus.SUCCESS, null, null);
    var step2 =
        new StepExecutionResult(
            2, "P", "Step B", Map.of(), List.of(), ExecutionStatus.FAILURE, "assertion", null);

    var scene1 = new SceneExecutionResult("Scene A", 1, List.of(step1), ExecutionStatus.SUCCESS);
    var scene2 = new SceneExecutionResult("Scene B", 2, List.of(step2), ExecutionStatus.FAILURE);

    var story1 = new StoryExecutionResult();
    story1.setTitle("Good");
    story1.setPath("ch1/good.md");
    story1.addSceneResult(scene1);

    var story2 = new StoryExecutionResult();
    story2.setTitle("Bad");
    story2.setPath("ch2/bad.md");
    story2.addSceneResult(scene2);

    var book =
        new BookResult(
            "Multi Chapter",
            List.of(
                new ChapterResult("Chapter 1", List.of(story1)),
                new ChapterResult("Chapter 2", List.of(story2))));

    String md = renderer.render(book);

    assertTrue(md.contains("| Total stories | 2 |"));
    assertTrue(md.contains("\u2705 Passed | 1 |"));
    assertTrue(md.contains("\u274C Failed | 1 |"));
    assertTrue(md.contains("## Chapter 1"));
    assertTrue(md.contains("## Chapter 2"));
  }

  @Test
  void renderStoryWithPrequels() {
    var prequelStep =
        new StepExecutionResult(
            1, "Hero", "Create hero", Map.of(), List.of(), ExecutionStatus.SUCCESS, null, null);
    var prequelScene =
        new SceneExecutionResult("Setup scene", 1, List.of(prequelStep), ExecutionStatus.SUCCESS);

    var prequel = new StoryExecutionResult();
    prequel.setTitle("Setup: Create Heroes");
    prequel.setPath("ch1/setup.md");
    prequel.addSceneResult(prequelScene);

    var mainStep =
        new StepExecutionResult(
            5, "Quest", "Assign quest", Map.of(), List.of(), ExecutionStatus.SUCCESS, null, null);
    var mainScene =
        new SceneExecutionResult("Main scene", 3, List.of(mainStep), ExecutionStatus.SUCCESS);

    var story = new StoryExecutionResult();
    story.setTitle("Main Tale");
    story.setPath("ch1/main.md");
    story.addSceneResult(mainScene);
    story.addPrequelResult(prequel);

    var book = new BookResult("Book", List.of(new ChapterResult("Ch1", List.of(story))));

    String md = renderer.render(book);

    assertTrue(md.contains("### \u2705 Main Tale"));
    assertTrue(md.contains("**Prequels:**"));
    assertTrue(md.contains("### \u2705 Setup: Create Heroes"));
  }
}
