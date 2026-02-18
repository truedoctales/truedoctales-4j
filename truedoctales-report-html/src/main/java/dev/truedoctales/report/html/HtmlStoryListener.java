package dev.truedoctales.report.html;

import dev.truedoctales.api.execute.PersistStoryListener;
import dev.truedoctales.api.execute.StoryExecutionListener;
import java.io.IOException;
import java.nio.file.Path;

/// Generates a Book-of-Truth HTML report from test execution results.
/// This listener collects execution results and delegates HTML generation to specialized classes.
public class HtmlStoryListener extends PersistStoryListener implements StoryExecutionListener {

  private static final Path OUTPUT_PATH = Path.of("target/book-of-truth/html");

  public HtmlStoryListener() {}

  @Override
  public void closeBook() {
    try {
      new HtmlContentGenerator(OUTPUT_PATH).generateReport(getBookResult());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
