package dev.truedoctales.parser;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.story.StoryModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownStoryParserImplTest {

  public static final String STORY_MD = "01_story.md";
  public static final String MAIN_MD = "02_main.md";
  private MarkdownStoryParser parser;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    parser = new MarkdownStoryParserImpl();
  }

  @Test
  void parse_withoutScene() throws IOException {
    // Arrange
    String content =
        """
            # Simple Story

            Here is some **markdown**

            """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertNotNull(story);
    assertEquals(Path.of(STORY_MD), story.path());
    assertEquals("Simple Story", story.title());
    assertTrue(story.scenes().isEmpty());
  }

  @Test
  void parse_withSceneWithoutStep() throws IOException {
    // Arrange
    String content =
        """
            # Simple Story

            ## First Scene

            Here is some **markdown**

            """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertNotNull(story);
    assertEquals(Path.of(STORY_MD), story.path());
    assertEquals("Simple Story", story.title());
    assertEquals(1, story.scenes().size());
    assertTrue(story.scenes().getFirst().steps().isEmpty());
  }

  @Test
  void parse_withSceneAndWithStep() throws IOException {
    // Arrange
    String content =
        """
        # Simple Story

        Story description here.

        ## Scene: Do something

        > **TestPlot** Action

        > | param |
        > | ----- |
        > | value |
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertNotNull(story);
    assertEquals(Path.of(STORY_MD), story.path());
    assertEquals("Simple Story", story.title());
    assertFalse(story.scenes().isEmpty());
  }

  @Test
  void parse_shouldHandleMultipleScenes() throws IOException {
    // Arrange
    String content =
        """
        # Story

        ## Story

        ## Scene: First scene

        > **TestPlot** First

        ## Scene: Second scene

        > **TestPlot** Second
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertEquals(2, story.scenes().size());
    assertEquals("First scene", story.scenes().get(0).title());
    assertEquals("Second scene", story.scenes().get(1).title());
  }

  @Test
  void parse_shouldHandlePrequelReferences() throws IOException {
    // Arrange
    String prequelContent =
        """
        # Prequel Story

        ## Story

        ## Scene: Setup

        > **TestPlot** Setup
        """;
    createTempFile("prequel.md", prequelContent);

    String mainContent =
        """
        # Main Story

        ## Story

        > Recap

        @Prequel [Setup](prequel.md)

        ## Scene: Main

        > **TestPlot** Main
        """;
    createTempFile(MAIN_MD, mainContent);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(MAIN_MD));

    // Assert
    assertNotNull(story);
    assertFalse(story.prequels().isEmpty());
  }

  @Test
  void parse_shouldHandleTablesWithData() throws IOException {
    // Arrange
    String content =
        """
        # Story

        ## Story

        ## Scene: Test

        > **TestPlot** Test

        > | name | age |
        > | ---- | --- |
        > | John | 30  |
        > | Jane | 25  |
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertFalse(story.scenes().isEmpty());
    assertFalse(story.scenes().getFirst().steps().isEmpty());
    var firstStep = story.scenes().getFirst().steps().getFirst();
    assertNotNull(firstStep);
    assertEquals(2, firstStep.inputRows().size());
  }

  @Test
  void parse_shouldExtractStoryDescription() throws IOException {
    // Arrange
    String content =
        """
        # My Story Title

        This is a multi-line
        description of the story.

        It can have **markdown** too.

        ## Story

        ## Scene: Test

        > **TestPlot** Action
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertEquals("My Story Title", story.title());
  }

  @Test
  void parse_shouldHandleStoryWithoutDescription() throws IOException {
    // Arrange
    String content =
        """
        # Story Title

        ## Story

        ## Scene: Test

        > **TestPlot** Action
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertEquals("Story Title", story.title());
  }

  @Test
  void parse_shouldHandleSceneWithoutDescription() throws IOException {
    // Arrange
    String content =
        """
        # Story

        ## Story

        ## Scene: Test Scene

        > **TestPlot** Action

        > | param |
        > | ----- |
        > | value |
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertEquals(1, story.scenes().size());
    assertEquals("Test Scene", story.scenes().getFirst().title());
  }

  @Test
  void parse_shouldAllowPrequelAnywhere() throws IOException {
    // Arrange - @Prequel can appear anywhere before scenes
    String content =
        """
        # Story

        ## Story

        @Prequel [Setup](prequel.md)

        ## Scene: Test

        > **TestPlot** Action
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert - should parse without throwing and include prequel
    assertNotNull(story);
    assertEquals(1, story.prequels().size());
    assertEquals(Path.of("prequel.md"), story.prequels().getFirst());
  }

  @Test
  void parse_shouldHandleRecapSection() throws IOException {
    // Arrange
    String prequelContent =
        """
        # Prequel

        ## Story

        ## Scene: Setup

        > **TestPlot** Setup
        """;
    createTempFile("prequel.md", prequelContent);

    String content =
        """
        # Story

        ## Story

        > Recap

        @Prequel [Prequel](prequel.md)

        ## Scene: Test

        > **TestPlot** Action
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertEquals(1, story.prequels().size());
    assertEquals(Path.of("prequel.md"), story.prequels().getFirst());
  }

  @Test
  void parse_shouldTreatDoubleHashAsSceneEvenWithoutMarker() throws IOException {
    // Arrange
    String content =
        """
        # Story

        ## Story

        ## Empty Scene

        ## Another Scene

        > **TestPlot** Action
        """;
    createTempFile(STORY_MD, content);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Assert
    assertEquals(2, story.scenes().size());
    assertEquals("Empty Scene", story.scenes().get(0).title());
    assertTrue(story.scenes().get(0).steps().isEmpty());
    assertEquals("Another Scene", story.scenes().get(1).title());
    assertFalse(story.scenes().get(1).steps().isEmpty());
  }

  @Test
  void parse_shouldHandlePrequelsBlock() throws IOException {
    // Arrange
    String prequelContent1 =
        """
        # Setup Users

        ## Scene: Create users

        > **TestPlot** Setup users
        """;
    createTempFile("setup-users.md", prequelContent1);

    String prequelContent2 =
        """
        # Setup Data

        ## Scene: Create data

        > **TestPlot** Setup data
        """;
    createTempFile("setup-data.md", prequelContent2);

    String mainContent =
        """
        # Main Story

        > Prequels
        > - [Setup Users](setup-users.md)
        > - [Setup Data](setup-data.md)

        ## Scene: Main Action

        > **TestPlot** Main action
        """;
    createTempFile(MAIN_MD, mainContent);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(MAIN_MD));

    // Assert
    assertNotNull(story);
    assertEquals(2, story.prequels().size());
    assertEquals(Path.of("setup-users.md"), story.prequels().get(0));
    assertEquals(Path.of("setup-data.md"), story.prequels().get(1));
  }

  @Test
  void parse_shouldHandlePrequelsBlockWithoutBullets() throws IOException {
    // Arrange
    String mainContent =
        """
        # Main Story

        > Prequels
        > [Setup](setup.md)
        > [Data](data.md)

        ## Scene: Main Action

        > **TestPlot** Main action
        """;
    createTempFile("setup.md", "# Setup\n## Scene: S\n> **TestPlot** S");
    createTempFile("data.md", "# Data\n## Scene: D\n> **TestPlot** D");
    createTempFile(MAIN_MD, mainContent);

    // Act
    StoryModel story = parser.parse(tempDir, Path.of(MAIN_MD));

    // Assert
    assertEquals(2, story.prequels().size());
    assertEquals(Path.of("setup.md"), story.prequels().get(0));
    assertEquals(Path.of("data.md"), story.prequels().get(1));
  }

  private Path createTempFile(String fileName, String content) throws IOException {
    Path file = tempDir.resolve(fileName);
    Files.writeString(file, content);
    return file;
  }

  @Test
  void parse_shouldIgnoreStandardCodeBlocksInScenes() throws IOException {
    // Standard fenced code blocks (```lang) between steps must not become fake steps
    String content =
        """
        # Story

        ## Scene: With Code Block

        Here is how you create a plot:

        ```java
        @Plot("Greeting")
        public class GreetingPlot {}
        ```

        > **TestPlot** Action
        """;
    createTempFile(STORY_MD, content);

    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    assertEquals(1, story.scenes().size());
    assertEquals(1, story.scenes().getFirst().steps().size());
    assertEquals("Action", story.scenes().getFirst().steps().getFirst().call().stepValue());
  }

  @Test
  void parse_shouldIgnoreBlockquoteWrappedCodeBlocksInScenes() throws IOException {
    // Blockquote-wrapped code fences (> ```lang) between steps must not become fake steps
    String content =
        """
        # Story

        ## Scene: With Blockquote Code Block

        > ```java
        > @Plot("Greeting")
        > public class GreetingPlot {}
        > ```

        > **TestPlot** Action
        """;
    createTempFile(STORY_MD, content);

    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    assertEquals(1, story.scenes().size());
    assertEquals(1, story.scenes().getFirst().steps().size());
    assertEquals("Action", story.scenes().getFirst().steps().getFirst().call().stepValue());
  }

  @Test
  void parse_shouldIgnoreCodeBlocksBetweenSteps() throws IOException {
    // Code blocks appearing between two executable steps must not create extra steps
    String content =
        """
        # Story

        ## Scene: Mixed

        > **TestPlot** First step

        ```java
        // some example code
        ```

        > **TestPlot** Second step
        """;
    createTempFile(STORY_MD, content);

    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    assertEquals(1, story.scenes().size());
    assertEquals(2, story.scenes().getFirst().steps().size());
    assertEquals("First step", story.scenes().getFirst().steps().get(0).call().stepValue());
    assertEquals("Second step", story.scenes().getFirst().steps().get(1).call().stepValue());
  }

  @Test
  void parse_shouldIgnoreStepCallInsideCodeBlock() throws IOException {
    // A line that looks like an executable step (> **Plot** step) inside a code block
    // must be treated as documentation and not produce an executable StepTask
    String content =
        """
        # Story

        ## Scene: With Step-Like Code Block

        Here is an example of how to call a step in your story:

        ```markdown
        > **Greeting** Say Hello
        ```

        > **TestPlot** Actual step
        """;
    createTempFile(STORY_MD, content);

    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    assertEquals(1, story.scenes().size());
    // Only the real step outside the code block must be parsed
    assertEquals(1, story.scenes().getFirst().steps().size());
    assertEquals("Actual step", story.scenes().getFirst().steps().getFirst().call().stepValue());
  }

  @Test
  void parse_shouldIgnoreSceneHeaderInsideCodeBlock() throws IOException {
    // A ## Scene: heading inside a fenced code block must NOT start a new scene;
    // it is documentation content only.
    String content =
        """
        # Story

        ## Scene: Real Scene

        Here is how a story file is structured:

        ```markdown
        ## Scene: Example scene heading

        > **PlotName** Example step
        ```

        > **TestPlot** Actual step
        """;
    createTempFile(STORY_MD, content);

    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Only one real scene, not two (the one inside the code block must be ignored)
    assertEquals(1, story.scenes().size());
    assertEquals("Real Scene", story.scenes().getFirst().title());
    // Only the actual executable step outside the code block
    assertEquals(1, story.scenes().getFirst().steps().size());
    assertEquals("Actual step", story.scenes().getFirst().steps().getFirst().call().stepValue());
  }

  @Test
  void parse_shouldIgnoreSceneHeaderInsideCodeBlockInHeaderSection() throws IOException {
    // A ## Scene: heading inside a fenced code block in the pre-scene header area
    // must NOT trigger the start of scenes; it is documentation content only.
    String content =
        """
        # Story

        Here is a code example with a fake scene heading:

        ```markdown
        ## Scene: Fake scene inside code block
        > **FakePlot** Fake step
        ```

        ## Scene: Real Scene

        > **TestPlot** Actual step
        """;
    createTempFile(STORY_MD, content);

    StoryModel story = parser.parse(tempDir, Path.of(STORY_MD));

    // Only the real scene must be present
    assertEquals(1, story.scenes().size());
    assertEquals("Real Scene", story.scenes().getFirst().title());
    assertEquals(1, story.scenes().getFirst().steps().size());
    assertEquals("Actual step", story.scenes().getFirst().steps().getFirst().call().stepValue());
  }
}
