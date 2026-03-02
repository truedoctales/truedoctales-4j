package dev.truedoctales.report.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlotGlossaryGeneratorTest {

  @TempDir Path tempDir;

  private Path executionDir;
  private Path outputDir;

  @BeforeEach
  void setUp() throws IOException {
    executionDir = tempDir.resolve("execution");
    outputDir = tempDir.resolve("output");
    Files.createDirectories(executionDir);
    Files.createDirectories(outputDir);
  }

  @Test
  void generate_shouldSkipWhenNoPlotRegistryJson() throws IOException {
    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    assertFalse(
        Files.exists(outputDir.resolve("plot-glossary.md")),
        "Should not create glossary when plot-registry.json is absent");
  }

  @Test
  void generate_shouldCreateGlossaryFromPlotRegistry() throws IOException {
    String plotRegistry =
        """
        {
          "plots": [
            {
              "plotId": "Hero",
              "steps": [
                { "plot": "Hero", "pattern": "Create hero", "inputType": "SEQUENCE" },
                { "plot": "Hero", "pattern": "Hero exists", "inputType": "SEQUENCE" }
              ]
            }
          ]
        }
        """;
    Files.writeString(executionDir.resolve("plot-registry.json"), plotRegistry);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    Path glossaryFile = outputDir.resolve("plot-glossary.md");
    assertTrue(Files.exists(glossaryFile), "Should create plot-glossary.md");

    String content = Files.readString(glossaryFile);
    assertTrue(content.contains("# Plot Glossary"), "Should have title");
    assertTrue(content.contains("## Hero"), "Should contain plot heading");
    assertTrue(content.contains("Create hero"), "Should list step pattern");
    assertTrue(content.contains("Hero exists"), "Should list step pattern");
    assertTrue(content.contains("SEQUENCE"), "Should include inputType");
  }

  @Test
  void generate_shouldSortPlotsByName() throws IOException {
    String plotRegistry =
        """
        {
          "plots": [
            { "plotId": "Zorro",  "steps": [{ "plot": "Zorro",  "pattern": "zorro step",  "inputType": "SEQUENCE" }] },
            { "plotId": "Alpha",  "steps": [{ "plot": "Alpha",  "pattern": "alpha step",  "inputType": "SEQUENCE" }] }
          ]
        }
        """;
    Files.writeString(executionDir.resolve("plot-registry.json"), plotRegistry);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    String content = Files.readString(outputDir.resolve("plot-glossary.md"));
    int alphaPos = content.indexOf("## Alpha");
    int zorroPos = content.indexOf("## Zorro");
    assertTrue(alphaPos < zorroPos, "Plots should be sorted alphabetically");
  }

  @Test
  void generate_shouldHandleEmptyStepsList() throws IOException {
    String plotRegistry =
        """
        {
          "plots": [
            { "plotId": "EmptyPlot", "steps": [] }
          ]
        }
        """;
    Files.writeString(executionDir.resolve("plot-registry.json"), plotRegistry);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    String content = Files.readString(outputDir.resolve("plot-glossary.md"));
    assertTrue(content.contains("## EmptyPlot"), "Should list the plot heading");
    assertTrue(content.contains("_No steps registered._"), "Should indicate no steps");
  }

  @Test
  void generate_shouldHandleMultiplePlots() throws IOException {
    String plotRegistry =
        """
        {
          "plots": [
            {
              "plotId": "Fight",
              "steps": [
                { "plot": "Fight", "pattern": "Attack fails", "inputType": "SEQUENCE" },
                { "plot": "Fight", "pattern": "Defeat with skill", "inputType": "SEQUENCE" }
              ]
            },
            {
              "plotId": "Hero",
              "steps": [
                { "plot": "Hero", "pattern": "Create hero", "inputType": "SEQUENCE" },
                { "plot": "Hero", "pattern": "Grant skill", "inputType": "BATCH" }
              ]
            }
          ]
        }
        """;
    Files.writeString(executionDir.resolve("plot-registry.json"), plotRegistry);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    String content = Files.readString(outputDir.resolve("plot-glossary.md"));
    assertTrue(content.contains("## Fight"), "Should contain Fight plot");
    assertTrue(content.contains("## Hero"), "Should contain Hero plot");
    assertTrue(content.contains("Attack fails"), "Should list Fight step");
    assertTrue(content.contains("Grant skill"), "Should list Hero step");
    assertTrue(content.contains("BATCH"), "Should include BATCH inputType");
  }

  @Test
  void generate_withBookReportGenerator_shouldCreateGlossaryInOutput() throws IOException {
    Path bookDir = tempDir.resolve("book");
    Files.createDirectories(bookDir);
    Files.writeString(bookDir.resolve("00_intro.md"), "# Book Introduction\n");

    String plotRegistry =
        """
        {
          "plots": [
            {
              "plotId": "Hero",
              "steps": [{ "plot": "Hero", "pattern": "Create hero", "inputType": "SEQUENCE" }]
            }
          ]
        }
        """;
    Files.writeString(executionDir.resolve("plot-registry.json"), plotRegistry);

    new BookReportGenerator(bookDir, executionDir, outputDir).generate();

    Path glossaryFile = outputDir.resolve("plot-glossary.md");
    assertTrue(Files.exists(glossaryFile), "BookReportGenerator should generate plot-glossary.md");
    assertTrue(Files.readString(glossaryFile).contains("## Hero"));
  }
}
