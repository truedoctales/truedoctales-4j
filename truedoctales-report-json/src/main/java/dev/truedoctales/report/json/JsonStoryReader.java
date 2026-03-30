package dev.truedoctales.report.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.truedoctales.api.model.listener.StoryBookExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.api.model.story.ChapterModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/// Utility for reading JSON story execution results.
///
/// Provides methods to deserialize JSON files back into StoryBookExecutionResult objects,
/// enabling report generation from persisted test execution data.
public class JsonStoryReader {

  private final ObjectMapper objectMapper;

  /// Creates a new JSON story reader with default configuration.
  public JsonStoryReader() {
    this.objectMapper = createObjectMapper();
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    // Enable field visibility for deserialization
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

  /// Reads a story book execution result from a JSON file.
  ///
  /// @param jsonFile the JSON file to read from
  /// @return the deserialized story book execution result
  /// @throws IOException if reading or parsing fails
  public StoryBookExecutionResult read(File jsonFile) throws IOException {
    if (!jsonFile.exists()) {
      throw new IOException("JSON file not found: " + jsonFile.getAbsolutePath());
    }

    return objectMapper.readValue(jsonFile, StoryBookExecutionResult.class);
  }

  /// Reads a story book execution result from a JSON file path.
  ///
  /// @param jsonPath the path to the JSON file
  /// @return the deserialized story book execution result
  /// @throws IOException if reading or parsing fails
  public StoryBookExecutionResult read(Path jsonPath) throws IOException {
    return read(jsonPath.toFile());
  }

  /// Reads a story book execution result from a JSON file path string.
  ///
  /// @param jsonFilePath the path string to the JSON file
  /// @return the deserialized story book execution result
  /// @throws IOException if reading or parsing fails
  public StoryBookExecutionResult read(String jsonFilePath) throws IOException {
    return read(new File(jsonFilePath));
  }

  /// Reads book metadata from a JSON file.
  ///
  /// @param metadataPath path to the book-metadata.json file
  /// @return the book metadata
  /// @throws IOException if reading or parsing fails
  public BookMetadata readBookMetadata(Path metadataPath) throws IOException {
    return objectMapper.readValue(metadataPath.toFile(), BookMetadata.class);
  }

  /// Reads a story execution wrapper from a JSON file.
  ///
  /// @param storyPath path to the story JSON file
  /// @return the story execution wrapper
  /// @throws IOException if reading or parsing fails
  public StoryExecutionWrapper readStoryExecution(Path storyPath) throws IOException {
    return objectMapper.readValue(storyPath.toFile(), StoryExecutionWrapper.class);
  }

  /// Reads chapter metadata from a JSON file.
  ///
  /// @param chapterPath path to the chapter metadata JSON file
  /// @return the chapter metadata wrapper
  /// @throws IOException if reading or parsing fails
  public ChapterMetadataWrapper readChapterMetadata(Path chapterPath) throws IOException {
    return objectMapper.readValue(chapterPath.toFile(), ChapterMetadataWrapper.class);
  }

  /// Record for book metadata from JSON.
  public record BookMetadata(String path, String title, String summary, Object intro) {}

  /// Record for chapter metadata with book context from JSON.
  public record ChapterMetadataWrapper(String bookPath, String bookTitle, ChapterModel chapter) {}

  /// Record for story execution with context from JSON.
  public record StoryExecutionWrapper(
      String bookPath, String bookTitle, ChapterModel chapter, StoryExecutionResult storyResult) {}
}
