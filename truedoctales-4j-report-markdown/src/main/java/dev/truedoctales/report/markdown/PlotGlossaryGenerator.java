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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Generates a plot glossary from a {@code plot-registry.json} produced by the
/// {@code JsonStoryListener}.
///
/// <p>Produces:
/// <ul>
///   <li>{@code plot-glossary.md} — index listing all plots with links to their detail pages
///   <li>{@code plots/<PlotId>.md} — one detail page per plot with an H2 section per step,
///       its inplaceVariables, and a usage example
/// </ul>
public class PlotGlossaryGenerator {

  private static final Logger logger = Logger.getLogger(PlotGlossaryGenerator.class.getName());

  static final String PLOT_REGISTRY_FILE = "plot-registry.json";
  static final String PLOT_GLOSSARY_FILE = "plot-glossary.md";
  static final String PLOTS_DIR = "plots";

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

  private final Path executionDirectory;
  private final Path outputDirectory;
  private final ObjectMapper objectMapper;

  /// Creates a new plot glossary generator.
  ///
  /// @param executionDirectory directory containing {@code plot-registry.json}
  /// @param outputDirectory directory where glossary files will be written
  public PlotGlossaryGenerator(Path executionDirectory, Path outputDirectory) {
    this.executionDirectory = executionDirectory;
    this.outputDirectory = outputDirectory;
    this.objectMapper = createObjectMapper();
  }

  /// Generates the plot glossary index and one page per plot.
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

    List<JsonNode> plots = sortedPlots(plotsNode);

    Files.createDirectories(outputDirectory);
    Path plotsDir = Files.createDirectories(outputDirectory.resolve(PLOTS_DIR));

    // Write one page per plot
    for (JsonNode plot : plots) {
      String plotId = plot.path("plotId").asText("(unknown)");
      String plotPageMarkdown = buildPlotPageMarkdown(plot);
      Path plotFile = plotsDir.resolve(plotId + ".md");
      Files.writeString(plotFile, plotPageMarkdown);
      logger.info("Plot page written to: " + plotFile);
    }

    // Write index
    String indexMarkdown = buildIndexMarkdown(plots);
    Path glossaryFile = outputDirectory.resolve(PLOT_GLOSSARY_FILE);
    Files.writeString(glossaryFile, indexMarkdown);
    logger.info("Plot glossary index written to: " + glossaryFile);
  }

  private List<JsonNode> sortedPlots(JsonNode plotsNode) {
    List<JsonNode> plots = new ArrayList<>();
    plotsNode.forEach(plots::add);
    plots.sort(
        (a, b) -> a.path("plotId").asText("").compareToIgnoreCase(b.path("plotId").asText("")));
    return plots;
  }

  /// Builds the index page listing all plots with links to their detail pages.
  private String buildIndexMarkdown(List<JsonNode> plots) {
    StringBuilder md = new StringBuilder();
    md.append("# Plot Glossary\n\n");
    md.append("A reference of all available plots and their steps.\n\n");
    md.append("| Plot | Steps |\n");
    md.append("|------|-------|\n");
    for (JsonNode plot : plots) {
      String plotId = plot.path("plotId").asText("(unknown)");
      JsonNode steps = plot.get("steps");
      int stepCount = (steps != null && steps.isArray()) ? steps.size() : 0;
      md.append("| [")
          .append(plotId)
          .append("](plots/")
          .append(plotId.replace(" ", "%20"))
          .append(".md) | ")
          .append(stepCount)
          .append(" step")
          .append(stepCount == 1 ? "" : "s")
          .append(" |\n");
    }
    return md.toString();
  }

  /// Builds the detail page for a single plot.
  private String buildPlotPageMarkdown(JsonNode plot) {
    String plotId = plot.path("plotId").asText("(unknown)");
    StringBuilder md = new StringBuilder();
    md.append("# ").append(plotId).append("\n\n");
    md.append("> Plot binding reference for the **").append(plotId).append("** plot.\n");

    JsonNode steps = plot.get("steps");
    if (steps == null || !steps.isArray() || !steps.iterator().hasNext()) {
      md.append("\n_No steps registered._\n");
      return md.toString();
    }

    steps.forEach(step -> appendStepSection(md, plotId, step));
    return md.toString();
  }

  /// A single variable/header entry extracted from JSON.
  record VarEntry(String name, String type, String description) {}

  /// Appends a single step section (H2 + description + inplaceVariables/tableVariables + usage
  // example) to the
  /// builder.
  private void appendStepSection(StringBuilder md, String plotId, JsonNode step) {
    String pattern = step.path("pattern").asText("");
    String inputType = step.path("inputType").asText("SEQUENCE");
    String description = step.path("description").asText("");
    List<VarEntry> variables = extractVariableBindings(step);
    List<VarEntry> headers = extractHeaderBindings(step);

    md.append("\n## ").append(pattern).append("\n\n");

    if (!description.isBlank()) {
      md.append(description).append("\n\n");
    }

    md.append("- **Pattern:** `").append(pattern).append("`\n");
    md.append("- **Input type:** ").append(inputType).append("\n");

    // Variables section
    if (!variables.isEmpty()) {
      md.append("\n### Variables\n\n");
      md.append("| Variable | Type | Description |\n");
      md.append("|----------|------|-------------|\n");
      for (VarEntry v : variables) {
        md.append("| `").append(v.name()).append("` | `").append(v.type()).append("` | ");
        md.append(v.description().isEmpty() ? "–" : v.description()).append(" |\n");
      }
    }

    // Headers section (table columns for @Table parameters)
    if (!headers.isEmpty()) {
      md.append("\n### Headers\n\n");
      md.append("| Header | Type | Description |\n");
      md.append("|--------|------|-------------|\n");
      for (VarEntry h : headers) {
        md.append("| `").append(h.name()).append("` | `").append(h.type()).append("` | ");
        md.append(h.description().isEmpty() ? "–" : h.description()).append(" |\n");
      }
    }

    // Determine effective columns for usage example
    List<String> columns = new ArrayList<>();
    variables.forEach(v -> columns.add(v.name()));
    headers.forEach(h -> columns.add(h.name()));

    md.append("\n### Usage Example\n\n");
    if (columns.isEmpty()) {
      md.append("```\n> **").append(plotId).append("** ").append(pattern).append("\n```\n");
    } else {
      // Show table invocation format (same for SEQUENCE and BATCH)
      md.append("```\n> **").append(plotId).append("** ").append(pattern).append("\n");
      md.append("> |");
      columns.forEach(v -> md.append(" ").append(v).append(" |"));
      md.append("\n> |");
      columns.forEach(v -> md.append(" ---- |"));
      md.append("\n> |");
      columns.forEach(v -> md.append(" value |"));
      md.append("\n```\n");
    }

    md.append("\n---\n");
  }

  /// Extracts variable bindings from the step JSON node's {@code inplaceVariables} array.
  ///
  /// Supports both the new object format ({@code {"name", "type", "description"}}) and the
  /// legacy flat string format for backward compatibility.
  static List<VarEntry> extractVariableBindings(JsonNode step) {
    return extractBindingArray(step, "inplaceVariables");
  }

  /// Extracts header bindings from the step JSON node's {@code tableVariables} array.
  ///
  /// Supports both the new object format ({@code {"name", "type", "description"}}) and the
  /// legacy flat string format for backward compatibility.
  static List<VarEntry> extractHeaderBindings(JsonNode step) {
    return extractBindingArray(step, "tableVariables");
  }

  private static List<VarEntry> extractBindingArray(JsonNode step, String fieldName) {
    List<VarEntry> entries = new ArrayList<>();
    JsonNode node = step.get(fieldName);
    if (node != null && node.isArray()) {
      node.forEach(
          item -> {
            if (item.isObject()) {
              entries.add(
                  new VarEntry(
                      item.path("name").asText(""),
                      item.path("type").asText("String"),
                      item.path("description").asText("")));
            } else {
              // Legacy flat string format
              entries.add(new VarEntry(item.asText(), "String", ""));
            }
          });
    }
    return entries;
  }

  /// Extracts variable names from a step pattern, e.g. {@code ${name}} → {@code name}.
  static List<String> extractVariables(String pattern) {
    List<String> vars = new ArrayList<>();
    Matcher m = VARIABLE_PATTERN.matcher(pattern);
    while (m.find()) {
      vars.add(m.group(1));
    }
    return vars;
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
