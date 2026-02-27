package dev.truedoctales.maven;

import dev.truedoctales.report.markdown.BookReportGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
/// <p>Intended usage: run {@code mvn test} (or {@code mvn verify}) first so that
/// {@code JsonStoryListener} writes execution JSON, then run {@code mvn site} which triggers
/// this goal in the {@code pre-site} phase. Output is written to
/// {@code ${project.build.directory}/site/truedoctales} following Maven site conventions.
@Mojo(name = "report", defaultPhase = LifecyclePhase.PRE_SITE)
public class ReportMojo extends AbstractMojo {

  /// Path to the original book directory containing the markdown stories.
  @Parameter(
      defaultValue = "${project.basedir}/../fairy-doc-tales",
      property = "truedoctales.bookDirectory")
  private Path bookDirectory;

  /// Directory that contains the JSON execution results produced by {@code JsonStoryListener}.
  @Parameter(
      defaultValue = "${project.build.directory}/book-of-truth/.execution",
      property = "truedoctales.executionDirectory")
  private Path executionDirectory;

  /// Directory where the enriched book copy will be written.
  @Parameter(
      defaultValue = "${project.build.directory}/site/truedoctales",
      property = "truedoctales.outputDirectory")
  private Path outputDirectory;

  @Override
  public void execute() throws MojoExecutionException {
    if (!Files.isDirectory(bookDirectory)) {
      getLog().warn("Book directory does not exist: " + bookDirectory);
      return;
    }
    if (!Files.isDirectory(executionDirectory)) {
      getLog().warn("Execution directory does not exist: " + executionDirectory);
      return;
    }

    try {
      new BookReportGenerator(bookDirectory, executionDirectory, outputDirectory).generate();
      getLog().info("Truedoctales enriched report written to " + outputDirectory);
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to generate Truedoctales report", e);
    }
  }
}
