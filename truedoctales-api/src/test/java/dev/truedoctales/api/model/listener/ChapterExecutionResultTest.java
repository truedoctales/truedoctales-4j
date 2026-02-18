package dev.truedoctales.api.model.listener;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.execution.StoryExecution;
import dev.truedoctales.api.model.story.ChapterModel;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChapterExecutionResult")
class ChapterExecutionResultTest {

  private static final Path CHAPTER_PATH = Path.of("chapters", "chapter-01");

  @Nested
  @DisplayName("constructor(ChapterModel)")
  class SingleParameterConstructor {

    @Test
    @DisplayName("should initialize with chapter model")
    void shouldInitializeWithChapterModel() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", "Chapter summary");

      // Act
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);

      // Assert
      assertNotNull(result);
      assertEquals(chapter, result.chapter());
      assertNotNull(result.storyResults());
      assertTrue(result.storyResults().isEmpty());
    }

    @Test
    @DisplayName("should initialize with empty story results list")
    void shouldInitializeWithEmptyStoryResultsList() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);

      // Act
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);

      // Assert
      assertEquals(0, result.storyResults().size());
    }
  }

  @Nested
  @DisplayName("constructor(ChapterModel, List<StoryExecutionResult>)")
  class TwoParameterConstructor {

    @Test
    @DisplayName("should initialize with chapter model and story results")
    void shouldInitializeWithChapterModelAndStoryResults() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", "Chapter summary");
      StoryExecutionResult story1 = createStoryExecutionResult("Story 1");
      StoryExecutionResult story2 = createStoryExecutionResult("Story 2");
      List<StoryExecutionResult> stories = List.of(story1, story2);

      // Act
      ChapterExecutionResult result = new ChapterExecutionResult(chapter, stories);

      // Assert
      assertNotNull(result);
      assertEquals(chapter, result.chapter());
      assertEquals(2, result.storyResults().size());
      assertEquals(story1, result.storyResults().get(0));
      assertEquals(story2, result.storyResults().get(1));
    }

    @Test
    @DisplayName("should handle null story results list")
    void shouldHandleNullStoryResultsList() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);

      // Act
      ChapterExecutionResult result = new ChapterExecutionResult(chapter, null);

      // Assert
      assertNotNull(result.storyResults());
      assertTrue(result.storyResults().isEmpty());
    }

    @Test
    @DisplayName("should handle empty story results list")
    void shouldHandleEmptyStoryResultsList() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);

      // Act
      ChapterExecutionResult result = new ChapterExecutionResult(chapter, List.of());

      // Assert
      assertNotNull(result.storyResults());
      assertTrue(result.storyResults().isEmpty());
    }
  }

  @Nested
  @DisplayName("addStoryResult()")
  class AddStoryResult {

    @Test
    @DisplayName("should add story result to empty list")
    void shouldAddStoryResultToEmptyList() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);
      StoryExecutionResult story = createStoryExecutionResult("Story 1");

      // Act
      result.addStoryResult(story);

      // Assert
      assertEquals(1, result.storyResults().size());
      assertEquals(story, result.storyResults().get(0));
    }

    @Test
    @DisplayName("should add multiple story results")
    void shouldAddMultipleStoryResults() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);
      StoryExecutionResult story1 = createStoryExecutionResult("Story 1");
      StoryExecutionResult story2 = createStoryExecutionResult("Story 2");
      StoryExecutionResult story3 = createStoryExecutionResult("Story 3");

      // Act
      result.addStoryResult(story1);
      result.addStoryResult(story2);
      result.addStoryResult(story3);

      // Assert
      assertEquals(3, result.storyResults().size());
      assertEquals(story1, result.storyResults().get(0));
      assertEquals(story2, result.storyResults().get(1));
      assertEquals(story3, result.storyResults().get(2));
    }

    @Test
    @DisplayName("should maintain order of added stories")
    void shouldMaintainOrderOfAddedStories() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);

      // Act
      for (int i = 1; i <= 5; i++) {
        result.addStoryResult(createStoryExecutionResult("Story " + i));
      }

      // Assert
      assertEquals(5, result.storyResults().size());
      for (int i = 0; i < 5; i++) {
        assertEquals("Story " + (i + 1), result.storyResults().get(i).execution().title());
      }
    }
  }

  @Nested
  @DisplayName("status()")
  class Status {

    @Test
    @DisplayName("should return SUCCESS when no stories")
    void shouldReturnSuccessWhenNoStories() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should return SUCCESS when all stories are SUCCESS")
    void shouldReturnSuccessWhenAllStoriesAreSuccess() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should return FAILURE when one story is FAILURE")
    void shouldReturnFailureWhenOneStoryIsFailure() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.FAILURE));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.FAILURE, status);
    }

    @Test
    @DisplayName("should return ERROR when one story is ERROR")
    void shouldReturnErrorWhenOneStoryIsError() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.ERROR));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should prioritize ERROR over FAILURE")
    void shouldPrioritizeErrorOverFailure() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.FAILURE));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.ERROR));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should delegate to ExecutionStatusCalculator")
    void shouldDelegateToExecutionStatusCalculator() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", null);
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.FAILURE));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.ERROR));
      result.addStoryResult(createStoryWithSceneResults(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = result.status();

      // Assert - ExecutionStatusCalculator should prioritize ERROR
      assertEquals(ExecutionStatus.ERROR, status);
    }
  }

  @Nested
  @DisplayName("chapter() accessor")
  class ChapterAccessor {

    @Test
    @DisplayName("should return the chapter model")
    void shouldReturnTheChapterModel() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", "Summary");
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);

      // Act
      ChapterModel retrievedChapter = result.chapter();

      // Assert
      assertNotNull(retrievedChapter);
      assertEquals(chapter, retrievedChapter);
      assertEquals("Test Chapter", retrievedChapter.title());
      assertEquals("Summary", retrievedChapter.summary());
    }

    @Test
    @DisplayName("should return same chapter instance")
    void shouldReturnSameChapterInstance() {
      // Arrange
      ChapterModel chapter = createChapterModel("Test Chapter", "Summary");
      ChapterExecutionResult result = new ChapterExecutionResult(chapter);

      // Act
      ChapterModel first = result.chapter();
      ChapterModel second = result.chapter();

      // Assert
      assertSame(first, second);
    }
  }

  private ChapterModel createChapterModel(String title, String summary) {
    return new ChapterModel(CHAPTER_PATH, title, summary, List.of());
  }

  private StoryExecutionResult createStoryExecutionResult(String title) {
    StoryExecution execution =
        new StoryExecution(Path.of("story.md"), title, "Summary", List.of(), List.of());
    return new StoryExecutionResult(execution);
  }

  private StoryExecutionResult createStoryWithSceneResults(ExecutionStatus status) {
    StoryExecution execution =
        new StoryExecution(Path.of("story.md"), "Test Story", "Summary", List.of(), List.of());
    StoryExecutionResult result = new StoryExecutionResult(execution);

    SceneExecutionResult scene = createSceneWithStatus(status);
    result.addSceneResult(scene);

    return result;
  }

  private SceneExecutionResult createSceneWithStatus(ExecutionStatus status) {
    StepExecutionResult step = new StepExecutionResult(null, status, null, null);
    return new SceneExecutionResult(null, List.of(step));
  }
}
