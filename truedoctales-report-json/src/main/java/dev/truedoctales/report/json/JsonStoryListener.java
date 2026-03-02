package dev.truedoctales.report.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.truedoctales.api.execute.PersistStoryListener;
import dev.truedoctales.api.model.listener.ChapterExecutionResult;
import dev.truedoctales.api.model.listener.StoryBookExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/// Story execution listener that persists execution results as JSON.
///
/// This listener writes one JSON file per story, plus a book-metadata.json file.
/// This allows for distributed test execution and easier merging of results.
public class JsonStoryListener extends PersistStoryListener {

  private static final Logger logger = Logger.getLogger(JsonStoryListener.class.getName());

  private final Path outputDirectory;
  private final ObjectMapper objectMapper;

  /// Creates a new JSON story listener with default output directory.
  ///
  /// Output will be written to target/truedoctales/json/
  public JsonStoryListener() {
    this(Paths.get("target/truedoctales-report/"));
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
    // Store book model for chapter metadata generation
    logger.info("JsonStoryListener: startBook called for " + storyBookModel.title());
  }

  @Override
  public void closeBook() {
    super.closeBook();
    logger.info("JsonStoryListener: closeBook called");
    writeJsonOutputs();
  }

  private void writeJsonOutputs() {
    try {
      StoryBookExecutionResult bookResult = getBookResult();
      if (bookResult == null) {
        logger.warning("No book result to write to JSON");
        return;
      }

      logger.info("Writing book with " + bookResult.getChapters().size() + " chapters");

      // Ensure output directory exists
      Files.createDirectories(outputDirectory);

      // Write book metadata (includes intro and structure)
      writeBookMetadata(bookResult);
      bookResult
          .getChapters()
          .forEach(
              chapterModel -> {
                try {
                  writeChapter(chapterModel);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    } catch (IOException e) {
      logger.severe("Failed to write JSON output: " + e.getMessage());
      logger.throwing(JsonStoryListener.class.getName(), "writeJsonOutputs", e);
    }
  }

  private void writeChapter(ChapterExecutionResult chapterModel) throws IOException {
    Path chapterDir = Files.createDirectories(outputDirectory.resolve(chapterModel.getPath()));
    writeChapterMetadata(chapterDir, chapterModel);
    for (StoryExecutionResult storyResult : chapterModel.getStories()) {
      writeStoryJson(storyResult);
    }
  }

  private void writeBookMetadata(StoryBookExecutionResult bookResult) throws IOException {
    Path metadataPath = outputDirectory.resolve("meta.json");
    objectMapper.writeValue(metadataPath.toFile(), new BookMetadata(bookResult.getTitle()));
  }

  private void writeChapterMetadata(Path outputChapterDir, ChapterExecutionResult chapterResult)
      throws IOException {
    // Create filename from chapter name
    String filename = "meta.json";
    Path chapterMeta = outputChapterDir.resolve(filename);
    objectMapper.writeValue(
        chapterMeta.toFile(),
        new ChapterMeta(
            chapterResult.getNumber(), chapterResult.getPath(), chapterResult.getTitle()));
  }

  private void writeStoryJson(StoryExecutionResult storyResult) throws IOException {

    Path storyPath = outputDirectory.resolve(storyResult.getPath());
    //    change from markdown to json
    Path storyMetaJson =
        storyPath
            .getParent()
            .resolve(storyPath.getFileName().toString().replaceAll("\\.md$", ".json"));

    objectMapper.writeValue(storyMetaJson.toFile(), storyResult);
  }

  /// Simple record to hold book metadata.
  record BookMetadata(String title) {}

  record ChapterMeta(Integer number, String path, String title) {}
}
