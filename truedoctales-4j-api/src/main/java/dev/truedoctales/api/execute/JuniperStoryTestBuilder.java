package dev.truedoctales.api.execute;

import dev.truedoctales.api.model.story.StoryBookModel;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;

/// Interface for building JUnit Jupiter dynamic tests from story executions.
///
/// Implementations convert story book executions into JUnit dynamic test nodes.
public interface JuniperStoryTestBuilder {
  /// Builds dynamic test nodes for the given story book.
  ///
  /// @param book the story book execution
  /// @param storyPath the path to the specific story
  /// @return stream of dynamic test nodes
  /// @throws Exception if test building fails
  Stream<DynamicNode> buildDynamicTests(StoryBookModel book, Path storyPath) throws Exception;
}
