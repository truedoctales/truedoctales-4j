package dev.truedoctales.report.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
  void generate_shouldCreateIndexAndPerPlotPages() throws IOException {
    Files.writeString(
        executionDir.resolve("plot-registry.json"),
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
        """);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    // Index page
    assertTrue(Files.exists(outputDir.resolve("plot-glossary.md")), "Should create index");
    String index = Files.readString(outputDir.resolve("plot-glossary.md"));
    assertTrue(index.contains("# Plot Glossary"), "Index should have title");
    assertTrue(index.contains("[Hero](plots/Hero.md)"), "Index should link to plot page");

    // Per-plot page
    Path plotPage = outputDir.resolve("plots/Hero.md");
    assertTrue(Files.exists(plotPage), "Should create per-plot page");
    String content = Files.readString(plotPage);
    assertTrue(content.contains("# Hero"), "Plot page should have title");
    assertTrue(content.contains("## Create hero"), "Step should be H2");
    assertTrue(content.contains("## Hero exists"), "Step should be H2");
  }

  @Test
  void generate_perPlotPage_shouldShowVariablesSection() throws IOException {
    Files.writeString(
        executionDir.resolve("plot-registry.json"),
        """
        {
          "plots": [
            {
              "plotId": "Hero",
              "steps": [
                { "plot": "Hero", "pattern": "Create hero ${id} ${name}", "inputType": "SEQUENCE" }
              ]
            }
          ]
        }
        """);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    String content = Files.readString(outputDir.resolve("plots/Hero.md"));
    assertTrue(content.contains("### Variables"), "Should have Variables section");
    assertTrue(content.contains("`id`"), "Should list variable id");
    assertTrue(content.contains("`name`"), "Should list variable name");
    assertTrue(content.contains("### Usage Example"), "Should have Usage Example section");
  }

  @Test
  void generate_perPlotPage_shouldShowUsageExampleWithoutVariables() throws IOException {
    Files.writeString(
        executionDir.resolve("plot-registry.json"),
        """
        {
          "plots": [
            {
              "plotId": "Greeting",
              "steps": [
                { "plot": "Greeting", "pattern": "Say Hello", "inputType": "SEQUENCE" }
              ]
            }
          ]
        }
        """);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    String content = Files.readString(outputDir.resolve("plots/Greeting.md"));
    assertTrue(content.contains("### Usage Example"), "Should have usage example");
    assertTrue(content.contains("> **Greeting** Say Hello"), "Usage should reference the plot");
    assertTrue(
        content.contains("```\n> **Greeting** Say Hello\n```"),
        "Usage should be wrapped in code block");
    assertFalse(content.contains("### Variables"), "No variables section when pattern has none");
  }

  @Test
  void generate_shouldSortPlotsByNameInIndex() throws IOException {
    Files.writeString(
        executionDir.resolve("plot-registry.json"),
        """
        {
          "plots": [
            { "plotId": "Zorro",  "steps": [{ "plot": "Zorro",  "pattern": "z step", "inputType": "SEQUENCE" }] },
            { "plotId": "Alpha",  "steps": [{ "plot": "Alpha",  "pattern": "a step", "inputType": "SEQUENCE" }] }
          ]
        }
        """);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    String index = Files.readString(outputDir.resolve("plot-glossary.md"));
    assertTrue(index.indexOf("Alpha") < index.indexOf("Zorro"), "Index should sort alphabetically");
  }

  @Test
  void generate_shouldHandleEmptyStepsList() throws IOException {
    Files.writeString(
        executionDir.resolve("plot-registry.json"),
        """
        { "plots": [ { "plotId": "Empty", "steps": [] } ] }
        """);

    new PlotGlossaryGenerator(executionDir, outputDir).generate();

    String content = Files.readString(outputDir.resolve("plots/Empty.md"));
    assertTrue(content.contains("# Empty"), "Should still create plot page");
    assertTrue(content.contains("_No steps registered._"), "Should indicate no steps");
  }

  @Test
  void extractVariables_shouldReturnEmptyListForPatternWithNoVars() {
    assertEquals(List.of(), PlotGlossaryGenerator.extractVariables("Create hero"));
  }

  @Test
  void extractVariables_shouldExtractAllVariableNames() {
    List<String> vars = PlotGlossaryGenerator.extractVariables("Create hero ${id} ${name} ${age}");
    assertEquals(List.of("id", "name", "age"), vars);
  }

  @Test
  void generate_withBookReportGenerator_shouldCreateGlossaryInOutput() throws IOException {
    Path bookDir = tempDir.resolve("book");
    Files.createDirectories(bookDir);
    Files.writeString(bookDir.resolve("00_intro.md"), "# Book Introduction\n");

    Files.writeString(
        executionDir.resolve("plot-registry.json"),
        """
        {
          "plots": [
            {
              "plotId": "Hero",
              "steps": [{ "plot": "Hero", "pattern": "Create hero", "inputType": "SEQUENCE" }]
            }
          ]
        }
        """);

    new BookReportGenerator(bookDir, executionDir, outputDir).generate();

    assertTrue(Files.exists(outputDir.resolve("plot-glossary.md")));
    assertTrue(Files.exists(outputDir.resolve("plots/Hero.md")));
    assertTrue(Files.readString(outputDir.resolve("plots/Hero.md")).contains("## Create hero"));
  }
}
