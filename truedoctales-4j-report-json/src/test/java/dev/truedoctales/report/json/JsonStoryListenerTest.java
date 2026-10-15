package dev.truedoctales.report.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonStoryListenerTest {

  @TempDir Path tempDir;

  @Test
  void closeBook_writesMavenProjectVersionToBookMetadata() throws Exception {
    JsonStoryListener listener = new JsonStoryListener(tempDir.resolve("report"));
    listener.startBook(new StoryBookModel(tempDir, "Test book", null, List.of()));
    listener.startChapter(new ChapterModel(1, Path.of("01_chapter"), "Chapter", List.of()));
    listener.getBookResult().getChapters().getFirst().setStories(List.of());

    listener.closeBook();

    JsonNode metadata = new ObjectMapper().readTree(tempDir.resolve("report/meta.json").toFile());
    assertEquals(
        System.getProperty("truedoctales.project.version"), metadata.get("version").asText());
  }
}
