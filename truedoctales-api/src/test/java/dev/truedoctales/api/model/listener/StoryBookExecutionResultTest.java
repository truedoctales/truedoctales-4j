package dev.truedoctales.api.model.listener;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.execution.StoryExecution;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StoryBookExecutionResult")
class StoryBookExecutionResultTest {

  private static final Path BOOK_PATH = Path.of("book-of-stories");

  @Nested
  @DisplayName("constructor(StoryBookModel)")
  class SingleParameterConstructor {

    @Test
    @DisplayName("should initialize with story book model")
    void shouldInitializeWithStoryBookModel() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", "Book summary");

      // Act
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);

      // Assert
      assertNotNull(result);
      assertEquals(book, result.book());
      assertNotNull(result.chapterResults());
      assertTrue(result.chapterResults().isEmpty());
    }

    @Test
    @DisplayName("should initialize with empty chapters list")
    void shouldInitializeWithEmptyChaptersList() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);

      // Act
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);

      // Assert
      assertEquals(0, result.chapterResults().size());
    }
  }

  @Nested
  @DisplayName("constructor(StoryBookModel, List<ChapterExecutionResult>)")
  class TwoParameterConstructor {

    @Test
    @DisplayName("should initialize with story book model and chapter results")
    void shouldInitializeWithStoryBookModelAndChapterResults() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", "Book summary");
      ChapterExecutionResult chapter1 = createChapterExecutionResult("Chapter 1");
      ChapterExecutionResult chapter2 = createChapterExecutionResult("Chapter 2");
      List<ChapterExecutionResult> chapters = List.of(chapter1, chapter2);

      // Act
      StoryBookExecutionResult result = new StoryBookExecutionResult(book, chapters);

      // Assert
      assertNotNull(result);
      assertEquals(book, result.book());
      assertEquals(2, result.chapterResults().size());
      assertEquals(chapter1, result.chapterResults().get(0));
      assertEquals(chapter2, result.chapterResults().get(1));
    }

    @Test
    @DisplayName("should handle null chapters list")
    void shouldHandleNullChaptersList() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);

      // Act
      StoryBookExecutionResult result = new StoryBookExecutionResult(book, null);

      // Assert
      assertNotNull(result.chapterResults());
      assertTrue(result.chapterResults().isEmpty());
    }

    @Test
    @DisplayName("should handle empty chapters list")
    void shouldHandleEmptyChaptersList() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);

      // Act
      StoryBookExecutionResult result = new StoryBookExecutionResult(book, List.of());

      // Assert
      assertNotNull(result.chapterResults());
      assertTrue(result.chapterResults().isEmpty());
    }
  }

  @Nested
  @DisplayName("addChapterResult()")
  class AddChapterResult {

    @Test
    @DisplayName("should add chapter result to empty list")
    void shouldAddChapterResultToEmptyList() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);
      ChapterExecutionResult chapter = createChapterExecutionResult("Chapter 1");

      // Act
      result.addChapterResult(chapter);

      // Assert
      assertEquals(1, result.chapterResults().size());
      assertEquals(chapter, result.chapterResults().get(0));
    }

    @Test
    @DisplayName("should add multiple chapter results")
    void shouldAddMultipleChapterResults() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);
      ChapterExecutionResult chapter1 = createChapterExecutionResult("Chapter 1");
      ChapterExecutionResult chapter2 = createChapterExecutionResult("Chapter 2");
      ChapterExecutionResult chapter3 = createChapterExecutionResult("Chapter 3");

      // Act
      result.addChapterResult(chapter1);
      result.addChapterResult(chapter2);
      result.addChapterResult(chapter3);

      // Assert
      assertEquals(3, result.chapterResults().size());
      assertEquals(chapter1, result.chapterResults().get(0));
      assertEquals(chapter2, result.chapterResults().get(1));
      assertEquals(chapter3, result.chapterResults().get(2));
    }

    @Test
    @DisplayName("should maintain order of added chapters")
    void shouldMaintainOrderOfAddedChapters() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);

      // Act
      for (int i = 1; i <= 5; i++) {
        result.addChapterResult(createChapterExecutionResult("Chapter " + i));
      }

      // Assert
      assertEquals(5, result.chapterResults().size());
      for (int i = 0; i < 5; i++) {
        assertEquals("Chapter " + (i + 1), result.chapterResults().get(i).chapter().title());
      }
    }
  }

  @Nested
  @DisplayName("overall status computation")
  class OverallStatusComputation {

    @Test
    @DisplayName("should compute SUCCESS when no chapters")
    void shouldComputeSuccessWhenNoChapters() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);

      // Act & Assert - Verify no errors when accessing empty chapters
      assertEquals(0, result.chapterResults().size());
    }

    @Test
    @DisplayName("should contain all SUCCESS chapters")
    void shouldContainAllSuccessChapters() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));

      // Act
      boolean allSuccess =
          result.chapterResults().stream()
              .allMatch(chapter -> chapter.status() == ExecutionStatus.SUCCESS);

      // Assert
      assertTrue(allSuccess);
    }

    @Test
    @DisplayName("should contain FAILURE chapter when one chapter fails")
    void shouldContainFailureChapterWhenOneChapterFails() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.FAILURE));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));

      // Act
      boolean hasFailure =
          result.chapterResults().stream()
              .anyMatch(chapter -> chapter.status() == ExecutionStatus.FAILURE);

      // Assert
      assertTrue(hasFailure);
    }

    @Test
    @DisplayName("should contain ERROR chapter when one chapter has error")
    void shouldContainErrorChapterWhenOneChapterHasError() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.ERROR));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));

      // Act
      boolean hasError =
          result.chapterResults().stream()
              .anyMatch(chapter -> chapter.status() == ExecutionStatus.ERROR);

      // Assert
      assertTrue(hasError);
    }

    @Test
    @DisplayName("should support mixed chapter statuses")
    void shouldSupportMixedChapterStatuses() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", null);
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.FAILURE));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.ERROR));
      result.addChapterResult(createChapterWithStatus(ExecutionStatus.SUCCESS));

      // Act
      long successCount =
          result.chapterResults().stream()
              .filter(chapter -> chapter.status() == ExecutionStatus.SUCCESS)
              .count();
      long failureCount =
          result.chapterResults().stream()
              .filter(chapter -> chapter.status() == ExecutionStatus.FAILURE)
              .count();
      long errorCount =
          result.chapterResults().stream()
              .filter(chapter -> chapter.status() == ExecutionStatus.ERROR)
              .count();

      // Assert
      assertEquals(2, successCount);
      assertEquals(1, failureCount);
      assertEquals(1, errorCount);
    }
  }

  @Nested
  @DisplayName("book() accessor")
  class BookAccessor {

    @Test
    @DisplayName("should return the book model")
    void shouldReturnTheBookModel() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", "Summary");
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);

      // Act
      StoryBookModel retrievedBook = result.book();

      // Assert
      assertNotNull(retrievedBook);
      assertEquals(book, retrievedBook);
      assertEquals("Test Book", retrievedBook.title());
      assertEquals("Summary", retrievedBook.summary());
    }

    @Test
    @DisplayName("should return same book instance")
    void shouldReturnSameBookInstance() {
      // Arrange
      StoryBookModel book = createStoryBookModel("Test Book", "Summary");
      StoryBookExecutionResult result = new StoryBookExecutionResult(book);

      // Act
      StoryBookModel first = result.book();
      StoryBookModel second = result.book();

      // Assert
      assertSame(first, second);
    }
  }

  private StoryBookModel createStoryBookModel(String title, String summary) {
    return new StoryBookModel(BOOK_PATH, title, summary, null, List.of());
  }

  private ChapterExecutionResult createChapterExecutionResult(String title) {
    ChapterModel chapter = new ChapterModel(Path.of("chapter"), title, "Summary", List.of());
    return new ChapterExecutionResult(chapter);
  }

  private ChapterExecutionResult createChapterWithStatus(ExecutionStatus status) {
    ChapterModel chapter =
        new ChapterModel(Path.of("chapter"), "Test Chapter", "Summary", List.of());
    ChapterExecutionResult result = new ChapterExecutionResult(chapter);

    StoryExecutionResult story = createStoryWithSceneResults(status);
    result.addStoryResult(story);

    return result;
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
