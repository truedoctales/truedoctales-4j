package dev.truedoctales.execution.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import dev.truedoctales.api.annotations.Scene;
import dev.truedoctales.api.annotations.Story;
import dev.truedoctales.api.model.execution.*;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.api.model.plot.StepBinding;
import dev.truedoctales.api.model.story.StepCall;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.*;

/// JUnit 5 extension that captures test execution for @Story annotated test classes.
///
/// <p>This extension automatically generates JSON files for code-based stories, enabling them to
/// be included in the Book of Truth documentation alongside markdown-based stories.
///
/// <h3>Usage:</h3>
///
/// <pre>{@code
/// @Story(
///     book = "Book of Stories",
///     chapter = "My Chapter",
///     name = "My Story",
///     summary = "A code-based story")
/// @ExtendWith(StoryExtension.class)
/// public class MyStoryTest {
///
///   @Scene(title = "First Scene", description = "Description in markdown")
///   @Test
///   void firstScene() {
///     // test code
///   }
/// }
/// }</pre>
///
/// <p>The extension will:
///
/// <ul>
///   <li>Detect classes annotated with @Story
///   <li>Capture test methods annotated with @Scene
///   <li>Build a StoryExecutionResult during test execution
/// </ul>
public class StoryExtension
    implements BeforeAllCallback,
        AfterAllCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        InvocationInterceptor,
        TestWatcher {

  private static final String EXECUTION_NAMESPACE = "storyExecution";
  private static final String CURRENT_SCENE_NAMESPACE = "currentScene";
  private static final String PARAMETER_TABLE_NAMESPACE = "parameterTable";
  private static final String CURRENT_PARAMETERS_NAMESPACE = "currentParameters";
  private static final String SCENE_ADDED_NAMESPACE = "sceneAdded";
  private static final String SCENE_DESCRIPTIONS_NAMESPACE = "sceneDescriptions";
  private static final String CODE_STEP_TYPE = "Code";

  private final ObjectMapper objectMapper;
  private final Path outputDirectory;

  /// Creates a new StoryExtension with default configuration.
  public StoryExtension() {
    this.objectMapper = createObjectMapper();
    this.outputDirectory = Paths.get("target/truedoctales-report");
  }

  /// Creates a new StoryExtension with custom output directory.
  ///
  /// @param outputDirectory the directory where JSON files will be written
  public StoryExtension(Path outputDirectory) {
    this.objectMapper = createObjectMapper();
    this.outputDirectory = outputDirectory;
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.setVisibility(
        mapper
            .getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(
                com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE));
    return mapper;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    Story storyAnnotation = testClass.getAnnotation(Story.class);

    if (storyAnnotation == null) {
      // Not a story test, skip
      return;
    }

    StoryExecutionResult executionResult = new StoryExecutionResult();
    executionResult.setNumber(parseNumber(storyAnnotation.storyPath()));
    executionResult.setPath(storyAnnotation.storyPath());
    executionResult.setTitle(storyAnnotation.title());

    // Store in context
    getStore(context).put(EXECUTION_NAMESPACE, executionResult);
    getStore(context).put(SCENE_DESCRIPTIONS_NAMESPACE, new LinkedHashMap<>());

    System.out.println("StoryExtension: Initialized for story: " + storyAnnotation.storyPath());
  }

  private Integer parseNumber(String s) {
    Path path = Paths.get(s);
    String fileName = path.getFileName().toString();
    var pattern = java.util.regex.Pattern.compile("^(\\d+)_.+").matcher(fileName);
    if (pattern.find()) {
      return Integer.parseInt(pattern.group(1));
    }
    throw new IllegalArgumentException("Invalid story path format, expected 'NNN_name.md': " + s);
  }

  @Override
  public void interceptTestMethod(
      InvocationInterceptor.@NonNull Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      @NonNull ExtensionContext extensionContext)
      throws Throwable {
    captureParameters(invocationContext, extensionContext);
    invocation.proceed();
  }

  @Override
  public void interceptTestTemplateMethod(
      InvocationInterceptor.@NonNull Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      @NonNull ExtensionContext extensionContext)
      throws Throwable {
    captureParameters(invocationContext, extensionContext);
    invocation.proceed();
  }

  private void captureParameters(
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
    List<Object> arguments = invocationContext.getArguments();
    Method method = invocationContext.getExecutable();
    Parameter[] parameters = method.getParameters();

    Map<String, String> paramMap = new LinkedHashMap<>();
    for (int i = 0; i < parameters.length && i < arguments.size(); i++) {
      String paramName = parameters[i].getName();
      Object paramValue = arguments.get(i);
      paramMap.put(paramName, paramValue != null ? paramValue.toString() : "null");
    }

    getStore(extensionContext).put(CURRENT_PARAMETERS_NAMESPACE, paramMap);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    Story storyAnnotation = context.getRequiredTestClass().getAnnotation(Story.class);
    if (storyAnnotation == null) {
      return;
    }

    Scene sceneAnnotation = context.getRequiredTestMethod().getAnnotation(Scene.class);
    if (sceneAnnotation == null) {
      return;
    }

    // Store scene description for markdown generation
    ExtensionContext classContext = context.getParent().orElse(context);
    @SuppressWarnings("unchecked")
    Map<String, String> descriptions =
        getStore(classContext).get(SCENE_DESCRIPTIONS_NAMESPACE, Map.class);
    if (descriptions != null
        && !sceneAnnotation.description().isBlank()
        && !descriptions.containsKey(sceneAnnotation.title())) {
      descriptions.put(sceneAnnotation.title(), sceneAnnotation.description());
    }

    // Create scene execution (will be reused for all parameter invocations)
    SceneExecution sceneExecution =
        new SceneExecution(
            sceneAnnotation.title(),
            null, // Scene number will be auto-assigned
            new ArrayList<>() // Code-based stories don't have explicit steps
            );

    getStore(context).put(CURRENT_SCENE_NAMESPACE, sceneExecution);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    Story storyAnnotation = context.getRequiredTestClass().getAnnotation(Story.class);
    if (storyAnnotation == null) {
      return;
    }

    Scene sceneAnnotation = context.getRequiredTestMethod().getAnnotation(Scene.class);
    if (sceneAnnotation == null) {
      return;
    }

    // For parameterized tests, use parent context to share state across invocations
    ExtensionContext templateContext = context.getParent().orElse(context);
    ExtensionContext.Store templateStore = getStore(templateContext);
    ExtensionContext.Store contextStore = getStore(context);

    // Get or create parameter table for this test method
    String methodKey = PARAMETER_TABLE_NAMESPACE + ":" + context.getRequiredTestMethod().getName();
    @SuppressWarnings("unchecked")
    List<Map<String, String>> parameterTable = templateStore.get(methodKey, List.class);
    if (parameterTable == null) {
      parameterTable = new ArrayList<>();
      templateStore.put(methodKey, parameterTable);
    }

    // Add current invocation's parameters to the table
    String displayName = context.getDisplayName();
    @SuppressWarnings("unchecked")
    Map<String, String> currentParams = contextStore.get(CURRENT_PARAMETERS_NAMESPACE, Map.class);
    if (currentParams != null && !currentParams.isEmpty()) {
      parameterTable.add(new LinkedHashMap<>(currentParams)); // Copy to avoid mutation
      // Clear current parameters
      contextStore.remove(CURRENT_PARAMETERS_NAMESPACE);
    } else {
      // For parameterized tests without InvocationInterceptor working,
      // try to extract parameters from display name
      Map<String, String> extractedParams = extractParametersFromDisplayName(context, displayName);
      if (extractedParams != null && !extractedParams.isEmpty()) {
        parameterTable.add(extractedParams);
      }
    }

    SceneExecution sceneExecution = contextStore.get(CURRENT_SCENE_NAMESPACE, SceneExecution.class);
    if (sceneExecution == null) {
      return;
    }

    // For code-based stories, create a single "implicit" step representing the entire test method
    String methodName = context.getRequiredTestMethod().getName();

    StepBinding binding = buildBinding(context.getRequiredTestClass());
    StepCall call = new StepCall(CODE_STEP_TYPE, sceneAnnotation.title());

    // Use the accumulated parameter table as step data (will grow with each invocation)
    List<Map<String, String>> tableSnapshot = new ArrayList<>(parameterTable);
    StepExecution implicitStep = new StepExecution(binding, call, tableSnapshot, 0);

    StepExecutionResult stepResult = new StepExecutionResult(implicitStep);

    SceneExecutionResult sceneResult =
        new SceneExecutionResult(sceneExecution, List.of(stepResult));

    StoryExecutionResult storyResult =
        templateStore.get(EXECUTION_NAMESPACE, StoryExecutionResult.class);
    if (storyResult != null) {
      // For parameterized tests, replace the last scene result with updated version
      // or add if this is the first invocation
      String sceneIndexKey = SCENE_ADDED_NAMESPACE + ":" + methodName;
      Integer sceneIndex = templateStore.get(sceneIndexKey, Integer.class);
      if (sceneIndex != null) {
        // Replace existing scene with updated parameter table
        storyResult.getSceneResults().set(sceneIndex, sceneResult);
      } else {
        // First invocation - add the scene and remember its index
        storyResult.addSceneResult(sceneResult);
        templateStore.put(sceneIndexKey, storyResult.getSceneResults().size() - 1);
      }
    }
  }

  private StepBinding buildBinding(Class<?> requiredTestClass) {
    // For code-based stories, we can use the test method name as the pattern
    // and a fixed plot name like "Code"
    return new StepBinding(
        CODE_STEP_TYPE,
        requiredTestClass.getSimpleName(),
        InputType.SEQUENCE,
        "",
        List.of(),
        List.of());
  }

  /// Extracts parameters from JUnit's display name for parameterized tests.
  ///
  /// This is a fallback when InvocationInterceptor doesn't capture parameters.
  /// Parses display names like 'Golden hair #"1": "First"' to extract parameter values.
  private Map<String, String> extractParametersFromDisplayName(
      ExtensionContext context, String displayName) {
    // Check if this looks like a parameterized test invocation
    // JUnit creates unique IDs with [test-template-invocation:#N]
    String uniqueId = context.getUniqueId();
    if (!uniqueId.contains("test-template-invocation")) {
      return null;
    }

    // For our test with @ParameterizedTest(name = "Golden hair #{0}: {1}"),
    // the display name is: Golden hair #"1": "First"
    // We need to extract the quoted values

    Method method = context.getRequiredTestMethod();
    Parameter[] parameters = method.getParameters();

    Map<String, String> paramMap = new LinkedHashMap<>();

    // Extract quoted strings from display name
    // Pattern: "value" or #"value"
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]*)\"");
    java.util.regex.Matcher matcher = pattern.matcher(displayName);

    int paramIndex = 0;
    while (matcher.find() && paramIndex < parameters.length) {
      String value = matcher.group(1);
      String paramName = parameters[paramIndex].getName();
      paramMap.put(paramName, value);
      paramIndex++;
    }

    return paramMap.isEmpty() ? null : paramMap;
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    Story storyAnnotation = context.getRequiredTestClass().getAnnotation(Story.class);
    if (storyAnnotation == null) {
      return;
    }

    Scene sceneAnnotation = context.getRequiredTestMethod().getAnnotation(Scene.class);
    if (sceneAnnotation == null) {
      return;
    }

    SceneExecution sceneExecution =
        getStore(context).get(CURRENT_SCENE_NAMESPACE, SceneExecution.class);
    if (sceneExecution == null) {
      return;
    }

    // Create failed step
    StepBinding binding = buildBinding(context.getRequiredTestClass());
    StepCall call = new StepCall(CODE_STEP_TYPE, sceneAnnotation.title());
    StepExecution implicitStep = new StepExecution(binding, call, List.of(), 0);

    StepExecutionResult stepResult = new StepExecutionResult(implicitStep, cause);

    SceneExecutionResult sceneResult =
        new SceneExecutionResult(sceneExecution, List.of(stepResult));

    StoryExecutionResult storyResult =
        getStore(context).get(EXECUTION_NAMESPACE, StoryExecutionResult.class);
    if (storyResult != null) {
      storyResult.addSceneResult(sceneResult);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();
    Story storyAnnotation = testClass.getAnnotation(Story.class);

    if (storyAnnotation == null) {
      return;
    }

    StoryExecutionResult executionResult =
        getStore(context).get(EXECUTION_NAMESPACE, StoryExecutionResult.class);

    if (executionResult == null) {
      System.err.println(
          "StoryExtension: No execution result found for: " + storyAnnotation.title());
      return;
    }

    @SuppressWarnings("unchecked")
    Map<String, String> sceneDescriptions =
        getStore(context).get(SCENE_DESCRIPTIONS_NAMESPACE, Map.class);

    // Write JSON file and markdown file
    writeJsonFile(storyAnnotation, executionResult);
    writeMarkdownFile(storyAnnotation, executionResult, sceneDescriptions);
  }

  private void writeJsonFile(Story storyAnnotation, StoryExecutionResult executionResult)
      throws IOException {
    // Ensure output directory exists
    Files.createDirectories(outputDirectory);

    // Create filename
    Path storyPath = Path.of(storyAnnotation.storyPath());
    Path storyFile =
        storyPath.getParent().resolve(storyPath.getFileName().toString().replace(".md", ".json"));
    outputDirectory
        .resolve(storyPath.getParent())
        .toFile()
        .mkdirs(); // Ensure parent directories exist
    Path outputPath = outputDirectory.resolve(storyFile);

    // Write JSON
    objectMapper.writeValue(outputPath.toFile(), executionResult);

    System.out.println(
        "StoryExtension: JSON written to: "
            + outputPath.toAbsolutePath()
            + " ("
            + Files.size(outputPath)
            + " bytes)");
  }

  private void writeMarkdownFile(
      Story storyAnnotation,
      StoryExecutionResult executionResult,
      Map<String, String> sceneDescriptions)
      throws IOException {
    Path storyPath = Path.of(storyAnnotation.storyPath());
    Path markdownPath = outputDirectory.resolve(storyPath);
    Files.createDirectories(markdownPath.getParent());

    String markdown = generateMarkdown(storyAnnotation, executionResult, sceneDescriptions);
    Files.writeString(markdownPath, markdown);

    System.out.println(
        "StoryExtension: Markdown written to: "
            + markdownPath.toAbsolutePath()
            + " ("
            + Files.size(markdownPath)
            + " bytes)");
  }

  /// Generates markdown content from the story annotation and execution results.
  ///
  /// The generated markdown follows the same format as hand-written markdown stories,
  /// so the markdown-report module can process both identically.
  private String generateMarkdown(
      Story storyAnnotation,
      StoryExecutionResult executionResult,
      Map<String, String> sceneDescriptions) {
    StringBuilder md = new StringBuilder();

    // Story header from @Story.markdown
    String storyMarkdown = storyAnnotation.markdown().stripIndent().strip();
    if (!storyMarkdown.isEmpty()) {
      md.append(storyMarkdown);
    } else {
      md.append("# ").append(storyAnnotation.title());
    }
    md.append("\n");

    // Generate scene sections
    if (executionResult.getSceneResults() != null) {
      for (SceneExecutionResult scene : executionResult.getSceneResults()) {
        md.append("\n## Scene: ").append(scene.title()).append("\n");

        // Add scene description if available
        if (sceneDescriptions != null && sceneDescriptions.containsKey(scene.title())) {
          String description = sceneDescriptions.get(scene.title()).stripIndent().strip();
          if (!description.isEmpty()) {
            md.append("\n").append(description).append("\n");
          }
        }

        // Add step declaration line for each step in the scene
        if (scene.stepResults() != null) {
          for (StepExecutionResult step : scene.stepResults()) {
            md.append("\n> **")
                .append(step.plot())
                .append("** ")
                .append(scene.title())
                .append("\n");

            // Render parameter table if step has data rows
            if (step.stepData() != null && !step.stepData().isEmpty()) {
              appendParameterTable(md, step.stepData());
            }
          }
        }
      }
    }

    return md.toString();
  }

  /// Appends a markdown table (in blockquote format) for parameterized test data.
  private void appendParameterTable(StringBuilder md, List<Map<String, String>> stepData) {
    if (stepData.isEmpty()) {
      return;
    }
    // Get column headers from the first row
    List<String> headers = new ArrayList<>(stepData.getFirst().keySet());

    // Calculate column widths
    int[] widths = new int[headers.size()];
    for (int i = 0; i < headers.size(); i++) {
      widths[i] = headers.get(i).length();
    }
    for (Map<String, String> row : stepData) {
      int col = 0;
      for (String header : headers) {
        String value = row.getOrDefault(header, "");
        widths[col] = Math.max(widths[col], value.length());
        col++;
      }
    }

    // Header row
    md.append(">\n> |");
    for (int i = 0; i < headers.size(); i++) {
      md.append(" ").append(pad(headers.get(i), widths[i])).append(" |");
    }
    md.append("\n");

    // Separator row
    md.append("> |");
    for (int i = 0; i < headers.size(); i++) {
      md.append("-").append("-".repeat(widths[i])).append("-|");
    }
    md.append("\n");

    // Data rows
    for (Map<String, String> row : stepData) {
      md.append("> |");
      for (int i = 0; i < headers.size(); i++) {
        String value = row.getOrDefault(headers.get(i), "");
        md.append(" ").append(pad(value, widths[i])).append(" |");
      }
      md.append("\n");
    }
  }

  private static String pad(String value, int width) {
    if (value.length() >= width) {
      return value;
    }
    return value + " ".repeat(width - value.length());
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getStore(
        ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass()));
  }
}
