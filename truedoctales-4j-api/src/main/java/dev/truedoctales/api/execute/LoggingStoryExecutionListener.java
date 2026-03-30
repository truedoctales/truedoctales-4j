package dev.truedoctales.api.execute;

import dev.truedoctales.api.model.execution.SceneExecution;
import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.execution.StoryExecution;
import dev.truedoctales.api.model.listener.SceneExecutionResult;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.story.ChapterModel;
import java.util.logging.Level;
import java.util.logging.Logger;

/// Logging implementation of StoryExecutionListener.
///
/// Logs all story execution events to the Java logger at INFO level.
public class LoggingStoryExecutionListener implements StoryExecutionListener {

  private final Logger LOGGER = Logger.getLogger(getClass().getName());

  @Override
  public void endStory(StoryExecution result) {
    LOGGER.info("Story ended: " + result.title());
  }

  @Override
  public void startStory(StoryExecution execution) {
    LOGGER.info("Starting story: " + execution.title());
  }

  @Override
  public void startChapter(ChapterModel execution) {
    LOGGER.info("  Starting chapter: " + execution.title());
  }

  @Override
  public void endChapter(ChapterModel result) {
    LOGGER.info("  Chapter ended with status: " + result.title());
  }

  @Override
  public void startScene(SceneExecution scene) {
    LOGGER.info("  Starting scene: " + scene.title());
  }

  @Override
  public void endScene(SceneExecutionResult result) {
    LOGGER.info("  Scene ended with status: " + result.status());
  }

  @Override
  public void startStep(StepExecution step) {
    LOGGER.info("    Starting binding: " + step.call() + " at line " + step.lineNumber());
  }

  @Override
  public void endStep(StepExecutionResult result) {
    LOGGER.info("    Step ended with status: " + result.status());
  }

  @Override
  public void closeBook() {
    LOGGER.info("╔════════════════════════════════════════════════════════════╗");
    LOGGER.info("║                  Story Book Execution Done!                ║");
    LOGGER.info("╚════════════════════════════════════════════════════════════╝");
  }

  @Override
  public void recordFailure(StepExecution step, Throwable e) {
    LOGGER.log(
        Level.SEVERE,
        "    Step failed: " + step.binding() + " with exception: " + e.getMessage(),
        e);
  }
}
