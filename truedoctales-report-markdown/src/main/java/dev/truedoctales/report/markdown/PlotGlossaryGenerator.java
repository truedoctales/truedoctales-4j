package dev.truedoctales.report.markdown;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/// Generates a {@code plot-glossary.md} file from a {@code plot-registry.json} produced by the
/// {@code JsonStoryListener}.
///
/// <p>The glossary lists every registered plot with its available steps, providing an
/// informational reference that complements the story-based test reports.
///
/// <h3>Example output</h3>
///
/// <pre>
/// # Plot Glossary
///
/// A reference of all available plots and their steps.
///
/// ## Hero
///
/// | Step | Input Type |
/// |------|-----------|
/// | Create hero | SEQUENCE |
/// | Hero exists | SEQUENCE |
/// </pre>
public class PlotGlossaryGenerator {

  private static final Logger logger = Logger.getLogger(PlotGlossaryGenerator.class.getName());

  static final String PLOT_REGISTRY_FILE = "plot-registry.json";
  static final String PLOT_GLOSSARY_FILE = "plot-glossary.md";

  private final Path executionDirectory;
  private final Path outputDirectory;
  private final ObjectMapper objectMapper;

  /// Creates a new plot glossary generator.
  ///
  /// @param executionDirectory directory containing {@code plot-registry.json}
  /// @param outputDirectory directory where {@code plot-glossary.md} will be written
  public PlotGlossaryGenerator(Path executionDirectory, Path outputDirectory) {
    this.executionDirectory = executionDirectory;
    this.outputDirectory = outputDirectory;
    this.objectMapper = createObjectMapper();
  }

  /// Generates {@code plot-glossary.md} if {@code plot-registry.json} exists.
  ///
  /// @throws IOException if reading or writing files fails
  public void generate() throws IOException {
    Path registryFile = executionDirectory.resolve(PLOT_REGISTRY_FILE);
    if (!Files.exists(registryFile)) {
      logger.info("No plot-registry.json found, skipping plot glossary generation");
      return;
    }

    JsonNode root = objectMapper.readTree(registryFile.toFile());
    JsonNode plotsNode = root.get("plots");
    if (plotsNode == null || !plotsNode.isArray()) {
      logger.warning("plot-registry.json has no 'plots' array, skipping glossary");
      return;
    }

    String markdown = buildMarkdown(plotsNode);
    Files.createDirectories(outputDirectory);
    Path glossaryFile = outputDirectory.resolve(PLOT_GLOSSARY_FILE);
    Files.writeString(glossaryFile, markdown);
    logger.info("Plot glossary written to: " + glossaryFile);
  }

  private String buildMarkdown(JsonNode plotsNode) {
    StringBuilder md = new StringBuilder();
    md.append("# Plot Glossary\n\n");
    md.append("A reference of all available plots and their steps.\n");

    List<JsonNode> plots = new ArrayList<>();
    plotsNode.forEach(plots::add);
    plots.sort(
        (a, b) -> {
          String aId = a.path("plotId").asText("");
          String bId = b.path("plotId").asText("");
          return aId.compareToIgnoreCase(bId);
        });

    for (JsonNode plot : plots) {
      String plotId = plot.path("plotId").asText("(unknown)");
      md.append("\n## ").append(plotId).append("\n\n");

      JsonNode steps = plot.get("steps");
      if (steps == null || !steps.isArray() || !steps.iterator().hasNext()) {
        md.append("_No steps registered._\n");
        continue;
      }

      md.append("| Step | Input Type |\n");
      md.append("|------|------------|\n");
      steps.forEach(
          step -> {
            String pattern = step.path("pattern").asText("");
            String inputType = step.path("inputType").asText("SEQUENCE");
            md.append("| ").append(pattern).append(" | ").append(inputType).append(" |\n");
          });
    }

    return md.toString();
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setVisibility(
        mapper
            .getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.ANY));
    return mapper;
  }
}
