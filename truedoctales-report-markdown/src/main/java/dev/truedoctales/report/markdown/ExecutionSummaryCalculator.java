package dev.truedoctales.report.markdown;

import dev.truedoctales.api.model.listener.ChapterExecutionResult;
import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import java.util.List;
import java.util.stream.Stream;

/// Calculates summary statistics from chapter execution results for the Book of Truth report.
/// This class follows the Single Responsibility Principle by focusing only on metric calculation.
final class ExecutionSummaryCalculator {

  private final List<ChapterExecutionResult> chapterResults;

  /// Creates a new ExecutionSummaryCalculator.
  ///
  /// @param chapterResults the chapter execution results to calculate statistics from
  ExecutionSummaryCalculator(List<ChapterExecutionResult> chapterResults) {
    this.chapterResults = chapterResults;
  }

  /// @return total number of chapters
  int totalChapters() {
    return chapterResults.size();
  }

  /// @return total number of stories across all chapters
  int totalStories() {
    return chapterResults.stream().mapToInt(c -> c.storyResults().size()).sum();
  }

  /// @return total number of scenes across all stories
  int totalScenes() {
    return chapterResults.stream()
        .flatMap(c -> c.storyResults().stream())
        .mapToInt(s -> s.sceneResults().size())
        .sum();
  }

  /// @return total number of steps across all scenes
  int totalSteps() {
    return chapterResults.stream()
        .flatMap(c -> c.storyResults().stream())
        .flatMap(s -> s.sceneResults().stream())
        .mapToInt(a -> a.stepResults().size())
        .sum();
  }

  /// @return number of successful steps
  long successfulSteps() {
    return stepResultsStream().filter(sc -> sc.status() == ExecutionStatus.SUCCESS).count();
  }

  /// @return number of failed or error steps
  long failedSteps() {
    return stepResultsStream()
        .filter(
            sc -> sc.status() == ExecutionStatus.FAILURE || sc.status() == ExecutionStatus.ERROR)
        .count();
  }

  private Stream<StepExecutionResult> stepResultsStream() {
    return chapterResults.stream()
        .flatMap(c -> c.storyResults().stream())
        .flatMap(s -> s.sceneResults().stream())
        .flatMap(a -> a.stepResults().stream());
  }
}
