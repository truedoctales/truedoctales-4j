package dev.truedoctales.report.markdown;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.truedoctales.api.model.execution.InputType;
import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BookReportGeneratorTest {

  @TempDir Path tempDir;

  private Path bookDir;
  private Path executionDir;
  private Path outputDir;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    bookDir = tempDir.resolve("book");
    executionDir = tempDir.resolve("execution");
    outputDir = tempDir.resolve("output");
    Files.createDirectories(bookDir);
    Files.createDirectories(executionDir);
    objectMapper = createObjectMapper();
  }

  @Test
  void generate_shouldCopyBookFilesToOutput() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Book Introduction\n\nWelcome.");
    Path chapterDir = Files.createDirectories(bookDir.resolve("01_chapter"));
    Files.writeString(
        chapterDir.resolve("01_story.md"), "# Story\n\n## Scene: Test\n\n> **Plot** Action\n");

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    assertTrue(Files.exists(outputDir.resolve("00_intro.md")));
    assertTrue(Files.exists(outputDir.resolve("01_chapter").resolve("01_story.md")));
  }

  @Test
  void generate_shouldCopyBookRootAssetFolder() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Intro\n");
    Path assetsDir = Files.createDirectories(bookDir.resolve("assets"));
    Files.writeString(assetsDir.resolve("icon.png"), "fake-icon-data");

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    assertTrue(
        Files.exists(outputDir.resolve("assets/icon.png")),
        "Should copy asset folder at book root level");
  }

  @Test
  void generate_shouldCopyChapterLevelAssetFolder() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Intro\n");
    Path chapterDir = Files.createDirectories(bookDir.resolve("01_chapter"));
    Files.writeString(chapterDir.resolve("01_story.md"), "# Story\n");
    Path chAssets = Files.createDirectories(chapterDir.resolve("assets"));
    Files.writeString(chAssets.resolve("diagram.png"), "fake-diagram-data");

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    assertTrue(
        Files.exists(outputDir.resolve("01_chapter/assets/diagram.png")),
        "Should copy asset folder at chapter level");
  }

  @Test
  void generate_shouldEnrichMarkdownWithExecutionResults() throws IOException {
    String storyContent = "# Story\n\n## Scene: Test\n\n> **Greeting** Say Hello\n";
    Path chapterDir = Files.createDirectories(bookDir.resolve("01_chapter"));
    Files.writeString(chapterDir.resolve("01_story.md"), storyContent);

    StoryExecutionResult result =
        buildStoryResult("01_chapter/01_story.md", ExecutionStatus.SUCCESS);
    Path storyJsonDir = Files.createDirectories(executionDir.resolve("01_chapter"));
    objectMapper.writeValue(storyJsonDir.resolve("01_story.json").toFile(), result);

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    String enrichedContent =
        Files.readString(outputDir.resolve("01_chapter").resolve("01_story.md"));
    assertTrue(enrichedContent.contains("> **Greeting** Say Hello ✅"));
  }

  @Test
  void generate_shouldShowFailureInEnrichedMarkdown() throws IOException {
    String storyContent = "# Story\n\n## Scene: Test\n\n> **Quest** Status is\n";
    Path chapterDir = Files.createDirectories(bookDir.resolve("01_chapter"));
    Files.writeString(chapterDir.resolve("01_story.md"), storyContent);

    StoryExecutionResult result =
        buildStoryResultWithError(
            "01_chapter/01_story.md",
            ExecutionStatus.FAILURE,
            "Expected COMPLETED but was IN_PROGRESS");
    Path storyJsonDir = Files.createDirectories(executionDir.resolve("01_chapter"));
    objectMapper.writeValue(storyJsonDir.resolve("01_story.json").toFile(), result);

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    String enrichedContent =
        Files.readString(outputDir.resolve("01_chapter").resolve("01_story.md"));
    assertTrue(enrichedContent.contains("> **Quest** Status is ❌"));
    assertTrue(enrichedContent.contains("Expected COMPLETED but was IN_PROGRESS"));
  }

  @Test
  void generate_shouldHandleMissingExecutionDirectory() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Book\n");

    BookReportGenerator generator =
        new BookReportGenerator(bookDir, tempDir.resolve("nonexistent"), outputDir);

    assertDoesNotThrow(generator::generate);
    assertTrue(Files.exists(outputDir.resolve("00_intro.md")));
  }

  @Test
  void generate_shouldHandleMissingBookDirectory() {
    BookReportGenerator generator =
        new BookReportGenerator(tempDir.resolve("nonexistent"), executionDir, outputDir);

    assertDoesNotThrow(generator::generate);
  }

  @Test
  void generate_shouldSkipMetaJsonFiles() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Book\n");
    objectMapper.writeValue(executionDir.resolve("meta.json").toFile(), Map.of("title", "My Book"));

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    assertDoesNotThrow(generator::generate);
  }

  @Test
  void generate_shouldFallBackToExecutionDirectoryForMarkdown() throws IOException {
    // Code-based stories have no markdown in the book directory.
    // The markdown is generated alongside the JSON in the execution directory.
    String storyContent = "# Code Story\n\n## Scene: Test\n\n> **Code** Test scene\n";
    Path storyJsonDir = Files.createDirectories(executionDir.resolve("03_chapter"));
    Files.writeString(storyJsonDir.resolve("01_code-story.md"), storyContent);

    StoryExecutionResult result =
        buildStoryResult("03_chapter/01_code-story.md", ExecutionStatus.SUCCESS);
    objectMapper.writeValue(storyJsonDir.resolve("01_code-story.json").toFile(), result);

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    Path enrichedPath = outputDir.resolve("03_chapter").resolve("01_code-story.md");
    assertTrue(Files.exists(enrichedPath));
    String enrichedContent = Files.readString(enrichedPath);
    assertTrue(enrichedContent.contains("> **Code** Test scene ✅"));
  }

  @Test
  void generate_shouldEnrichCodeBasedStoryWithTable() throws IOException {
    // Simulates a code-based story with a parameterized test table
    String storyContent =
        """
        # Code Story

        ## Scene: Parameterized scene

        > **Code** Parameterized scene
        >
        > | name  | value |
        > |-------|-------|
        > | alpha | 1     |
        > | beta  | 2     |
        """;
    Path storyJsonDir = Files.createDirectories(executionDir.resolve("03_chapter"));
    Files.writeString(storyJsonDir.resolve("01_param-story.md"), storyContent);

    StoryExecutionResult result =
        buildStoryResult("03_chapter/01_param-story.md", ExecutionStatus.SUCCESS);
    objectMapper.writeValue(storyJsonDir.resolve("01_param-story.json").toFile(), result);

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    Path enrichedPath = outputDir.resolve("03_chapter").resolve("01_param-story.md");
    assertTrue(Files.exists(enrichedPath));
    String enrichedContent = Files.readString(enrichedPath);
    assertTrue(enrichedContent.contains("> **Code** Parameterized scene ✅"));
    assertTrue(enrichedContent.contains("| name  | value |"));
    assertTrue(enrichedContent.contains("| alpha | 1     |"));
  }

  @Test
  void generate_shouldIncludeCodeBasedStoryAlongsideBookIntro() throws IOException {
    // Reproduces the real-world scenario:
    // - Book has 03_chapter/00_intro.md but NOT 01_devil.md (code-based)
    // - Execution dir has 03_chapter/01_devil.json + 03_chapter/01_devil.md (from StoryExtension)
    // Expected: output contains BOTH 00_intro.md AND enriched 01_devil.md
    Path bookChapter = Files.createDirectories(bookDir.resolve("03_chapter"));
    Files.writeString(bookChapter.resolve("00_intro.md"), "# Chapter Intro\n");

    String codeStoryMd =
        """
        # The Devil with the Three Golden Hairs (Code)

        ## Scene: The quest begins

        > **Code** The quest begins
        """;

    // JSON written by StoryExtension uses field-only visibility ObjectMapper.
    // Use the real JSON shape to catch any deserialization mismatch.
    String codeStoryJson =
        """
        {
          "path" : "03_chapter/01_devil.md",
          "title" : "The Devil (Code)",
          "prequelResults" : null,
          "sceneResults" : [ {
            "title" : "The quest begins",
            "lineNumber" : null,
            "stepResults" : [ {
              "lineNumber" : 0,
              "plot" : "Code",
              "pattern" : "questBegins",
              "inputType" : "SEQUENCE",
              "variables" : { },
              "stepData" : [ ],
              "status" : "SUCCESS",
              "errorMessage" : null,
              "throwable" : null
            } ],
            "status" : "SUCCESS"
          } ]
        }
        """;

    Path execChapter = Files.createDirectories(executionDir.resolve("03_chapter"));
    Files.writeString(execChapter.resolve("01_devil.md"), codeStoryMd);
    Files.writeString(execChapter.resolve("01_devil.json"), codeStoryJson);

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    // Book intro must be copied
    Path introPath = outputDir.resolve("03_chapter").resolve("00_intro.md");
    assertTrue(Files.exists(introPath), "00_intro.md should be copied from book directory");

    // Code-based story must appear via execution-directory fallback
    Path devilPath = outputDir.resolve("03_chapter").resolve("01_devil.md");
    assertTrue(Files.exists(devilPath), "01_devil.md should be created from execution directory");

    String enriched = Files.readString(devilPath);
    assertTrue(
        enriched.contains("> **Code** The quest begins ✅"),
        "Code-based story should be enriched with ✅ badge");
  }

  // Helper methods

  private StoryExecutionResult buildStoryResult(String path, ExecutionStatus status) {
    StoryExecutionResult result = new StoryExecutionResult();
    result.setPath(path);
    result.setTitle("Test Story");
    StepExecutionResult step =
        new StepExecutionResult(
            1, "Plot", "pattern", InputType.SEQUENCE, Map.of(), List.of(), status, null, null);
    SceneExecutionResult scene = new SceneExecutionResult("Scene Title", 1, List.of(step), status);
    result.addSceneResult(scene);
    return result;
  }

  private StoryExecutionResult buildStoryResultWithError(
      String path, ExecutionStatus status, String errorMessage) {
    StoryExecutionResult result = new StoryExecutionResult();
    result.setPath(path);
    result.setTitle("Test Story");
    StepExecutionResult step =
        new StepExecutionResult(
            1,
            "Plot",
            "pattern",
            InputType.SEQUENCE,
            Map.of(),
            List.of(),
            status,
            errorMessage,
            null);
    SceneExecutionResult scene = new SceneExecutionResult("Scene Title", 1, List.of(step), status);
    result.addSceneResult(scene);
    return result;
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new Jdk8Module());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setVisibility(
        mapper
            .getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.ANY)
            .withIsGetterVisibility(JsonAutoDetect.Visibility.ANY));
    return mapper;
  }
}
