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
        List.of(new ChapterModel(Path.of("test/path"), "Test Chapter", null, List.of()));
    ChapterModel intro = new ChapterModel(Path.of("intro/path"), "Intro", null, List.of());

    // Act
    StoryBookModel book =
        new StoryBookModel(Path.of("test-book"), "Test Book", null, intro, chapters);

    // Assert
    assertNotNull(book);
    assertEquals(1, book.chapters().size());
    assertNotNull(book.getIntro());
    assertEquals(1, book.chapters().size());
    assertEquals("Test Book", book.title());
  }

  @Test
  void parse_shouldParseStepWithTableInputFromMarkdown() throws IOException {
    // Arrange - Create a test Markdown file with step and table input
    String markdownContent =
        """
        # Greeting Example

        This demonstrates how to use steps with table input.

        ## Story

        ## Scene: Greet multiple people

        A scene that shows how to greet different people using variables and table data.

        > **Greeting** Greet ${name}
        >
        > | name  |
        > |-------|
        > | John  |
        > | Jane  |
        > | Alice |
        """;

    // Act - Parse this content using MarkdownStoryParser directly
    MarkdownStoryParser parser = new MarkdownStoryParserImpl();
    java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("story-test-");
    java.nio.file.Path storyFile = tempDir.resolve("test-story.md");
    java.nio.file.Files.writeString(storyFile, markdownContent);

    StoryModel story = parser.parse(tempDir, Path.of("test-story.md"));

    // Assert
    assertNotNull(story);
    assertEquals("Greeting Example", story.title());
    assertEquals("This demonstrates how to use steps with table input.", story.summary());
    assertFalse(story.scenes().isEmpty());

    // Verify the scene structure
    SceneModel scene = story.scenes().getFirst();
    assertEquals("Greet multiple people", scene.title());
    assertNotNull(scene.description());
    assertTrue(scene.description().contains("scene that shows how to greet different people"));

    // Scene should have description + step task
    assertTrue(scene.steps().size() >= 2, "Scene should have description and step");

    // First element might be description
    int stepIndex = 0;
    if (scene.steps().getFirst() instanceof StepDescription) {
      stepIndex = 1; // Skip description to get to step task
    }

    // Verify the step task with table data
    var stepTask = (StepTask) scene.steps().get(stepIndex);
    assertEquals("Greeting", stepTask.call().plotName());
    assertEquals("Greet ${name}", stepTask.call().stepValue());
    assertEquals(3, stepTask.inputRows().size());

    // Verify table rows
    assertEquals("John", stepTask.inputRows().get(0).get("name"));
    assertEquals("Jane", stepTask.inputRows().get(1).get("name"));
    assertEquals("Alice", stepTask.inputRows().get(2).get("name"));

    // Cleanup
    java.nio.file.Files.deleteIfExists(storyFile);
    java.nio.file.Files.deleteIfExists(tempDir);
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
    assertEquals(3, scene.steps().size(), "Should have 3 elements: step, description, step");

    // Verify first execution step
    var firstActual = (StepTask) scene.steps().getFirst();
    assertEquals("Plot", firstActual.call().plotName());
    assertEquals("First step", firstActual.call().stepValue());
    assertTrue(firstActual.inputRows().isEmpty());

    // Verify description
    var descActual = (StepDescription) scene.steps().get(1);
    assertEquals("Description between steps.", descActual.markdown());

    // Verify second execution step
    var secondActual = (StepTask) scene.steps().get(2);
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

    // Expected: [StepTask(with table), StepDescription, StepTask(with table)]
    assertEquals(3, scene.steps().size(), "Should have 3 elements");

    // Verify first step with table
    var firstActual = (StepTask) scene.steps().getFirst();
    assertEquals("Operations", firstActual.call().plotName());
    assertEquals("Create users", firstActual.call().stepValue());
    assertEquals(2, firstActual.inputRows().size());
    assertEquals("John", firstActual.inputRows().get(0).get("name"));
    assertEquals("john@example.com", firstActual.inputRows().get(0).get("email"));
    assertEquals("Jane", firstActual.inputRows().get(1).get("name"));
    assertEquals("jane@example.com", firstActual.inputRows().get(1).get("email"));

    // Verify description
    var descActual = (StepDescription) scene.steps().get(1);
    assertEquals("After creating users, verify them.", descActual.markdown());

    // Verify second step with table
    var secondActual = (StepTask) scene.steps().get(2);
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
  void parse_shouldParseRealWorldExample_FromMarkdown() throws IOException {
    // Test Case 3: Real-world example from 00_the-first-step.md
    // Three execution steps with code blocks and descriptions between them
    String markdownContent =
        """
        # How the binding works

        At first you need to create a java class with annotation @Plot annotation.

        ```java
        @Plot("Greeting")
        public class SamplePlot {
            // plot implementation
        }
        ```

        ## Story

        ## Scene: Our first step.

        We can define a step using the @Step annotation.
        ```java
        @Step("Say Hello")
        public void sayHello() {
            System.out.println("Hello, True Doc Tales!");
        }
        ```

        > **Greeting** Say Hello


        Now we want to call the step with a variable.

        ```java
        @Step("Greet ${name}")
        public void greet(String name) {
            System.out.println("Hello, " + name + "!");
        }
        ```
        > **Greeting** Greet John

        Or greet multiple people.
        You can still use the same plot.

        > **Greeting** Greet ${name}
        >
        > | name  |
        > |-------|
        > | John  |
        > | Jane  |
        > | Doe   |
        """;

    MarkdownStoryParser parser = new MarkdownStoryParserImpl();
    java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("real-world-test-");
    java.nio.file.Path storyFile = tempDir.resolve("real-world.md");
    java.nio.file.Files.writeString(storyFile, markdownContent);

    StoryModel story = parser.parse(tempDir, Path.of("real-world.md"));
    SceneModel scene = story.scenes().getFirst();

    // Expected structure: [Description, StepTask, Description, StepTask, Description, StepTask]
    // 6 total elements: 3 execution steps + 3 descriptions
    assertEquals(6, scene.steps().size(), "Should have 6 elements: 3 descriptions + 3 steps");

    // Element 0: Scene description with code block
    assertInstanceOf(
        StepDescription.class, scene.steps().get(0), "Element 0 should be scene description");

    // Element 1: First execution step "Say Hello" (no table)
    assertInstanceOf(StepTask.class, scene.steps().get(1), "Element 1 should be StepTask");
    var step1Actual = (StepTask) scene.steps().get(1);
    assertEquals("Greeting", step1Actual.call().plotName());
    assertEquals("Say Hello", step1Actual.call().stepValue());
    assertTrue(step1Actual.inputRows().isEmpty());

    // Element 2: Description between step 1 and 2
    assertInstanceOf(
        StepDescription.class, scene.steps().get(2), "Element 2 should be description");
    var desc2 = (StepDescription) scene.steps().get(2);
    assertTrue(
        desc2.markdown().contains("Now we want to call the step with a variable"),
        "Description should contain expected text");

    // Element 3: Second execution step "Greet John" (no table)
    assertInstanceOf(StepTask.class, scene.steps().get(3), "Element 3 should be StepTask");
    var step2Actual = (StepTask) scene.steps().get(3);
    assertEquals("Greeting", step2Actual.call().plotName());
    assertEquals("Greet John", step2Actual.call().stepValue());
    assertTrue(step2Actual.inputRows().isEmpty());

    // Element 4: Description between step 2 and 3
    assertInstanceOf(
        StepDescription.class, scene.steps().get(4), "Element 4 should be description");
    var desc3 = (StepDescription) scene.steps().get(4);
    assertTrue(
        desc3.markdown().contains("Or greet multiple people"),
        "Description should contain expected text");

    // Element 5: Third execution step "Greet ${name}" WITH table
    assertInstanceOf(StepTask.class, scene.steps().get(5), "Element 5 should be StepTask");
    var step3Actual = (StepTask) scene.steps().get(5);
    assertEquals("Greeting", step3Actual.call().plotName());
    assertEquals("Greet ${name}", step3Actual.call().stepValue());
    assertEquals(3, step3Actual.inputRows().size());
    assertEquals("John", step3Actual.inputRows().get(0).get("name"));
    assertEquals("Jane", step3Actual.inputRows().get(1).get("name"));
    assertEquals("Doe", step3Actual.inputRows().get(2).get("name"));

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
