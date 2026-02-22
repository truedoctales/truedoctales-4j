package dev.truedoctales.execution.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import dev.truedoctales.api.annotations.Scene;
import dev.truedoctales.api.annotations.Story;
import dev.truedoctales.api.model.execution.SceneExecution;
import dev.truedoctales.api.model.execution.StepBinding;
import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.execution.StoryExecution;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.listener.StoryExecutionResult;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StepCall;
import dev.truedoctales.report.json.JsonStoryReader;
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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestWatcher;

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
///   <li>Write JSON to target/book-of-truth/json/{Chapter}--{Story}.json
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
  private static final String CODE_STEP_TYPE = "Code";
  private static final int MAX_FILENAME_LENGTH = 100; // Limit to avoid filesystem issues

  private final ObjectMapper objectMapper;
  private final Path outputDirectory;

  /// Creates a new StoryExtension with default configuration.
  public StoryExtension() {
    this.objectMapper = createObjectMapper();
    this.outputDirectory = Paths.get("target/book-of-truth/json");
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
  public void beforeAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();
    Story storyAnnotation = testClass.getAnnotation(Story.class);

    if (storyAnnotation == null) {
      // Not a story test, skip
      return;
    }

    // Create story execution model
    StoryExecution storyExecution =
        new StoryExecution(
            Paths.get("code-based", testClass.getSimpleName() + ".java"),
            storyAnnotation.name(),
            List.of(), // No prequels for code-based stories
            new ArrayList<>() // Scenes will be added during test execution
            );

    StoryExecutionResult executionResult = new StoryExecutionResult(storyExecution);

    // Store in context
    getStore(context).put(EXECUTION_NAMESPACE, executionResult);

    System.out.println(
        "StoryExtension: Initialized for story: "
            + storyAnnotation.name()
            + " in chapter: "
            + storyAnnotation.chapter());
  }

  @Override
  public void interceptTestMethod(
      InvocationInterceptor.Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    // Capture method parameters before test execution
    List<Object> arguments = invocationContext.getArguments();
    Method method = invocationContext.getExecutable();
    Parameter[] parameters = method.getParameters();

    // Build parameter map
    Map<String, String> paramMap = new LinkedHashMap<>();
    for (int i = 0; i < parameters.length && i < arguments.size(); i++) {
      String paramName = parameters[i].getName();
      Object paramValue = arguments.get(i);
      paramMap.put(paramName, paramValue != null ? paramValue.toString() : "null");
    }

    // Store in context for use in afterTestExecution
    getStore(extensionContext).put(CURRENT_PARAMETERS_NAMESPACE, paramMap);

    // Proceed with test execution
    invocation.proceed();
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    Story storyAnnotation = context.getRequiredTestClass().getAnnotation(Story.class);
    if (storyAnnotation == null) {
      return;
    }

    Scene sceneAnnotation = context.getRequiredTestMethod().getAnnotation(Scene.class);
    if (sceneAnnotation == null) {
      return;
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
  public void afterTestExecution(ExtensionContext context) throws Exception {
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
    StepBinding binding = new StepBinding(CODE_STEP_TYPE, methodName);
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
        storyResult.sceneResults().set(sceneIndex, sceneResult);
      } else {
        // First invocation - add the scene and remember its index
        storyResult.addSceneResult(sceneResult);
        templateStore.put(sceneIndexKey, storyResult.sceneResults().size() - 1);
      }
    }
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
    String methodName = context.getRequiredTestMethod().getName();
    StepBinding binding = new StepBinding(CODE_STEP_TYPE, methodName);
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
          "StoryExtension: No execution result found for: " + storyAnnotation.name());
      return;
    }

    // Write JSON file
    writeJsonFile(storyAnnotation, executionResult);
  }

  private void writeJsonFile(Story storyAnnotation, StoryExecutionResult executionResult)
      throws IOException {
    // Ensure output directory exists
    Files.createDirectories(outputDirectory);

    // Create chapter model
    ChapterModel chapterModel =
        new ChapterModel(
            Paths.get("code-based", sanitizeFilename(storyAnnotation.chapter())),
            storyAnnotation.chapter(),
            List.of() // No stories in chapter model (this is for context only)
            );

    // Write chapter metadata if not already written
    writeChapterMetadata(storyAnnotation.book(), chapterModel);

    // Create wrapper with chapter context
    JsonStoryReader.StoryExecutionWrapper wrapper =
        new JsonStoryReader.StoryExecutionWrapper(
            storyAnnotation.book(), storyAnnotation.book(), chapterModel, executionResult);

    // Create filename
    String chapterName = sanitizeFilename(storyAnnotation.chapter());
    String storyName = sanitizeFilename(storyAnnotation.name());
    String filename = chapterName + "--" + storyName + ".json";
    Path outputPath = outputDirectory.resolve(filename);

    // Write JSON
    objectMapper.writeValue(outputPath.toFile(), wrapper);

    System.out.println(
        "StoryExtension: JSON written to: "
            + outputPath.toAbsolutePath()
            + " ("
            + Files.size(outputPath)
            + " bytes)");
  }

  private String loadChapterSummary(String chapterTitle) {
    // Try to find and read the chapter intro from markdown
    // This looks for patterns like: 03_chapter-devil-three-golden-hairs/00_intro.md
    // For now, return null - chapter summary will come from markdown if it exists
    // This prevents coupling between code tests and markdown structure
    return null;
  }

  private void writeChapterMetadata(String bookTitle, ChapterModel chapterModel)
      throws IOException {
    // Create filename from chapter name
    String chapterName = sanitizeFilename(chapterModel.title());
    String filename = "chapter--" + chapterName + ".json";
    Path chapterPath = outputDirectory.resolve(filename);

    // Only write if file doesn't already exist (don't overwrite markdown-generated metadata)
    if (Files.exists(chapterPath)) {
      return;
    }

    // Create a chapter metadata wrapper (matching JsonStoryListener format)
    ChapterMetadataWrapper wrapper =
        new ChapterMetadataWrapper(
            "code-based", // bookPath for code-based chapters
            bookTitle,
            chapterModel);

    objectMapper.writeValue(chapterPath.toFile(), wrapper);
    System.out.println("StoryExtension: Chapter metadata written to: " + chapterPath);
  }

  private String sanitizeFilename(String name) {
    // Remove invalid filename characters and limit length
    String sanitized = name.replaceAll("[^a-zA-Z0-9-_\\s]", "").replaceAll("\\s+", "-");
    return sanitized.substring(0, Math.min(sanitized.length(), MAX_FILENAME_LENGTH));
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getStore(
        ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass()));
  }

  /// Record to hold chapter metadata with book context (matches JsonStoryListener format).
  record ChapterMetadataWrapper(String bookPath, String bookTitle, ChapterModel chapter) {}
}
