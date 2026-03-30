package dev.truedoctales.parser;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.story.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoryBookParserImplTest {

  @Test
  void parse_shouldThrowExceptionForNonExistentPath() throws IOException {
    // Arrange
    Path bookDir = Path.of("target/test-book");
    bookDir.toFile().mkdirs(); // Ensure the directory exists but is empty

    File intro =
        bookDir.resolve(Paths.get("00_intro.md")).toFile(); // Ensure intro.md does not exist
    Files.write(intro.toPath(), List.of("# Test Book"));

    bookDir.resolve(Paths.get("01_chapter")).toFile().mkdirs();

    File chapterIntro =
        bookDir
            .resolve(Paths.get("01_chapter/00_intro.md"))
            .toFile(); // Ensure intro.md does not exist
    Files.write(chapterIntro.toPath(), List.of("# Test Chapter"));

    File story =
        bookDir
            .resolve(Paths.get("01_chapter/01_story.md"))
            .toFile(); // Ensure intro.md does not exist
    Files.write(story.toPath(), List.of("# Test Story"));

    StoryBookParserImpl parserImpl = new StoryBookParserImpl(bookDir);

    StoryBookModel bookModel = parserImpl.parse();
    assertNotNull(bookModel);
    assertEquals("Test Book", bookModel.title());
    ChapterModel chapterModel = bookModel.chapters().getFirst();
    assertEquals("Test Chapter", chapterModel.title());
    StoryModel storyModel = chapterModel.stories().getFirst();
    assertEquals("Test Story", storyModel.title());

    Files.walk(bookDir)
        .sorted(Comparator.reverseOrder())
        .forEach(
            path -> {
              try {
                Files.deleteIfExists(path);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
