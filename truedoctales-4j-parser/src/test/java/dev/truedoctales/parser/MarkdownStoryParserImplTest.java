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
}
