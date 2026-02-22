package dev.truedoctales.execution.execute;

import dev.truedoctales.api.model.execution.*;
import dev.truedoctales.api.model.story.*;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/// Maps story book models to execution models with bound steps.
///
/// Resolves binding bindings by matching binding patterns with plot bindings and extracts
/// variables from binding values.
public class StoryBookExecutionMapperImpl implements Function<StoryBookModel, StoryBookExecution> {

  private static final Logger LOGGER =
      java.util.logging.Logger.getLogger(StoryBookExecutionMapperImpl.class.getName());
  private static final org.slf4j.Logger log =
      LoggerFactory.getLogger(StoryBookExecutionMapperImpl.class);

  private final Set<PlotBinding> plotBindings;
  private final ChapterExecutionMapper chapterExecutionMapper = new ChapterExecutionMapper();
  private final StoryExecutionMapper storyExecutionMapper = new StoryExecutionMapper();
  private final SceneExecutionMapper sceneExecutionMapper = new SceneExecutionMapper();
  private final StepExecutionMapper stepExecutionMapper = new StepExecutionMapper();
  private final VariableExtractor variableExtractor = new VariableExtractor();

  /// Creates a new mapper with the given plot bindings.
  ///
  /// @param plotBindings the available plot bindings
  public StoryBookExecutionMapperImpl(Set<PlotBinding> plotBindings) {
    this.plotBindings = plotBindings;
  }

  @Override
  public StoryBookExecution apply(StoryBookModel book) {
    LOGGER.info("Build execution model for story book: " + book.title());
    StoryBookExecution storyBookExecution =
        new StoryBookExecution(
            book.path(),
            book.title(),
            Optional.ofNullable(book.prequelChapter()).map(chapterExecutionMapper).orElse(null),
            book.chapters().stream().map(chapterExecutionMapper).toList());
    LOGGER.info("Build execution model for story book: " + storyBookExecution.title());
    return storyBookExecution;
  }

  /// Maps a chapter model to execution model.
  ///
  /// @param chapterModel the chapter model
  /// @return the chapter execution
  public ChapterExecution mapChapter(ChapterModel chapterModel) {
    return chapterExecutionMapper.apply(chapterModel);
  }

  /// Maps a story model to execution model.
  ///
  /// @param storyModel the story model
  /// @return the story execution
  public StoryExecution mapStory(StoryModel storyModel) {
    return storyExecutionMapper.apply(storyModel);
  }

  /// Maps a scene model to execution model.
  ///
  /// @param sceneModel the scene model
  /// @return the scene execution
  public SceneExecution mapScene(SceneModel sceneModel) {
    return sceneExecutionMapper.apply(sceneModel);
  }

  /// Maps a binding model to execution model.
  ///
  /// @param stepModel the binding task model
  /// @return the binding execution
  public StepExecution mapStep(StepTask stepModel) {
    return stepExecutionMapper.apply(stepModel);
  }

  class ChapterExecutionMapper implements Function<ChapterModel, ChapterExecution> {

    @Override
    public ChapterExecution apply(ChapterModel chapterModel) {
      if (chapterModel == null) {
        return null;
      }
      LOGGER.info("Build execution model for chapter: " + chapterModel.title());
      return new ChapterExecution(
          chapterModel.path(),
          chapterModel.title(),
          chapterModel.summary(),
          chapterModel.stories().stream().map(storyExecutionMapper).toList());
    }
  }

  class StoryExecutionMapper implements Function<StoryModel, StoryExecution> {

    @Override
    public StoryExecution apply(StoryModel storyModel) {
      if (storyModel == null) {
        return null;
      }
      LOGGER.info("Build execution model for story: " + storyModel.title());

      return new StoryExecution(
          storyModel.path(),
          storyModel.title(),
          storyModel.prequels(),
          storyModel.scenes().stream().map(sceneExecutionMapper).toList());
    }
  }

  class SceneExecutionMapper implements Function<SceneModel, SceneExecution> {

    @Override
    public SceneExecution apply(SceneModel sceneModel) {
      if (sceneModel == null) {
        return null;
      }
      LOGGER.fine("Build execution model for scene: " + sceneModel.title());
      // Filter to only execute StepTask instances, StepDescription instances are for documentation
      return new SceneExecution(
          sceneModel.title(),
          sceneModel.startLineNumber(),
          sceneModel.steps().stream().map(stepExecutionMapper).toList());
    }
  }

  StepBinding findStepBinding(StepCall stepCall) {
    List<StepBinding> matchingBindings = findMatchingStepBindings(stepCall);

    if (matchingBindings.size() > 1) {
      log.warn(
          "Multiple bindings found for step call: {}. Matching bindings: {}",
          stepCall,
          matchingBindings);
    }
    return matchingBindings.stream()
        .sorted(Comparator.comparing((StepBinding m) -> m.pattern().length()).reversed())
        .findFirst()
        .orElseThrow(() -> throwNoBindingFoundError(stepCall));
  }

  private List<StepBinding> findMatchingStepBindings(StepCall stepCall) {
    return plotBindings.stream()
        .filter(p -> p.plotId().equals(stepCall.plotName()))
        .flatMap(p -> p.steps().stream())
        .filter(p -> variableExtractor.matches(p.pattern(), stepCall.stepValue()))
        .toList();
  }

  private void throwMultipleBindingsError(StepCall stepCall) {
    throw new IllegalStateException("Multiple binding bindings found for binding key: " + stepCall);
  }

  private IllegalStateException throwNoBindingFoundError(StepCall stepCall) {
    return new IllegalStateException("No binding binding found for binding key: " + stepCall);
  }

  class StepExecutionMapper implements Function<StepTask, StepExecution> {

    @Override
    public StepExecution apply(StepTask stepModel) {
      if (stepModel == null) {
        return null;
      }
      try {
        StepBinding stepBinding = findStepBinding(stepModel.call());
        Map<String, String> extractedVariables =
            variableExtractor.extractVariables(stepBinding.pattern(), stepModel.call().stepValue());
        List<Map<String, String>> inputRows = new ArrayList<>(stepModel.inputRows());

        // Keep variables separate from table data
        Map<String, String> variables = extractedVariables != null ? extractedVariables : Map.of();

        return new StepExecution(
            stepBinding, stepModel.call(), inputRows, stepModel.lineNumber(), variables);
      } catch (Exception e) {
        throw new RuntimeException("Error mapping binding: " + stepModel, e);
      }
    }
  }
}
