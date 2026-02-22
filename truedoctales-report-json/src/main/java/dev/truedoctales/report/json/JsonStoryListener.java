package dev.truedoctales.report.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.truedoctales.api.execute.PersistStoryListener;
import dev.truedoctales.api.model.listener.ChapterExecutionResult;
import dev.truedoctales.api.model.listener.StoryBookExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/// Story execution listener that persists execution results as JSON.
///
/// This listener writes one JSON file per story, plus a book-metadata.json file.
/// This allows for distributed test execution and easier merging of results.
public class JsonStoryListener extends PersistStoryListener {

  private final Path outputDirectory;
  private final ObjectMapper objectMapper;
  private StoryBookModel bookModel; // Store book model for chapter metadata generation

  /// Creates a new JSON story listener with default output directory.
  ///
  /// Output will be written to target/book-of-truth/json/
  public JsonStoryListener() {
    this(Paths.get("target/book-of-truth/json"));
  }

  /// Creates a new JSON story listener with specified output directory.
  ///
  /// @param outputDirectory the directory where JSON files will be written
  public JsonStoryListener(Path outputDirectory) {
    this.outputDirectory = outputDirectory;
    this.objectMapper = createObjectMapper();
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    // Don't fail on unknown properties
    mapper.disable(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // Enable field visibility for serialization since our models use methods, not getters
    mapper.setVisibility(
        mapper
            .getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
            .withIsGetterVisibility(
                com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY));
    return mapper;
  }

  @Override
  public void startBook(StoryBookModel storyBookModel) {
    super.startBook(storyBookModel);
    this.bookModel = storyBookModel; // Store for later use
    System.out.println("JsonStoryListener: startBook called for " + storyBookModel.title());
  }

  @Override
  public void closeBook() {
    super.closeBook();
    System.out.println("JsonStoryListener: closeBook called");
    writeJsonOutputs();
  }

  private void writeJsonOutputs() {
    try {
      StoryBookExecutionResult bookResult = getBookResult();
      if (bookResult == null) {
        System.err.println("No book result to write to JSON");
        return;
      }

      System.out.println("Writing book with " + bookResult.chapterResults().size() + " chapters");

      // Ensure output directory exists
      Files.createDirectories(outputDirectory);

      // Write book metadata (includes intro and structure)
      writeBookMetadata(bookResult);

      // Write chapter metadata for ALL chapters from the book model (including empty ones)
      int chapterCount = 0;
      if (bookModel != null) {
        for (ChapterModel chapter : bookModel.chapters()) {
          writeChapterMetadataFromModel(bookResult, chapter);
          chapterCount++;
        }
      }

      // Write each story as a separate JSON file
      int storyCount = 0;
      for (ChapterExecutionResult chapterResult : bookResult.chapterResults()) {
        for (StoryExecutionResult storyResult : chapterResult.storyResults()) {
          writeStoryJson(bookResult, chapterResult, storyResult);
          storyCount++;
        }
      }

      System.out.println(
          "Chapter metadata JSON written: " + chapterCount + " chapters to " + outputDirectory);
      System.out.println(
          "Story execution JSON written: " + storyCount + " stories to " + outputDirectory);
    } catch (IOException e) {
      System.err.println("Failed to write JSON output: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void writeBookMetadata(StoryBookExecutionResult bookResult) throws IOException {
    // Create a simplified book metadata object
    BookMetadata metadata =
        new BookMetadata(bookResult.book().path().toString(), bookResult.book().title());

    Path metadataPath = outputDirectory.resolve("book-metadata.json");
    objectMapper.writeValue(metadataPath.toFile(), metadata);
    System.out.println("Book metadata written to: " + metadataPath);
  }

  private void writeChapterMetadata(
      StoryBookExecutionResult bookResult, ChapterExecutionResult chapterResult)
      throws IOException {
    // Create filename from chapter name
    String chapterName = sanitizeFilename(chapterResult.chapter().title());
    String filename = "chapter--" + chapterName + ".json";

    Path chapterPath = outputDirectory.resolve(filename);

    // Create a chapter metadata wrapper
    ChapterMetadataWrapper wrapper =
        new ChapterMetadataWrapper(
            bookResult.book().path().toString(),
            bookResult.book().title(),
            chapterResult.chapter());

    objectMapper.writeValue(chapterPath.toFile(), wrapper);
  }

  private void writeChapterMetadataFromModel(
      StoryBookExecutionResult bookResult, ChapterModel chapter) throws IOException {
    // Create filename from chapter name
    String chapterName = sanitizeFilename(chapter.title());
    String filename = "chapter--" + chapterName + ".json";

    Path chapterPath = outputDirectory.resolve(filename);

    // Create a chapter metadata wrapper
    ChapterMetadataWrapper wrapper =
        new ChapterMetadataWrapper(
            bookResult.book().path().toString(), bookResult.book().title(), chapter);

    objectMapper.writeValue(chapterPath.toFile(), wrapper);
  }

  private void writeStoryJson(
      StoryBookExecutionResult bookResult,
      ChapterExecutionResult chapterResult,
      StoryExecutionResult storyResult)
      throws IOException {

    // Create filename from chapter and story names
    String chapterName = sanitizeFilename(chapterResult.chapter().title());
    String storyName = sanitizeFilename(storyResult.execution().title());
    String filename = chapterName + "--" + storyName + ".json";

    Path storyPath = outputDirectory.resolve(filename);

    // Create a story execution wrapper that includes context
    StoryExecutionWrapper wrapper =
        new StoryExecutionWrapper(
            bookResult.book().path().toString(),
            bookResult.book().title(),
            chapterResult.chapter(),
            storyResult);

    objectMapper.writeValue(storyPath.toFile(), wrapper);
  }

  private String sanitizeFilename(String name) {
    // Remove invalid filename characters and limit length
    String sanitized = name.replaceAll("[^a-zA-Z0-9-_\\s]", "").replaceAll("\\s+", "-");
    return sanitized.substring(0, Math.min(sanitized.length(), 100));
  }

  /// Returns the output directory for JSON files.
  ///
  /// @return the output directory
  public Path getOutputDirectory() {
    return outputDirectory;
  }

  /// Simple record to hold book metadata.
  record BookMetadata(String path, String title) {}

  /// Record to hold chapter metadata with book context.
  record ChapterMetadataWrapper(String bookPath, String bookTitle, ChapterModel chapter) {}

  /// Wrapper class to include book and chapter context with story execution.
  record StoryExecutionWrapper(
      String bookPath, String bookTitle, ChapterModel chapter, StoryExecutionResult storyResult) {}
}
