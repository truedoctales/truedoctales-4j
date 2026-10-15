package dev.truedoctales.sample.quarkus;

import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.story.StoryBookModel;
import dev.truedoctales.execution.jupiter.JupiterStoryTestExecutor;
import dev.truedoctales.parser.StoryBookParserImpl;
import dev.truedoctales.report.json.JsonStoryListener;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

@QuarkusTest
class QuarkusStoryBookTest {

  private static final Path BOOK_PATH = Path.of("src/test/resources/quarkus-doc-tales");

  @Inject PlotRegistry plotRegistry;

  @TestFactory
  Stream<DynamicNode> runStory() throws IOException {
    StoryBookModel book = new StoryBookParserImpl(BOOK_PATH).parse();
    StoryExecutionListener listener = new JsonStoryListener();
    listener.startBook(book);
    JupiterStoryTestExecutor executor = new JupiterStoryTestExecutor(plotRegistry, listener);
    return book.chapters().stream()
        .flatMap(chapter -> chapter.stories().stream())
        .flatMap(story -> executor.buildDynamicTests(book, story.path()))
        .onClose(listener::closeBook);
  }
}
