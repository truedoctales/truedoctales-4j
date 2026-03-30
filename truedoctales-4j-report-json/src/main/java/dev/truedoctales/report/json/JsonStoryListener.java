package dev.truedoctales.report.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.truedoctales.api.execute.PersistStoryListener;
import dev.truedoctales.api.model.listener.ChapterExecutionResult;
import dev.truedoctales.api.model.listener.StoryBookExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.api.model.plot.PlotBinding;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/// Story execution listener that persists execution results as JSON.
///
/// This listener writes one JSON file per story, plus a book-metadata.json file.
/// This allows for distributed test execution and easier merging of results.
public class JsonStoryListener extends PersistStoryListener {

  private static final Logger logger = Logger.getLogger(JsonStoryListener.class.getName());

  private final Path outputDirectory;
  private final ObjectMapper objectMapper;
  private StoryBookModel currentBookModel;

  /// Creates a new JSON story listener with default output directory.
  ///
  /// Output will be written to target/truedoctales-report/
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
    mapper.disable(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
    this.currentBookModel = storyBookModel;
    logger.info("JsonStoryListener: startBook called for " + storyBookModel.title());
  }

  @Override
  public void onPlotBindings(Set<PlotBinding> bindings) {
    try {
      Files.createDirectories(outputDirectory);
      Path plotRegistryPath = outputDirectory.resolve("plot-registry.json");
      objectMapper.writeValue(plotRegistryPath.toFile(), new PlotRegistry(bindings));
      logger.info(
          "JsonStoryListener: plot-registry.json written with " + bindings.size() + " plots");
    } catch (IOException e) {
      logger.warning("Failed to write plot-registry.json: " + e.getMessage());
    }
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

      Files.createDirectories(outputDirectory);

      // Collect all unique prequel stories before writing anything else
      Map<String, StoryExecutionResult> prequelsByPath = collectPrequels(bookResult);

      // Determine if the book has a root-level intro file
      boolean hasIntro =
          currentBookModel != null
              && Files.isRegularFile(currentBookModel.path().resolve("00_intro.md"));

      writeBookMetadata(bookResult, hasIntro);

      // Write 00_prequels chapter if there are any prequel stories
      if (!prequelsByPath.isEmpty()) {
        writePrequelChapter(prequelsByPath);
      }

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

  /// Collects all unique prequel stories across all chapters and stories, keyed by path.
  private Map<String, StoryExecutionResult> collectPrequels(StoryBookExecutionResult bookResult) {
    Map<String, StoryExecutionResult> prequelsByPath = new LinkedHashMap<>();
    for (ChapterExecutionResult chapter : bookResult.getChapters()) {
      if (chapter.getStories() == null) continue;
      for (StoryExecutionResult story : chapter.getStories()) {
        collectPrequelsFromStory(story, prequelsByPath);
      }
    }
    return prequelsByPath;
  }

  private void collectPrequelsFromStory(
      StoryExecutionResult story, Map<String, StoryExecutionResult> collected) {
    if (story.getPrequelResults() == null) return;
    for (StoryExecutionResult prequel : story.getPrequelResults()) {
      collected.putIfAbsent(prequel.getPath(), prequel);
    }
  }

  /// Writes the 00_prequels chapter directory with meta.json and individual story JSON files.
  private void writePrequelChapter(Map<String, StoryExecutionResult> prequelsByPath)
      throws IOException {
    Path prequelDir = Files.createDirectories(outputDirectory.resolve("00_prequels"));
    // Write chapter meta
    objectMapper.writeValue(
        prequelDir.resolve("meta.json").toFile(), new ChapterMeta(0, "00_prequels", "Prequels"));
    // Write each prequel story
    for (StoryExecutionResult prequel : prequelsByPath.values()) {
      Path storyPath = outputDirectory.resolve(prequel.getPath());
      Path storyJson =
          storyPath
              .getParent()
              .resolve(storyPath.getFileName().toString().replaceAll("\\.md$", ".json"));
      Files.createDirectories(storyJson.getParent());
      objectMapper.writeValue(storyJson.toFile(), prequel);
      logger.info("  Wrote prequel: " + storyJson);
    }
  }

  private void writeChapter(ChapterExecutionResult chapterModel) throws IOException {
    Path chapterDir = Files.createDirectories(outputDirectory.resolve(chapterModel.getPath()));
    writeChapterMetadata(chapterDir, chapterModel);
    for (StoryExecutionResult storyResult : chapterModel.getStories()) {
      writeStoryJson(storyResult);
    }
  }

  private void writeBookMetadata(StoryBookExecutionResult bookResult, boolean hasIntro)
      throws IOException {
    Path metadataPath = outputDirectory.resolve("meta.json");
    objectMapper.writeValue(
        metadataPath.toFile(), new BookMetadata(bookResult.getTitle(), hasIntro));
  }

  private void writeChapterMetadata(Path outputChapterDir, ChapterExecutionResult chapterResult)
      throws IOException {
    Path chapterMeta = outputChapterDir.resolve("meta.json");
    objectMapper.writeValue(
        chapterMeta.toFile(),
        new ChapterMeta(
            chapterResult.getNumber(), chapterResult.getPath(), chapterResult.getTitle()));
  }

  private void writeStoryJson(StoryExecutionResult storyResult) throws IOException {
    Path storyPath = outputDirectory.resolve(storyResult.getPath());
    Path storyMetaJson =
        storyPath
            .getParent()
            .resolve(storyPath.getFileName().toString().replaceAll("\\.md$", ".json"));
    objectMapper.writeValue(storyMetaJson.toFile(), storyResult);
  }

  /// Simple record to hold book metadata.
  record BookMetadata(String title, boolean hasIntro) {}

  record ChapterMeta(Integer number, String path, String title) {}

  /// Record wrapping the full set of plot bindings for JSON serialisation.
  record PlotRegistry(Set<PlotBinding> plots) {}
}
