package dev.truedoctales.api.model.execution;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truedoctales.api.model.story.StepCall;
import dev.truedoctales.api.model.story.StepTask;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoryExecutionTest {

  private static final Path STORY_PATH = Path.of("chapters", "chapter-01", "tailor.md");

  @Test
  void shouldExposeProvidedValues() {
    StoryExecution story = createStory("A brave little tailor");

    assertEquals(STORY_PATH, story.path());
    assertEquals("Brave Little Tailor", story.title());
    assertEquals("A brave little tailor", story.summary());
    assertEquals(List.of(Path.of("prequels", "origin.md")), story.prequels());
    assertEquals(1, story.scenes().size());
    assertEquals("Opening Scene", story.scenes().getFirst().title());
  }

  @Test
  void shouldAllowNullSummery() {
    StoryExecution story = createStory(null);

    assertNull(story.summary());
  }

  @Test
  void shouldImplementValueEquality() {
    StoryExecution first = createStory("Ending A");
    StoryExecution second = createStory("Ending A");
    StoryExecution different = createStory("Ending B");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertNotEquals(first, different);
  }

  @Test
  void shouldRoundTripWithJackson() throws Exception {
    StoryExecution story = createStory("Story for serialization");
    ObjectMapper mapper = new ObjectMapper();

    String json = mapper.writeValueAsString(story);
    StoryExecution restored = mapper.readValue(json, StoryExecution.class);

    assertEquals(story.title(), restored.title());
    assertEquals(story.summary(), restored.summary());
    assertTrue(restored.path().endsWith(STORY_PATH));
    assertTrue(restored.prequels().getFirst().endsWith(Path.of("prequels", "origin.md")));
    assertEquals(story.scenes(), restored.scenes());
  }

  private StoryExecution createStory(String summary) {
    List<Path> prequels = List.of(Path.of("prequels", "origin.md"));
    List<SceneExecution> scenes = List.of(createScene());
    return new StoryExecution(STORY_PATH, "Brave Little Tailor", summary, prequels, scenes);
  }

  private SceneExecution createScene() {
    StepBinding binding = new StepBinding("Tailor Plot", "defeat the giants");
    StepCall call = new StepCall("Tailor Plot", "defeat the giants");
    Map<String, String> row = Map.of("hero", "Tailor", "giants", "2");
    StepExecution execution = new StepExecution(binding, call, List.of(row), 42);
    StepTask task = new StepTask(42, call, List.of(row));
    return new SceneExecution(
        "Opening Scene", 1, "The tailor arrives", List.of(execution), List.of(task));
  }
}
