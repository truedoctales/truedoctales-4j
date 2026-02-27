package dev.truedoctales.report.markdown;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Logger;

/// Generates an enriched markdown report by merging the original book with execution results.
///
/// The generator:
/// <ol>
///   <li>Copies all original book files (markdown, images, etc.) to the output directory</li>
///   <li>Reads each story execution result from the JSON execution directory</li>
///   <li>Merges the execution results into the copied markdown files</li>
/// </ol>
///
/// Each step in the markdown is annotated with its execution status (✅ SUCCESS, ❌ FAILURE,
/// ⚠️ ERROR, ⏭️ SKIPPED). Failed and errored steps also include the error message.
///
/// ### Example Usage
/// <pre>
/// BookReportGenerator generator = new BookReportGenerator(
///     Path.of("fairy-doc-tales"),
///     Path.of("target/book-of-truth/.execution"),
///     Path.of("target/book-of-truth")
/// );
/// generator.generate();
/// </pre>
public class BookReportGenerator {

  private static final Logger logger = Logger.getLogger(BookReportGenerator.class.getName());

  private final Path bookDirectory;
  private final Path executionDirectory;
  private final Path outputDirectory;
  private final ObjectMapper objectMapper;
  private final StoryReportMerger merger;

  /// Creates a new book report generator.
  ///
  /// @param bookDirectory the root directory of the original book (markdown files)
  /// @param executionDirectory the directory containing JSON execution results
  /// @param outputDirectory the directory where the enriched report will be written
  public BookReportGenerator(Path bookDirectory, Path executionDirectory, Path outputDirectory) {
    this.bookDirectory = bookDirectory;
    this.executionDirectory = executionDirectory;
    this.outputDirectory = outputDirectory;
    this.objectMapper = createObjectMapper();
    this.merger = new StoryReportMerger();
  }

  /// Generates the enriched markdown report.
  ///
  /// @throws IOException if reading or writing files fails
  public void generate() throws IOException {
    logger.info(
        "Generating report from book: " + bookDirectory + " with execution: " + executionDirectory);

    Files.createDirectories(outputDirectory);

    copyBookFiles();
    enrichWithExecutionResults();

    logger.info("Report generated in: " + outputDirectory);
  }

  private void copyBookFiles() throws IOException {
    if (!Files.exists(bookDirectory)) {
      logger.warning("Book directory not found: " + bookDirectory);
      return;
    }
    Files.walkFileTree(
        bookDirectory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path target = outputDirectory.resolve(bookDirectory.relativize(dir));
            Files.createDirectories(target);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Path target = outputDirectory.resolve(bookDirectory.relativize(file));
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private void enrichWithExecutionResults() throws IOException {
    if (!Files.exists(executionDirectory)) {
      logger.warning("Execution directory not found: " + executionDirectory);
      return;
    }
    Files.walkFileTree(
        executionDirectory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (isStoryJsonFile(file)) {
              enrichStoryFile(file);
            }
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private boolean isStoryJsonFile(Path file) {
    String name = file.getFileName().toString();
    return name.endsWith(".json") && !name.equals("meta.json");
  }

  private void enrichStoryFile(Path jsonFile) {
    try {
      StoryExecutionResult result =
          objectMapper.readValue(jsonFile.toFile(), StoryExecutionResult.class);
      if (result == null || result.getPath() == null) {
        return;
      }

      Path markdownPath = bookDirectory.resolve(result.getPath());
      if (!Files.exists(markdownPath)) {
        logger.warning("Original markdown not found: " + markdownPath);
        return;
      }

      String originalMarkdown = Files.readString(markdownPath);
      String enriched = merger.merge(originalMarkdown, result);

      Path outputPath = outputDirectory.resolve(result.getPath());
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, enriched);

      logger.info("Enriched: " + result.getPath());
    } catch (IOException e) {
      logger.warning("Failed to enrich story: " + jsonFile + " - " + e.getMessage());
    }
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new Jdk8Module());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
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
