package dev.truedoctales.parser;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.internal.parsing.StoryBookParser;
import dev.truedoctales.api.model.story.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoryBookParserImplTest {

  @Test
  void parse_shouldThrowExceptionForNonExistentPath() {
    // Arrange
    StoryBookParser parser = new StoryBookParserImpl(Path.of("non-existent-book"));

    // Act & Assert
    assertThrows(IOException.class, parser::parse);
  }

  @Test
  void storyBookModel_shouldCreateWithValidData() {
    // Arrange
    List<ChapterModel> chapters =
        List.of(new ChapterModel(Path.of("test/path"), "Test Chapter",  List.of()));
    ChapterModel intro = new ChapterModel(Path.of("intro/path"), "Intro",  List.of());

    // Act
    StoryBookModel book = new StoryBookModel(Path.of("test-book"), "Test Book", null, chapters);

    // Assert
    assertNotNull(book);
    assertEquals(1, book.chapters().size());
    assertEquals(1, book.chapters().size());
    assertEquals("Test Book", book.title());
  }

  @Test
  void parse_shouldParseTwoStepsWithDescription_Simple() throws IOException {
    // Test Case 1: Two execution steps separated by description (simplest multi-step case)
    String markdownContent =
        """
        # Simple Multi-Step

        ## Story

        ## Scene: Two steps

        > **Plot** First step

        Description between steps.

        > **Plot** Second step
        """;

    MarkdownStoryParser parser = new MarkdownStoryParserImpl();
    java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("two-step-test-");
    java.nio.file.Path storyFile = tempDir.resolve("two-step.md");
    java.nio.file.Files.writeString(storyFile, markdownContent);

    StoryModel story = parser.parse(tempDir, Path.of("two-step.md"));
    SceneModel scene = story.scenes().getFirst();

    // Expected: [StepTask, StepDescription, StepTask]
    assertEquals(2, scene.steps().size(), "Should have 3 elements: step, description, step");

    // Verify first execution step
    var firstActual = (StepTask) scene.steps().getFirst();
    assertEquals("Plot", firstActual.call().plotName());
    assertEquals("First step", firstActual.call().stepValue());
    assertTrue(firstActual.inputRows().isEmpty());

    // Verify second execution step
    var secondActual = (StepTask) scene.steps().get(1);
    assertEquals("Plot", secondActual.call().plotName());
    assertEquals("Second step", secondActual.call().stepValue());
    assertTrue(secondActual.inputRows().isEmpty());

    java.nio.file.Files.deleteIfExists(storyFile);
    java.nio.file.Files.deleteIfExists(tempDir);
  }

  @Test
  void parse_shouldParseTwoStepsWithTables() throws IOException {
    // Test Case 2: Two execution steps with tables, separated by description
    String markdownContent =
        """
        # Steps With Tables

        ## Story

        ## Scene: Create and verify

        > **Operations** Create users
        >
        > | name | email |
        > |------|-------|
        > | John | john@example.com |
        > | Jane | jane@example.com |

        After creating users, verify them.

        > **Operations** Verify user ${email}
        >
        > | email |
        > |-------|
        > | john@example.com |
        > | jane@example.com |
        """;

    MarkdownStoryParser parser = new MarkdownStoryParserImpl();
    java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("tables-test-");
    java.nio.file.Path storyFile = tempDir.resolve("tables.md");
    java.nio.file.Files.writeString(storyFile, markdownContent);

    StoryModel story = parser.parse(tempDir, Path.of("tables.md"));
    SceneModel scene = story.scenes().getFirst();

    // Expected: [StepTask(with table),  StepTask(with table)]
    assertEquals(2, scene.steps().size(), "Should have 3 elements");

    // Verify first step with table
    var firstActual = scene.steps().getFirst();
    assertEquals("Operations", firstActual.call().plotName());
    assertEquals("Create users", firstActual.call().stepValue());
    assertEquals(2, firstActual.inputRows().size());
    assertEquals("John", firstActual.inputRows().get(0).get("name"));
    assertEquals("john@example.com", firstActual.inputRows().get(0).get("email"));
    assertEquals("Jane", firstActual.inputRows().get(1).get("name"));
    assertEquals("jane@example.com", firstActual.inputRows().get(1).get("email"));

    // Verify second step with table
    var secondActual = scene.steps().get(1);
    assertEquals("Operations", secondActual.call().plotName());
    assertEquals("Verify user ${email}", secondActual.call().stepValue());
    assertEquals(2, secondActual.inputRows().size());
    assertEquals("john@example.com", secondActual.inputRows().get(0).get("email"));
    assertEquals("jane@example.com", secondActual.inputRows().get(1).get("email"));

    java.nio.file.Files.deleteIfExists(storyFile);
    java.nio.file.Files.deleteIfExists(tempDir);
  }

  @Test
  void parse_shouldParseTwoStepsWithTablesWitoutDescription() throws IOException {
    // Test Case 2: Two execution steps with tables, separated by description
    String markdownContent =
        """
        # Steps With Tables

        ## Scene: Create and verify

        > **Test** test
        >
        > | name |
        > |------|
        > | John |

        > **Test** test
        >
        > | name |
        > |------|
        > | John |


        """;

    MarkdownStoryParser parser = new MarkdownStoryParserImpl();
    java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("tables-test-");
    java.nio.file.Path storyFile = tempDir.resolve("tables.md");
    java.nio.file.Files.writeString(storyFile, markdownContent);

    StoryModel story = parser.parse(tempDir, Path.of("tables.md"));
    SceneModel scene = story.scenes().getFirst();

    System.out.println("Steps: " + scene.steps());
    scene.steps().forEach(s -> System.out.println(s.getClass().getSimpleName() + ": " + s));

    assertEquals(2, scene.steps().size(), "Should have 2 step tasks");

    java.nio.file.Files.deleteIfExists(storyFile);
    java.nio.file.Files.deleteIfExists(tempDir);
  }

  @Test
  void parse_shouldParseBookExampleWithStepsAndTables() throws IOException {
    // Arrange - Simulate the exact structure from 00_the-first-step.md
    String markdownContent =
        """
        # How the binding works

        At first you need to create a java class with annotation @Plot annotation.

        ## Story

        ## Scene: Our first step.

        We can define a step using the @Step annotation to greet multiple people.

        > **Greeting** Greet ${name}
        >
        > | name  |
        > |-------|
        > | John  |
        > | Jane  |
        > | Doe   |
        """;

    // Act
    MarkdownStoryParser parser = new MarkdownStoryParserImpl();
    java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("book-example-test-");
    java.nio.file.Path storyFile = tempDir.resolve("book-example-story.md");
    java.nio.file.Files.writeString(storyFile, markdownContent);

    StoryModel story = parser.parse(tempDir, Path.of("book-example-story.md"));

    // Assert
    assertNotNull(story);
    assertEquals("How the binding works", story.title());
    assertFalse(story.scenes().isEmpty());

    SceneModel scene = story.scenes().getFirst();
    assertEquals("Our first step.", scene.title());

    // Find the step task (first StepTask in the scene)
    StepTask stepTask = null;
    for (var step : scene.steps()) {
      if (step instanceof StepTask) {
        stepTask = (StepTask) step;
        break;
      }
    }

    assertNotNull(stepTask, "Should find a step task");
    assertEquals("Greeting", stepTask.call().plotName());
    assertEquals("Greet ${name}", stepTask.call().stepValue());
    assertEquals(3, stepTask.inputRows().size());
    assertEquals("John", stepTask.inputRows().get(0).get("name"));
    assertEquals("Jane", stepTask.inputRows().get(1).get("name"));
    assertEquals("Doe", stepTask.inputRows().get(2).get("name"));

    // Cleanup
    java.nio.file.Files.deleteIfExists(storyFile);
    java.nio.file.Files.deleteIfExists(tempDir);
  }
}
