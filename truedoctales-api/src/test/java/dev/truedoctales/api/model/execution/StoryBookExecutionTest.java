package dev.truedoctales.api.model.execution;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StoryBookExecutionTest {

  @Test
  void loadStory_shouldReturnStory_whenStoryExistsInRegularChapter() {
    // Arrange
    Path storyPath = Path.of("book/chapter1/story1.md");
    StoryExecution expectedStory = createStory(storyPath, "Story 1");
    ChapterExecution chapter = createChapter(Path.of("book/chapter1"), "Chapter 1", expectedStory);
    StoryBookExecution book = createBook(null, List.of(chapter));

    // Act
    StoryExecution actualStory = book.loadStory(storyPath);

    // Assert
    assertEquals(expectedStory, actualStory);
    assertEquals(storyPath, actualStory.path());
  }

  @Test
  void loadStory_shouldReturnStory_whenStoryExistsInIntroChapter() {
    // Arrange
    Path storyPath = Path.of("book/intro/story1.md");
    StoryExecution expectedStory = createStory(storyPath, "Intro Story");
    ChapterExecution introChapter = createChapter(Path.of("book/intro"), "Intro", expectedStory);
    ChapterExecution regularChapter =
        createChapter(
            Path.of("book/chapter1"),
            "Chapter 1",
            createStory(Path.of("book/chapter1/story2.md"), "Story 2"));
    StoryBookExecution book = createBook(introChapter, List.of(regularChapter));

    // Act
    StoryExecution actualStory = book.loadStory(storyPath);

    // Assert
    assertEquals(expectedStory, actualStory);
    assertEquals(storyPath, actualStory.path());
  }

  @Test
  void loadStory_shouldReturnStory_whenMultipleStoriesExistInDifferentChapters() {
    // Arrange
    Path targetStoryPath = Path.of("book/chapter2/story-target.md");
    StoryExecution story1 = createStory(Path.of("book/chapter1/story1.md"), "Story 1");
    StoryExecution targetStory = createStory(targetStoryPath, "Target Story");
    StoryExecution story3 = createStory(Path.of("book/chapter3/story3.md"), "Story 3");

    ChapterExecution chapter1 = createChapter(Path.of("book/chapter1"), "Chapter 1", story1);
    ChapterExecution chapter2 = createChapter(Path.of("book/chapter2"), "Chapter 2", targetStory);
    ChapterExecution chapter3 = createChapter(Path.of("book/chapter3"), "Chapter 3", story3);

    StoryBookExecution book = createBook(null, List.of(chapter1, chapter2, chapter3));

    // Act
    StoryExecution actualStory = book.loadStory(targetStoryPath);

    // Assert
    assertEquals(targetStory, actualStory);
  }

  @Test
  void loadStory_shouldReturnStory_whenChapterContainsMultipleStories() {
    // Arrange
    Path targetStoryPath = Path.of("book/chapter1/story2.md");
    StoryExecution story1 = createStory(Path.of("book/chapter1/story1.md"), "Story 1");
    StoryExecution targetStory = createStory(targetStoryPath, "Story 2");
    StoryExecution story3 = createStory(Path.of("book/chapter1/story3.md"), "Story 3");

    ChapterExecution chapter =
        createChapterWithMultipleStories(
            Path.of("book/chapter1"), "Chapter 1", story1, targetStory, story3);
    StoryBookExecution book = createBook(null, List.of(chapter));

    // Act
    StoryExecution actualStory = book.loadStory(targetStoryPath);

    // Assert
    assertEquals(targetStory, actualStory);
  }

  @Test
  void loadStory_shouldThrowIllegalArgumentException_whenStoryNotFound() {
    // Arrange
    Path nonExistentPath = Path.of("book/chapter1/non-existent.md");
    StoryExecution story = createStory(Path.of("book/chapter1/story1.md"), "Story 1");
    ChapterExecution chapter = createChapter(Path.of("book/chapter1"), "Chapter 1", story);
    StoryBookExecution book = createBook(null, List.of(chapter));

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> book.loadStory(nonExistentPath));

    assertTrue(exception.getMessage().contains("Story not found"));
    assertTrue(exception.getMessage().contains(nonExistentPath.toString()));
  }

  @Test
  void loadStory_shouldThrowIllegalArgumentException_whenBookHasNoChapters() {
    // Arrange
    Path storyPath = Path.of("book/chapter1/story1.md");
    StoryBookExecution book = createBook(null, List.of());

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> book.loadStory(storyPath));

    assertTrue(exception.getMessage().contains("Story not found"));
  }

  @Test
  void loadStory_shouldThrowIllegalArgumentException_whenChaptersExistButStoryNotInThem() {
    // Arrange
    Path searchPath = Path.of("book/chapter2/story2.md");
    StoryExecution story = createStory(Path.of("book/chapter1/story1.md"), "Story 1");
    ChapterExecution chapter = createChapter(Path.of("book/chapter1"), "Chapter 1", story);
    StoryBookExecution book = createBook(null, List.of(chapter));

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> book.loadStory(searchPath));

    assertTrue(exception.getMessage().contains("Story not found"));
  }

  @Test
  void findChapterForStory_shouldReturnChapter_whenStoryExistsInRegularChapter() {
    // Arrange
    Path storyPath = Path.of("book/chapter1/story1.md");
    StoryExecution story = createStory(storyPath, "Story 1");
    ChapterExecution expectedChapter = createChapter(Path.of("book/chapter1"), "Chapter 1", story);
    StoryBookExecution book = createBook(null, List.of(expectedChapter));

    // Act
    Optional<ChapterExecution> result = book.findChapterForStory(story);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(expectedChapter, result.get());
  }

  @Test
  void findChapterForStory_shouldReturnChapter_whenStoryExistsInIntroChapter() {
    // Arrange
    Path storyPath = Path.of("book/intro/story1.md");
    StoryExecution story = createStory(storyPath, "Intro Story");
    ChapterExecution introChapter = createChapter(Path.of("book/intro"), "Intro", story);
    ChapterExecution regularChapter =
        createChapter(
            Path.of("book/chapter1"),
            "Chapter 1",
            createStory(Path.of("book/chapter1/story2.md"), "Story 2"));
    StoryBookExecution book = createBook(introChapter, List.of(regularChapter));

    // Act
    Optional<ChapterExecution> result = book.findChapterForStory(story);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(introChapter, result.get());
  }

  @Test
  void findChapterForStory_shouldReturnCorrectChapter_whenMultipleChaptersExist() {
    // Arrange
    Path targetStoryPath = Path.of("book/chapter2/story-target.md");
    StoryExecution story1 = createStory(Path.of("book/chapter1/story1.md"), "Story 1");
    StoryExecution targetStory = createStory(targetStoryPath, "Target Story");
    StoryExecution story3 = createStory(Path.of("book/chapter3/story3.md"), "Story 3");

    ChapterExecution chapter1 = createChapter(Path.of("book/chapter1"), "Chapter 1", story1);
    ChapterExecution expectedChapter =
        createChapter(Path.of("book/chapter2"), "Chapter 2", targetStory);
    ChapterExecution chapter3 = createChapter(Path.of("book/chapter3"), "Chapter 3", story3);

    StoryBookExecution book = createBook(null, List.of(chapter1, expectedChapter, chapter3));

    // Act
    Optional<ChapterExecution> result = book.findChapterForStory(targetStory);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(expectedChapter, result.get());
  }

  @Test
  void findChapterForStory_shouldReturnEmpty_whenStoryChapterPathDoesNotMatchAnyChapter() {
    // Arrange
    Path orphanStoryPath = Path.of("book/orphan-chapter/story.md");
    StoryExecution orphanStory = createStory(orphanStoryPath, "Orphan Story");

    StoryExecution regularStory = createStory(Path.of("book/chapter1/story1.md"), "Story 1");
    ChapterExecution chapter = createChapter(Path.of("book/chapter1"), "Chapter 1", regularStory);
    StoryBookExecution book = createBook(null, List.of(chapter));

    // Act
    Optional<ChapterExecution> result = book.findChapterForStory(orphanStory);

    // Assert
    assertFalse(result.isPresent());
  }

  @Test
  void findChapterForStory_shouldReturnEmpty_whenBookHasNoChapters() {
    // Arrange
    StoryExecution story = createStory(Path.of("book/chapter1/story1.md"), "Story 1");
    StoryBookExecution book = createBook(null, List.of());

    // Act
    Optional<ChapterExecution> result = book.findChapterForStory(story);

    // Assert
    assertFalse(result.isPresent());
  }

  @Test
  void findChapterForStory_shouldReturnEmpty_whenIntroIsNullAndStoryNotInRegularChapters() {
    // Arrange
    StoryExecution story = createStory(Path.of("book/intro/story1.md"), "Story 1");
    ChapterExecution regularChapter =
        createChapter(
            Path.of("book/chapter1"),
            "Chapter 1",
            createStory(Path.of("book/chapter1/story2.md"), "Story 2"));
    StoryBookExecution book = createBook(null, List.of(regularChapter));

    // Act
    Optional<ChapterExecution> result = book.findChapterForStory(story);

    // Assert
    assertFalse(result.isPresent());
  }

  // Helper methods to create test data structures

  private StoryBookExecution createBook(ChapterExecution intro, List<ChapterExecution> chapters) {
    return new StoryBookExecution(Path.of("book"), "Test Book", intro, chapters);
  }

  private ChapterExecution createChapter(
      Path chapterPath, String title, StoryExecution... stories) {
    return new ChapterExecution(chapterPath, title, null, List.of(stories));
  }

  private ChapterExecution createChapterWithMultipleStories(
      Path chapterPath, String title, StoryExecution... stories) {
    return new ChapterExecution(chapterPath, title, null, List.of(stories));
  }

  private StoryExecution createStory(Path storyPath, String title) {
    return new StoryExecution(storyPath, title, null, List.of(), List.of());
  }
}
