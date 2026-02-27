package dev.truedoctales.maven;

import dev.truedoctales.report.markdown.BookReportGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/// Maven goal that generates an enriched book report from True Doc Tales execution results.
///
/// Delegates to {@link BookReportGenerator} from the {@code truedoctales-report-markdown} module.
/// The generator copies the original book directory and enriches story markdown files with
/// per-step pass/fail status badges produced by test execution.
///
/// <p>Intended usage: bind this goal to the {@code verify} phase so that {@code mvn clean verify}
/// both runs the tests and generates the enriched markdown report.  Output is written to
/// {@code ${project.build.directory}/truedoctales-markdown}.
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

  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Book directory: " + bookDirectory);
    getLog().info("Execution directory: " + executionDirectory);
    getLog().info("Output directory: " + outputDirectory);

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
      new BookReportGenerator(bookDirectory, executionDirectory, outputDirectory).generate();
      logGeneratedFiles();
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to generate Truedoctales report", e);
    }
  }

  private void logGeneratedFiles() throws IOException {
    if (!Files.isDirectory(outputDirectory)) {
      getLog().warn("Output directory was not created: " + outputDirectory);
      return;
    }
    long count = 0;
    try (Stream<Path> files = Files.walk(outputDirectory)) {
      for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
        getLog().info("  Generated: " + outputDirectory.relativize(file));
        count++;
      }
    }
    getLog().info("Truedoctales report: " + count + " file(s) written to " + outputDirectory);
  }
}
