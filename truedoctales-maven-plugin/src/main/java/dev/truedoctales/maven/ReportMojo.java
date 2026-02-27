package dev.truedoctales.maven;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/// Maven goal that generates a Markdown report from True Doc Tales execution results.
///
/// Reads the JSON execution output produced by {@code JsonStoryListener} and generates
/// a Markdown report summarising book, chapter, story, scene, and step results.
///
/// <p>Default binding: {@code verify} phase. Input defaults to
/// {@code ${project.build.directory}/book-of-truth/.execution} and output defaults to
/// {@code ${project.build.directory}/site/truedoctales}.
@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY)
public class ReportMojo extends AbstractMojo {

  /// Directory that contains the JSON execution results produced by {@code JsonStoryListener}.
  @Parameter(
      defaultValue = "${project.build.directory}/book-of-truth/.execution",
      property = "truedoctales.inputDirectory")
  private Path inputDirectory;

  /// Directory where the Markdown report will be written.
  @Parameter(
      defaultValue = "${project.build.directory}/site/truedoctales",
      property = "truedoctales.outputDirectory")
  private Path outputDirectory;

  /// The file name of the generated report.
  @Parameter(defaultValue = "report.md", property = "truedoctales.reportFileName")
  private String reportFileName;

  @Override
  public void execute() throws MojoExecutionException {
    if (!Files.isDirectory(inputDirectory)) {
      getLog().warn("Input directory does not exist: " + inputDirectory);
      return;
    }

    try {
      var bookResult = readExecutionResults();
      String markdown = new MarkdownReportRenderer().render(bookResult);

      Files.createDirectories(outputDirectory);
      Path reportFile = outputDirectory.resolve(reportFileName);
      Files.writeString(reportFile, markdown);

      getLog().info("Truedoctales Markdown report written to " + reportFile);
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to generate Truedoctales report", e);
    }
  }

  /// Reads the JSON execution data and re-assembles it into a {@link BookResult}.
  BookResult readExecutionResults() throws IOException {
    ObjectMapper mapper = createObjectMapper();

    Path metaPath = inputDirectory.resolve("meta.json");
    String bookTitle = "True Doc Tales Report";
    if (Files.exists(metaPath)) {
      var meta = mapper.readValue(metaPath.toFile(), BookMeta.class);
      if (meta.title() != null) {
        bookTitle = meta.title();
      }
    }

    List<ChapterResult> chapters = new ArrayList<>();
    try (DirectoryStream<Path> dirs =
        Files.newDirectoryStream(inputDirectory, Files::isDirectory)) {
      for (Path chapterDir : dirs) {
        chapters.add(readChapter(mapper, chapterDir));
      }
    }

    return new BookResult(bookTitle, chapters);
  }

  private ChapterResult readChapter(ObjectMapper mapper, Path chapterDir) throws IOException {
    String chapterTitle = chapterDir.getFileName().toString();

    Path chapterMeta = chapterDir.resolve("meta.json");
    if (Files.exists(chapterMeta)) {
      var meta = mapper.readValue(chapterMeta.toFile(), ChapterMeta.class);
      if (meta.title() != null) {
        chapterTitle = meta.title();
      }
    }

    List<StoryExecutionResult> stories = new ArrayList<>();
    try (DirectoryStream<Path> jsonFiles = Files.newDirectoryStream(chapterDir, "*.json")) {
      for (Path jsonFile : jsonFiles) {
        if ("meta.json".equals(jsonFile.getFileName().toString())) {
          continue;
        }
        stories.add(mapper.readValue(jsonFile.toFile(), StoryExecutionResult.class));
      }
    }

    return new ChapterResult(chapterTitle, stories);
  }

  private static ObjectMapper createObjectMapper() {
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

  // --- inner value types used for JSON deserialization and rendering ---

  record BookMeta(String title) {}

  record ChapterMeta(String path, String title) {}

  record BookResult(String title, List<ChapterResult> chapters) {}

  record ChapterResult(String title, List<StoryExecutionResult> stories) {}
}
