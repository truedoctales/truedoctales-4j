package dev.truedoctales.parser;

import dev.truedoctales.api.internal.parsing.StoryBookParser;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import dev.truedoctales.api.model.story.StoryModel;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parser for story book directory structures.
 *
 * <p>This parser:
 *
 * <ul>
 *   <li>Scans book directory for chapter subdirectories
 *   <li>Identifies prequel chapters by {@code 00_} prefix convention
 *   <li>Parses all markdown files within each chapter
 *   <li>Parses {@code 00_intro.md} files at book and chapter levels
 *   <li>Builds complete {@link StoryBookModel} representation
 * </ul>
 *
 * <h3>Directory Structure</h3>
 *
 * <pre>
 * book-of-stories/
 *   00_intro.md              (book title and summary)
 *   01_chapter-name/
 *     00_intro.md            (chapter title and summary)
 *     01_story.md
 *     02_another-story.md
 * </pre>
 *
 * <p><strong>Modern Java 25 Features:</strong>
 *
 * <ul>
 *   <li>Stream API with try-with-resources
 *   <li>Optional for null-safe handling
 *   <li>Functional error handling with UncheckedIOException
 *   <li>Immutable collections with List.copyOf()
 * </ul>
 */
public final class StoryBookParserImpl implements StoryBookParser {

  private static final Logger LOGGER = Logger.getLogger(StoryBookParserImpl.class.getName());
  private static final String PREQUEL_PREFIX = "00_";
  private static final String INTRO_FILE = "00_intro.md";

  private final Path bookPath;
  private final MarkdownStoryParser storyParser;
  private final IntroMarkdownParser introParser;

  /**
   * Creates a new story book parser.
   *
   * @param bookPath the root path of the story book
   */
  public StoryBookParserImpl(Path bookPath) {
    this.bookPath = bookPath;
    this.storyParser = new MarkdownStoryParserImpl();
    this.introParser = new IntroMarkdownParser();
  }

  @Override
  public StoryBookModel parse() throws IOException {
    validateBookPath();

    LOGGER.info(() -> "Parsing story book: " + bookPath);

    // Parse book-level intro
    var bookIntro = parseBookIntro();

    // Parse all chapters
    var allChapters = parseAllChapters();
    var prequelChapter = extractPrequelChapter(allChapters);
    var chapters = filterRegularChapters(allChapters);

    return new StoryBookModel(
        bookPath,
        bookIntro.map(IntroMarkdownParser.IntroContent::title).orElse("Book of Stories"),
        bookIntro.map(IntroMarkdownParser.IntroContent::summary).orElse(null),
        prequelChapter.orElse(null),
        chapters);
  }

  // ===== Private Implementation =====

  private void validateBookPath() throws IOException {
    if (!Files.exists(bookPath)) {
      throw new IOException("Book path does not exist: " + bookPath.toAbsolutePath());
    }
    if (!Files.isDirectory(bookPath)) {
      throw new IOException("Book path is not a directory: " + bookPath);
    }
  }

  private Optional<IntroMarkdownParser.IntroContent> parseBookIntro() {
    Path introPath = bookPath.resolve(INTRO_FILE);
    try {
      var intro = introParser.parse(introPath);
      if (intro != null) {
        LOGGER.info(() -> "Parsed book intro: " + intro.title());
      }
      return Optional.ofNullable(intro);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to parse book intro: " + introPath, e);
      return Optional.empty();
    }
  }

  private List<ChapterModel> parseAllChapters() throws IOException {
    try (Stream<Path> dirStream = Files.list(bookPath)) {
      return dirStream.filter(Files::isDirectory).sorted().map(this::parseChapter).toList();
    }
  }

  private Optional<ChapterModel> extractPrequelChapter(List<ChapterModel> chapters) {
    return chapters.stream().filter(chapter -> isPrequelChapter(chapter.path())).findFirst();
  }

  private List<ChapterModel> filterRegularChapters(List<ChapterModel> chapters) {
    return chapters.stream().filter(chapter -> !isPrequelChapter(chapter.path())).toList();
  }

  private boolean isPrequelChapter(Path path) {
    return path.getFileName().toString().startsWith(PREQUEL_PREFIX);
  }

  /** Parses a single chapter directory. */
  private ChapterModel parseChapter(Path chapterPath) {
    Path relativePath = bookPath.relativize(chapterPath);
    String chapterName = relativePath.getFileName().toString();

    LOGGER.info(() -> "Parsing chapter: " + chapterName);

    // Parse chapter intro
    var chapterIntro = parseChapterIntro(chapterPath);

    // Parse all story files in chapter
    var stories = parseStories(chapterPath);

    return new ChapterModel(
        relativePath.getFileName(),
        chapterIntro.map(IntroMarkdownParser.IntroContent::title).orElse(chapterName),
        chapterIntro.map(IntroMarkdownParser.IntroContent::summary).orElse(null),
        stories);
  }

  private Optional<IntroMarkdownParser.IntroContent> parseChapterIntro(Path chapterPath) {
    Path introPath = chapterPath.resolve(INTRO_FILE);
    try {
      var intro = introParser.parse(introPath);
      if (intro != null) {
        LOGGER.info(() -> "Parsed chapter intro: " + intro.title());
      }
      return Optional.ofNullable(intro);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to parse chapter intro: " + introPath, e);
      return Optional.empty();
    }
  }

  private List<StoryModel> parseStories(Path chapterPath) {
    try (Stream<Path> fileStream = Files.list(chapterPath)) {
      return fileStream.filter(this::isStoryFile).sorted().map(this::parseStory).toList();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to list stories in chapter: " + chapterPath, e);
      throw new UncheckedIOException(e);
    }
  }

  private boolean isStoryFile(Path path) {
    return Files.isRegularFile(path)
        && path.toString().endsWith(".md")
        && !path.getFileName().toString().equals(INTRO_FILE);
  }

  private StoryModel parseStory(Path storyFile) {
    try {
      Path relativeStoryPath = bookPath.relativize(storyFile);
      LOGGER.info(() -> "Parsing story: " + relativeStoryPath);
      return storyParser.parse(bookPath, relativeStoryPath);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to parse story: " + storyFile, e);
      throw new UncheckedIOException(e);
    }
  }
}
