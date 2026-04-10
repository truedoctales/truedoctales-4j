package dev.truedoctales.execution.jupiter;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.execution.*;
import dev.truedoctales.api.model.plot.PlotBinding;
import dev.truedoctales.api.model.plot.StepBinding;
import dev.truedoctales.api.model.story.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class JupiterStoryTestExecutorTest {

  private JupiterStoryTestExecutor executor;
  private TestPlotRegistry plotRegistry;
  private TestExecutionListener listener;

  @BeforeEach
  void setUp() {
    plotRegistry = new TestPlotRegistry();
    listener = new TestExecutionListener();
    executor = new JupiterStoryTestExecutor(plotRegistry, listener);
  }

  @Test
  void buildDynamicTests_shouldPropagateStepFailures() {
    // Arrange
    plotRegistry.shouldThrowAssertionError = true;
    var book = createTestBookModel();

    // Act
    List<DynamicNode> nodes =
        executor
            .buildDynamicTests(book, book.prequelChapter().stories().getFirst().path())
            .collect(Collectors.toList());

    // Assert - Find the scene test node and execute it to verify it throws
    assertNotNull(nodes);
    assertFalse(nodes.isEmpty());

    // Find and execute the scene test
    DynamicNode sceneNode =
        nodes.stream()
            .filter(n -> n.getDisplayName().startsWith("Scene:"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No scene test found"));

    // Execute the test and verify it throws AssertionError
    assertThrows(
        AssertionError.class,
        () -> {
          if (sceneNode instanceof org.junit.jupiter.api.DynamicTest test) {
            test.getExecutable().execute();
          }
        });
  }

  @Test
  void buildDynamicTests_shouldPropagateRuntimeExceptions() {
    // Arrange
    plotRegistry.shouldThrowRuntimeException = true;
    var book = createTestBookModel();
    var path = book.prequelChapter().stories().getFirst().path();

    // Act
    List<DynamicNode> nodes = executor.buildDynamicTests(book, path).toList();

    // Find and execute the scene test
    DynamicNode sceneNode =
        nodes.stream()
            .filter(n -> n.getDisplayName().startsWith("Scene:"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No scene test found"));

    // Execute the test and verify it throws AssertionError
    assertThrows(
        AssertionError.class,
        () -> {
          if (sceneNode instanceof org.junit.jupiter.api.DynamicTest test) {
            test.getExecutable().execute();
          }
        });
  }

  @Test
  void buildDynamicTests_shouldNotThrowWhenAllStepsPass() {
    // Arrange
    plotRegistry.shouldThrowAssertionError = false;
    plotRegistry.shouldThrowRuntimeException = false;
    var book = createTestBookModel();
    var path = book.prequelChapter().stories().getFirst().path();

    // Act
    List<DynamicNode> nodes = executor.buildDynamicTests(book, path).toList();

    // Find and execute the scene test
    DynamicNode sceneNode =
        nodes.stream()
            .filter(n -> n.getDisplayName().startsWith("Scene:"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No scene test found"));

    // Execute the test and verify it doesn't throw
    assertDoesNotThrow(
        () -> {
          if (sceneNode instanceof org.junit.jupiter.api.DynamicTest test) {
            test.getExecutable().execute();
          }
        });
  }

  private StoryBookModel createTestBookModel() {
    Path bookPath = Path.of("book");
    Path introPath = Path.of("intro");
    Path storyPath = introPath.resolve("test.md");

    StepTask step = new StepTask(0, new StepCall("TestPlot", "test binding"));
    SceneModel scene = new SceneModel("Test Scene", 1, List.of(step));
    StoryModel story = new StoryModel(0, storyPath, "Test Story", List.of(), List.of(scene));
    ChapterModel intro = new ChapterModel(0, introPath, "Intro", List.of(story));
    return new StoryBookModel(bookPath, "Test Book", intro, List.of());
  }

  static class TestPlotRegistry implements PlotRegistry {
    boolean shouldThrowAssertionError = false;
    boolean shouldThrowRuntimeException = false;

    @Override
    public Set<PlotBinding> getBindings() {
      return Set.of(
          new PlotBinding(
              "TestPlot",
              List.of(new StepBinding("TestPlot", "test binding", InputType.SEQUENCE))));
    }

    @Override
    public Object invoke(StepExecution stepExecution) {
      if (shouldThrowAssertionError) {
        throw new AssertionFailedError("Test assertion failed");
      }
      if (shouldThrowRuntimeException) {
        throw new RuntimeException("Test runtime exception");
      }
      return null;
    }
  }

  static class TestExecutionListener implements StoryExecutionListener {
    @Override
    public void endStory(StoryExecution result) {
      // No-op for testing
    }
  }
}
