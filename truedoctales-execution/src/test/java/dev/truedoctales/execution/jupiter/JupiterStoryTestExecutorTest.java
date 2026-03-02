package dev.truedoctales.execution.jupiter;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.execution.*;
import dev.truedoctales.api.model.story.StepCall;
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
    StoryBookExecution book = createTestBookExecution();
    ChapterExecution chapter = book.prequelChapter();
    StoryExecution story = chapter.stories().getFirst();

    // Act
    List<DynamicNode> nodes =
        executor.buildDynamicTests(book, story.path()).collect(Collectors.toList());

    // Assert - Find the scene test node and execute it to verify it throws
    assertNotNull(nodes);
    assertFalse(nodes.isEmpty());

    // Find and execute the scene test
    DynamicNode sceneNode =
        nodes.stream()
            .filter(n -> n.getDisplayName().startsWith("Scene:"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No scene test found"));

    // Execute the test and verify it throws AssertionFailedError
    assertThrows(
        AssertionFailedError.class,
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
    StoryBookExecution book = createTestBookExecution();
    ChapterExecution chapter = book.prequelChapter();
    StoryExecution story = chapter.stories().getFirst();

    // Act
    List<DynamicNode> nodes = executor.buildDynamicTests(book, story.path()).toList();

    // Find and execute the scene test
    DynamicNode sceneNode =
        nodes.stream()
            .filter(n -> n.getDisplayName().startsWith("Scene:"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No scene test found"));

    // Execute the test and verify it throws RuntimeException
    assertThrows(
        RuntimeException.class,
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
    StoryBookExecution book = createTestBookExecution();
    ChapterExecution chapter = book.prequelChapter();
    StoryExecution story = chapter.stories().getFirst();

    // Act
    List<DynamicNode> nodes = executor.buildDynamicTests(book, story.path()).toList();

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

  private StoryBookExecution createTestBookExecution() {
    Path bookPath = Path.of("book");
    Path introPath = Path.of("intro");
    Path storyPath = introPath.resolve("test.md");
    StepBinding binding = new StepBinding("TestPlot", "test binding", InputType.SEQUENCE);
    StepExecution step =
        new StepExecution(binding, new StepCall("TestPlot", "test binding"), List.of(), 0);
    SceneExecution scene = new SceneExecution("Test Scene", 0, List.of(step));
    StoryExecution story = new StoryExecution(storyPath, "Test Story", List.of(), List.of(scene));
    ChapterExecution intro = new ChapterExecution(0, introPath, "Intro", List.of(story));
    return new StoryBookExecution(bookPath, "Test Book", intro, List.of());
  }

  static class TestPlotRegistry implements PlotRegistry {
    boolean shouldThrowAssertionError = false;
    boolean shouldThrowRuntimeException = false;

    @Override
    public Set<PlotBinding> getBindings() {
      return Set.of();
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
