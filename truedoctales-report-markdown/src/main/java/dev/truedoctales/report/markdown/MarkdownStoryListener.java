package dev.truedoctales.report.markdown;

import dev.truedoctales.api.execute.PersistStoryListener;
import dev.truedoctales.api.execute.StoryExecutionListener;
import java.io.IOException;
import java.nio.file.Path;

/// Generates a Book-of-Truth Markdown report from test execution results.
/// This listener collects execution results and delegates Markdown generation to specialized
/// classes.
/// The output is a folder structure suitable for GitHub Wiki or other documentation systems.
public class MarkdownStoryListener extends PersistStoryListener implements StoryExecutionListener {

  private static final Path OUTPUT_PATH = Path.of("target/book-of-truth/markdown");

  public MarkdownStoryListener() {}

  @Override
  public void closeBook() {
    try {
      new MarkdownContentGenerator(OUTPUT_PATH).generateReport(getBookResult());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
