package dev.truedoctales.execution.jupiter;

import dev.truedoctales.api.execute.JuniperStoryTestBuilder;
import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.execution.*;
import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.execution.execute.StepExecutor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

/// JUnit Jupiter executor that runs test steps based on story execution models.
///
/// This class orchestrates the execution of test stories by delegating to specialized components
/// for method matching and invocation. Converts story executions into JUnit dynamic tests.
public class JupiterStoryTestExecutor implements JuniperStoryTestBuilder {

  private final StoryExecutionListener executionListener;
  private final StepExecutor stepExecutor;

  /// Creates a new JUnit executor with the given registry and listener.
  ///
  /// @param plotRegistry the plot registry for method invocation
  /// @param executionListener the execution listener for events
  public JupiterStoryTestExecutor(
      PlotRegistry plotRegistry, StoryExecutionListener executionListener) {
    this.executionListener = executionListener;
    this.stepExecutor = new StepExecutor(plotRegistry, executionListener);
  }

  @Override
  public Stream<DynamicNode> buildDynamicTests(StoryBookExecution book, Path storyPath) {

    StoryExecution story = book.loadStory(storyPath);
    ChapterExecution chapter = book.findChapterForStory(story).orElseThrow();
    Optional<DynamicTest> startChapter =
        Optional.of(chapter)
            .filter(Predicate.not(c -> c.equals(book.intro())))
            .filter(c -> chapter.stories().getFirst().equals(story))
            .map(
                c ->
                    DynamicTest.dynamicTest(
                        "start Chapter: " + chapter.title(),
                        () -> {
                          executionListener.startChapter(
                              new ChapterModel(
                                  chapter.path(), chapter.title(), chapter.summary(), List.of()));
                        }));

    Optional<DynamicTest> closeChapter =
        Optional.of(chapter)
            .filter(Predicate.not(c -> c.equals(book.intro())))
            .filter(c -> chapter.stories().getLast().equals(story))
            .map(
                c ->
                    DynamicTest.dynamicTest(
                        "End Chapter: " + chapter.title(),
                        () ->
                            executionListener.endChapter(
                                new ChapterModel(
                                    chapter.path(),
                                    chapter.title(),
                                    chapter.summary(),
                                    List.of()))));

    List<DynamicNode> nodes = new ArrayList<>();
    startChapter.ifPresent(nodes::add);

    nodes.add(
        DynamicTest.dynamicTest(
            "start Story: " + story.title(), () -> executionListener.startStory(story)));

    // Add prequels container if prequels exist
    if (!story.prequels().isEmpty()) {
      nodes.add(buildPrequelsContainer(book, story));
    }

    // Add each scene as a dynamic test
    for (SceneExecution scene : story.scenes()) {
      nodes.add(buildSceneTest(scene));
    }

    nodes.add(
        DynamicTest.dynamicTest(
            "End Story: " + story.title(), () -> executionListener.endStory(story)));

    closeChapter.ifPresent(nodes::add);

    return nodes.stream();
  }

  private DynamicContainer buildPrequelsContainer(
      StoryBookExecution book, StoryExecution storyExecution) {

    Stream<DynamicNode> prequels =
        storyExecution.prequels().stream()
            .map(
                prequelPath ->
                    book.path()
                        .resolve(storyExecution.path().getParent().resolve(prequelPath))
                        .normalize())
            .map(prequelRootPath -> book.path().relativize(prequelRootPath).normalize())
            .flatMap(prequelStory -> buildDynamicTests(book, prequelStory));
    return DynamicContainer.dynamicContainer("Prequels for ", prequels);
  }

  private DynamicTest buildSceneTest(SceneExecution scene) {
    return DynamicTest.dynamicTest("Scene: " + scene.title(), () -> executeScene(scene));
  }

  private void executeScene(SceneExecution scene) {
    executionListener.startScene(scene);
    List<StepExecutionResult> stepResults = new ArrayList<>();
    for (StepExecution step : scene.steps()) {
      stepResults.add(stepExecutor.execute(step));
    }
    executionListener.endScene(new SceneExecutionResult(scene, stepResults));

    // Propagate failures to JUnit
    propagateFailures(stepResults);
  }

  private void propagateFailures(List<StepExecutionResult> stepResults) {
    for (StepExecutionResult result : stepResults) {
      if (result.status() == ExecutionStatus.ERROR || result.status() == ExecutionStatus.FAILURE) {
        // Re-throw the original exception to fail the JUnit test
        if (result.throwable() != null) {
          if (result.throwable() instanceof Error error) {
            throw error;
          }
          if (result.throwable() instanceof RuntimeException runtimeException) {
            throw runtimeException;
          }
          throw new AssertionError(result.errorMessage(), result.throwable());
        } else {
          throw new AssertionError(result.errorMessage());
        }
      }
    }
  }
}
