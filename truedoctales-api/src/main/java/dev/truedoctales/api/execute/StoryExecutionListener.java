package dev.truedoctales.api.execute;

import dev.truedoctales.api.model.execution.SceneExecution;
import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.execution.StoryExecution;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.plot.PlotBinding;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.util.List;
import java.util.Set;

/// Interface for listening to story execution lifecycle events.
///
/// Implementations can track execution progress, log events, generate reports, or perform
/// other actions in response to story execution events.
public interface StoryExecutionListener {

  /// Called when book execution starts.
  ///
  /// @param storyBookModel the book model being executed
  default void startBook(StoryBookModel storyBookModel) {}

  /// Called when book execution completes.
  default void closeBook() {}

  /// Called once with the full set of plot bindings registered for this book.
  ///
  /// @param bindings all registered plot bindings
  default void onPlotBindings(Set<PlotBinding> bindings) {}

  /// Called when a story execution starts.
  ///
  /// @param execution the story execution detales
  default void startStory(StoryExecution execution) {}

  /// Called when a story execution ends.
  ///
  /// @param result the story execution detales
  default void endStory(StoryExecution result) {}

  /// Called when a chapter execution starts.
  ///
  /// @param execution the chapter model
  default void startChapter(ChapterModel execution) {}

  /// Called when a chapter execution ends.
  ///
  /// @param execution the chapter model
  default void endChapter(ChapterModel execution) {}

  /// Called when a binding execution starts.
  ///
  /// @param step the binding execution detales
  default void startStep(StepExecution step) {}

  /// Called when a binding execution ends.
  ///
  /// @param result the binding execution result
  default void endStep(StepExecutionResult result) {}

  /// Called when a binding fails.
  ///
  /// @param step the binding that failed
  /// @param e the exception that occurred
  default void recordFailure(StepExecution step, Throwable e) {
    e.printStackTrace();
  }

  /// Called when a scene execution starts.
  ///
  /// @param scene the scene execution detales
  default void startScene(SceneExecution scene) {}

  /// Called when a scene execution ends.
  ///
  /// @param result the scene execution result
  default void endScene(SceneExecutionResult result) {}

  /// Delegate listener that forwards events to multiple listeners.
  record DelegateStoryExecutionListener(List<StoryExecutionListener> listener)
      implements StoryExecutionListener {

    /// Creates a delegate with the given listeners.
    ///
    /// @param listener the listeners to delegate to
    public DelegateStoryExecutionListener(StoryExecutionListener... listener) {
      this(List.of(listener));
    }

    @Override
    public void startStory(StoryExecution execution) {
      listener.forEach(l -> l.startStory(execution));
    }

    @Override
    public void endStory(StoryExecution result) {
      listener.forEach(l -> l.endStory(result));
    }

    @Override
    public void startChapter(ChapterModel execution) {
      listener.forEach(l -> l.startChapter(execution));
    }

    @Override
    public void endChapter(ChapterModel result) {
      listener.forEach(l -> l.endChapter(result));
    }

    @Override
    public void startStep(StepExecution step) {
      listener.forEach(l -> l.startStep(step));
    }

    @Override
    public void endStep(StepExecutionResult result) {
      listener.forEach(l -> l.endStep(result));
    }

    @Override
    public void recordFailure(StepExecution step, Throwable e) {
      listener.forEach(l -> l.recordFailure(step, e));
    }

    @Override
    public void startScene(SceneExecution scene) {
      listener.forEach(l -> l.startScene(scene));
    }

    @Override
    public void endScene(SceneExecutionResult result) {
      listener.forEach(l -> l.endScene(result));
    }

    @Override
    public void closeBook() {
      listener.forEach(StoryExecutionListener::closeBook);
    }

    @Override
    public void onPlotBindings(Set<PlotBinding> bindings) {
      listener.forEach(l -> l.onPlotBindings(bindings));
    }
  }
}
