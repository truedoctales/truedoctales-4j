package dev.truedoctales.report.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutputFormatTest {

  @TempDir Path tempDir;

  private Path bookDir;
  private Path executionDir;
  private Path outputDir;

  @BeforeEach
  void setUp() throws IOException {
    bookDir = tempDir.resolve("book");
    executionDir = tempDir.resolve("execution");
    outputDir = tempDir.resolve("output");
    Files.createDirectories(bookDir);
    Files.createDirectories(executionDir);
  }

  @Test
  void generate_markdownOnly_shouldNotCreateHtmlDirectory() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Intro\n\nWelcome.");

    BookReportGenerator generator =
        new BookReportGenerator(bookDir, executionDir, outputDir, List.of(OutputFormat.MARKDOWN));
    generator.generate();

    assertTrue(Files.exists(outputDir.resolve("00_intro.md")), "Markdown should be generated");
    assertFalse(
        Files.isDirectory(outputDir.resolveSibling("truedoctales-html")),
        "HTML directory should not be created");
  }

  @Test
  void generate_htmlOnly_shouldCreateHtmlDirectory() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Intro\n\nWelcome.");

    BookReportGenerator generator =
        new BookReportGenerator(bookDir, executionDir, outputDir, List.of(OutputFormat.HTML));
    generator.generate();

    Path htmlDir = outputDir.resolveSibling("truedoctales-html");
    assertTrue(Files.isDirectory(htmlDir), "HTML directory should be created");
    assertTrue(Files.exists(htmlDir.resolve("00_intro.html")), "HTML file should be generated");
  }

  @Test
  void generate_both_shouldCreateMarkdownAndHtml() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Intro\n\nWelcome.");

    BookReportGenerator generator =
        new BookReportGenerator(
            bookDir, executionDir, outputDir, List.of(OutputFormat.MARKDOWN, OutputFormat.HTML));
    generator.generate();

    assertTrue(Files.exists(outputDir.resolve("00_intro.md")), "Markdown should be generated");
    Path htmlDir = outputDir.resolveSibling("truedoctales-html");
    assertTrue(Files.isDirectory(htmlDir), "HTML directory should be created");
    assertTrue(Files.exists(htmlDir.resolve("00_intro.html")), "HTML file should be generated");
  }

  @Test
  void generate_defaultConstructor_shouldProduceMarkdownOnly() throws IOException {
    Files.writeString(bookDir.resolve("00_intro.md"), "# Intro\n\nWelcome.");

    BookReportGenerator generator = new BookReportGenerator(bookDir, executionDir, outputDir);
    generator.generate();

    assertTrue(Files.exists(outputDir.resolve("00_intro.md")), "Markdown should be generated");
    assertFalse(
        Files.isDirectory(outputDir.resolveSibling("truedoctales-html")),
        "HTML directory should not be created by default");
  }
}
