package dev.truedoctales.maven;

import dev.truedoctales.report.html.HtmlBookReportGenerator;
import dev.truedoctales.report.markdown.BookReportGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/// Maven goal that generates an enriched book report from True Doc Tales execution results.
///
/// Delegates to {@link BookReportGenerator} from the {@code truedoctales-report-markdown} module
/// and to {@link HtmlBookReportGenerator} from the {@code truedoctales-report-html} module.
///
/// <p>Intended usage: bind this goal to the {@code verify} phase so that {@code mvn clean verify}
/// both runs the tests and generates the enriched reports. Markdown output is written to
/// {@code ${project.build.directory}/truedoctales-markdown} and HTML output to
/// {@code ${project.build.directory}/truedoctales-html}.
///
/// <p>Use the {@code reportFormats} parameter to control which output formats are generated
/// (MARKDOWN, HTML, or both).
@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY)
public class ReportMojo extends AbstractMojo {

  /// Path to the original book directory containing the markdown stories.
  @Parameter(
      defaultValue = "${project.basedir}/../fairy-doc-tales",
      property = "truedoctales.bookDirectory")
  private Path bookDirectory;

  /// Directory that contains the JSON execution results produced by {@code JsonStoryListener}.
  @Parameter(
      defaultValue = "${project.build.directory}/truedoctales-report",
      property = "truedoctales.executionDirectory")
  private Path executionDirectory;

  /// Directory where the enriched book copy will be written.
  @Parameter(
      defaultValue = "${project.build.directory}/truedoctales-markdown",
      property = "truedoctales.outputDirectory")
  private Path outputDirectory;

  /// List of report formats to generate. Supported values: MARKDOWN, HTML.
  /// Defaults to both MARKDOWN and HTML.
  @Parameter(property = "truedoctales.reportFormats")
  private List<OutputFormat> reportFormats;

  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Book directory: " + bookDirectory);
    getLog().info("Execution directory: " + executionDirectory);
    getLog().info("Output directory: " + outputDirectory);

    if (reportFormats == null || reportFormats.isEmpty()) {
      reportFormats = List.of(OutputFormat.MARKDOWN, OutputFormat.HTML);
    }
    getLog().info("Report formats: " + reportFormats);

    if (!Files.isDirectory(bookDirectory)) {
      getLog().warn("Book directory does not exist: " + bookDirectory);
      return;
    }
    if (!Files.isDirectory(executionDirectory)) {
      getLog().warn("Execution directory does not exist: " + executionDirectory);
      getLog().warn("Run 'mvn verify' first to generate execution JSON.");
      return;
    }

    try {
      if (reportFormats.contains(OutputFormat.MARKDOWN)) {
        new BookReportGenerator(bookDirectory, executionDirectory, outputDirectory).generate();
      }
      if (reportFormats.contains(OutputFormat.HTML)) {
        generateHtmlReport();
      }
      logGeneratedFiles();
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to generate Truedoctales report", e);
    }
  }

  private void generateHtmlReport() throws IOException {
    // HTML is generated from enriched markdown. If markdown was already produced,
    // use that; otherwise generate enriched markdown in a temporary location first.
    Path markdownSource;
    Path tempMarkdownDir = null;
    if (reportFormats.contains(OutputFormat.MARKDOWN)) {
      markdownSource = outputDirectory;
    } else {
      tempMarkdownDir = outputDirectory.resolveSibling("truedoctales-markdown-tmp");
      new BookReportGenerator(bookDirectory, executionDirectory, tempMarkdownDir).generate();
      markdownSource = tempMarkdownDir;
    }
    Path htmlOutput = outputDirectory.resolveSibling("truedoctales-html");
    // Pass the JSON execution directory so navigation is built from JSON, not re-parsed markdown.
    new HtmlBookReportGenerator(markdownSource, executionDirectory, htmlOutput).generate();

    // Clean up temporary markdown if it was created
    if (tempMarkdownDir != null && Files.isDirectory(tempMarkdownDir)) {
      try (Stream<Path> walk = Files.walk(tempMarkdownDir)) {
        walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
      }
    }
  }

  private void logGeneratedFiles() throws IOException {
    logFilesInDirectory(outputDirectory, "Markdown");
    Path htmlDir = outputDirectory.resolveSibling("truedoctales-html");
    logFilesInDirectory(htmlDir, "HTML");
  }

  private void logFilesInDirectory(Path directory, String label) throws IOException {
    if (!Files.isDirectory(directory)) {
      return;
    }
    long count = 0;
    try (Stream<Path> files = Files.walk(directory)) {
      for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
        getLog().info("  Generated: " + directory.relativize(file));
        count++;
      }
    }
    getLog()
        .info("Truedoctales " + label + " report: " + count + " file(s) written to " + directory);
  }
}
