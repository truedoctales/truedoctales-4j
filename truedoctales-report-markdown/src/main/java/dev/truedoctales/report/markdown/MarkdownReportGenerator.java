package dev.truedoctales.report.markdown;

import dev.truedoctales.api.model.listener.ChapterExecutionResult;
import dev.truedoctales.api.model.listener.StoryBookExecutionResult;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import dev.truedoctales.report.json.JsonStoryReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/// Standalone Markdown report generator that reads from JSON files.
///
/// This class generates Markdown documentation by reading test execution results
/// from JSON files (one per story plus book-metadata.json), enabling report
/// generation independent of test execution.
public class MarkdownReportGenerator {

  private final Path jsonInputDirectory;
  private final Path markdownOutputPath;

  /// Creates a new Markdown report generator.
  ///
  /// @param jsonInputDirectory directory containing story JSON files and book-metadata.json
  /// @param markdownOutputPath path where the Markdown report will be generated
  public MarkdownReportGenerator(Path jsonInputDirectory, Path markdownOutputPath) {
    this.jsonInputDirectory = jsonInputDirectory;
    this.markdownOutputPath = markdownOutputPath;
  }

  /// Generates the Markdown report by reading JSON files and producing Markdown output.
  ///
  /// @throws IOException if reading JSON or writing Markdown fails
  public void generate() throws IOException {
    // Read all story JSON files and combine them into a single book result
    StoryBookExecutionResult bookResult = combineStoryJsonFiles();

    // Generate Markdown report
    MarkdownContentGenerator generator = new MarkdownContentGenerator(markdownOutputPath);
    generator.generateReport(bookResult);

    System.out.println("Markdown report generated at: " + markdownOutputPath.toAbsolutePath());
  }

  private StoryBookExecutionResult combineStoryJsonFiles() throws IOException {
    JsonStoryReader reader = new JsonStoryReader();

    // Read book metadata
    Path metadataPath = jsonInputDirectory.resolve("book-metadata.json");
    JsonStoryReader.BookMetadata metadata = reader.readBookMetadata(metadataPath);

    // Create book model from metadata
    Path bookPath;
    try {
      bookPath = Path.of(java.net.URI.create(metadata.path()));
    } catch (Exception e) {
      // If path is not a valid URI, treat as relative path
      bookPath = Path.of(metadata.path());
    }

    StoryBookModel bookModel =
        new StoryBookModel(
            bookPath,
            metadata.title(),
            metadata.summary(),
            null, // intro chapter will be added if found
            new ArrayList<>());

    // Create book execution result
    StoryBookExecutionResult bookResult = new StoryBookExecutionResult(bookModel);

    // Read chapter metadata files to get chapter intros
    Map<String, ChapterModel> chapterMetadata = new HashMap<>();
    try (Stream<Path> paths = Files.list(jsonInputDirectory)) {
      paths
          .filter(path -> path.toString().endsWith(".json"))
          .filter(path -> path.getFileName().toString().startsWith("chapter--"))
          .forEach(
              path -> {
                try {
                  JsonStoryReader.ChapterMetadataWrapper wrapper = reader.readChapterMetadata(path);
                  chapterMetadata.put(wrapper.chapter().title(), wrapper.chapter());
                } catch (IOException e) {
                  System.err.println(
                      "Failed to read chapter metadata: " + path + " - " + e.getMessage());
                }
              });
    }

    // Group stories by chapter
    Map<String, List<JsonStoryReader.StoryExecutionWrapper>> storiesByChapter = new HashMap<>();

    // Read all story JSON files (skip chapter metadata files)
    try (Stream<Path> paths = Files.list(jsonInputDirectory)) {
      paths
          .filter(path -> path.toString().endsWith(".json"))
          .filter(path -> !path.getFileName().toString().equals("book-metadata.json"))
          .filter(
              path ->
                  !path.getFileName()
                      .toString()
                      .startsWith("chapter--")) // Skip chapter metadata files
          .forEach(
              path -> {
                try {
                  JsonStoryReader.StoryExecutionWrapper wrapper = reader.readStoryExecution(path);
                  String chapterTitle = wrapper.chapter().title();
                  storiesByChapter
                      .computeIfAbsent(chapterTitle, k -> new ArrayList<>())
                      .add(wrapper);
                } catch (IOException e) {
                  System.err.println("Failed to read story JSON: " + path + " - " + e.getMessage());
                }
              });
    }

    // Create chapter execution results (sorted by chapter title for consistent ordering)
    storiesByChapter.entrySet().stream()
        .sorted(Map.Entry.comparingByKey()) // Sort chapters alphabetically by title
        .forEach(
            entry -> {
              List<JsonStoryReader.StoryExecutionWrapper> wrappers = entry.getValue();
              if (wrappers.isEmpty()) {
                return;
              }

              // Use chapter metadata if available, otherwise use chapter from first story
              String chapterTitle = entry.getKey();
              ChapterModel chapter =
                  chapterMetadata.getOrDefault(chapterTitle, wrappers.get(0).chapter());
              ChapterExecutionResult chapterResult = new ChapterExecutionResult(chapter);

              // Add all stories to this chapter
              for (JsonStoryReader.StoryExecutionWrapper wrapper : wrappers) {
                chapterResult.addStoryResult(wrapper.storyResult());
              }

              bookResult.addChapterResult(chapterResult);
            });

    return bookResult;
  }

  /// Main method for standalone execution.
  ///
  /// @param args command line arguments: [jsonInputDirectory] [markdownOutputPath]
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println(
          "Usage: MarkdownReportGenerator <jsonInputDirectory> <markdownOutputPath>");
      System.exit(1);
    }

    Path jsonInput = Path.of(args[0]);
    Path markdownOutput = Path.of(args[1]);

    MarkdownReportGenerator generator = new MarkdownReportGenerator(jsonInput, markdownOutput);
    generator.generate();
  }
}
