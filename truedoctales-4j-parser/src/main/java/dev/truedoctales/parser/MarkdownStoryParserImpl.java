package dev.truedoctales.parser;

import dev.truedoctales.api.model.story.SceneModel;
import dev.truedoctales.api.model.story.StoryModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * Markdown parser for story files with table-driven test data.
 *
 * <p>Parses story-driven markdown using a stream-based, multi-phase approach:
 *
 * <ol>
 *   <li><strong>Header Phase</strong> - {@link HeaderParser} extracts title, summary, prequels
 *   <li><strong>Scene Phase</strong> - {@link SceneParser} processes scenes with steps
 *   <li><strong>Step Phase</strong> - {@link StepParser} handles executable tasks and descriptions
 * </ol>
 *
 * <h3>Supported Features</h3>
 *
 * <ul>
 *   <li>Blockquote step syntax: {@code > **PlotName** Step description}
 *   <li>Scene markers: {@code ## Scene: Title}
 *   <li>Prequel references: {@code @Prequel [label](path)}
 *   <li>Markdown tables with pipe-separated columns
 *   <li>Markdown descriptions between steps
 * </ul>
 *
 * <p><strong>Architecture:</strong> Parser is completely decoupled from test execution. Returns
 * structured metamodel that can be validated and executed independently.
 *
 * <p><strong>Modern Java 25 Features:</strong>
 *
 * <ul>
 *   <li>Try-with-resources for automatic resource management
 *   <li>Optional for null-safe value handling
 *   <li>Immutable lists with List.copyOf()
 * </ul>
 */
public final class MarkdownStoryParserImpl implements MarkdownStoryParser {

  private static final Logger LOGGER = Logger.getLogger(MarkdownStoryParserImpl.class.getName());
  private static final String SCENE_PREFIX = "##";
  private static final String STORY_MARKER = "## Story";
  private static final Pattern FILE_NUMBER_PATTERN = Pattern.compile("^(\\d+)_.*\\.md$");

  @Override
  public StoryModel parse(@NonNull Path rootDir, @NonNull Path storyPath) throws IOException {
    LOGGER.info(() -> "Parsing story: " + storyPath);

    try (var inputStream = Files.newInputStream(rootDir.resolve(storyPath));
        var reader = new BufferedReader(new InputStreamReader(inputStream))) {
      return parseStream(reader, storyPath);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to parse story file: " + storyPath, e);
      throw new IOException("Parse failed for " + storyPath, e);
    }
  }

  // ===== Private Implementation =====

  private StoryModel parseStream(BufferedReader reader, Path storyPath) throws IOException {
    // Phase 1: Parse header section
    var headerParser = new HeaderParser();
    String currentLine = parseHeaderSection(reader, headerParser);

    // Phase 2: Parse scenes
    List<SceneModel> scenes = parseScenesSection(reader, currentLine, headerParser.getLineNumber());

    // Build final story model
    String title = headerParser.getTitle().orElseGet(() -> deriveTitle(storyPath));

    LOGGER.info(() -> "Parsing complete: " + scenes.size() + " scene(s) in '" + title + "'");

    return new StoryModel(
        deriveNumber(storyPath),
        storyPath,
        title,
        headerParser.getPrequelPaths(),
        List.copyOf(scenes));
  }

  private String parseHeaderSection(BufferedReader reader, HeaderParser parser) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      if (!parser.parseLine(line)) {
        return line; // Header complete, return scene start line
      }
    }
    return null; // EOF reached
  }

  private List<SceneModel> parseScenesSection(
      BufferedReader reader, String firstLine, int headerLineNumber) throws IOException {

    List<SceneModel> scenes = new ArrayList<>();
    SceneParser currentScene = null;

    // Check if first line is a scene marker
    if (firstLine != null && isSceneHeader(firstLine.trim())) {
      currentScene = new SceneParser(extractSceneTitle(firstLine), headerLineNumber + 1);
    }

    // Parse remaining lines
    String line;
    while ((line = reader.readLine()) != null) {
      if (currentScene == null) {
        // Still looking for first scene
        if (isSceneHeader(line.trim())) {
          currentScene = new SceneParser(extractSceneTitle(line), headerLineNumber + 1);
        }
        continue;
      }

      if (!currentScene.parseLine(line)) {
        // Current scene complete, line starts next scene
        scenes.add(currentScene.build());
        currentScene = new SceneParser(extractSceneTitle(line), currentScene.getLineNumber());
      }
    }

    // Finalize last scene
    if (currentScene != null) {
      scenes.add(currentScene.build());
    }

    return scenes;
  }

  private String extractSceneTitle(String line) {
    String trimmed = line.trim();
    if (!isSceneHeader(trimmed)) {
      return "";
    }
    String titlePart = trimmed.substring(SCENE_PREFIX.length()).trim();
    if (titlePart.toLowerCase().startsWith("scene:")) {
      titlePart = titlePart.substring("scene:".length()).trim();
    }
    return titlePart.isEmpty() ? "Untitled Scene" : titlePart;
  }

  private boolean isSceneHeader(String trimmedLine) {
    return trimmedLine.startsWith(SCENE_PREFIX) && !trimmedLine.equalsIgnoreCase(STORY_MARKER);
  }

  private static String deriveTitle(Path storyPath) {
    return storyPath.getFileName().toString();
  }

  private static Integer deriveNumber(Path storyPath) {
    Path filename = storyPath.getFileName();
    var matcher = FILE_NUMBER_PATTERN.matcher(filename.toString());
    if (matcher.matches()) {
      return Integer.parseInt(matcher.group(1));
    }
    throw new IllegalArgumentException(
        "Filename must start with a number followed by an underscore (e.g., '01_story.md'): "
            + filename);
  }
}
