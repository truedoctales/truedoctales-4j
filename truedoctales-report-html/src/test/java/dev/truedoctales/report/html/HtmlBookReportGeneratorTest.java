package dev.truedoctales.report.html;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlBookReportGeneratorTest {

  @TempDir Path tempDir;

  private Path markdownDir;
  private Path htmlOutputDir;

  @BeforeEach
  void setUp() throws IOException {
    markdownDir = tempDir.resolve("markdown");
    htmlOutputDir = tempDir.resolve("html");
    Files.createDirectories(markdownDir);
  }

  @Test
  void generate_shouldCreateHtmlFilesFromMarkdown() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Introduction\n\nWelcome.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    assertTrue(Files.exists(htmlOutputDir.resolve("00_intro.html")));
    assertTrue(Files.exists(htmlOutputDir.resolve("index.html")));
  }

  @Test
  void generate_shouldCreateHtmlWithNavigation() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n\nWelcome.");
    Path chapterDir = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(chapterDir.resolve("01_story.md"), "# My Story\n\nContent.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String introHtml = Files.readString(htmlOutputDir.resolve("00_intro.html"));
    assertTrue(introHtml.contains("<nav class=\"sidebar\""), "Should have sidebar navigation");
    assertTrue(introHtml.contains("Book Intro"), "Should contain page title in navigation");
    assertTrue(introHtml.contains("My Story"), "Should contain other pages in navigation");
    assertTrue(introHtml.contains("01_chapter/01_story.html"), "Should link to chapter page");
  }

  @Test
  void generate_shouldRenderMarkdownAsHtml() throws IOException {
    String markdown =
        """
        # Story Title

        ## Scene: First scene

        > **Greeting** Say Hello ✅

        Some **bold** text and *italic* text.
        """;
    Files.writeString(markdownDir.resolve("story.md"), markdown);

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("story.html"));
    assertTrue(html.contains("<h1>Story Title</h1>"), "Should render h1");
    assertTrue(html.contains("<h2>Scene: First scene</h2>"), "Should render h2");
    assertTrue(html.contains("<strong>bold</strong>"), "Should render bold");
    assertTrue(html.contains("<em>italic</em>"), "Should render italic");
  }

  @Test
  void generate_shouldIncludeMermaidJs() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(html.contains("mermaid"), "Should include mermaid.js reference");
  }

  @Test
  void generate_shouldRenderMermaidDiagrams() throws IOException {
    String markdown =
        """
        # Diagram Page

        ```mermaid
        graph TD
            A[Start] --> B[End]
        ```
        """;
    Files.writeString(markdownDir.resolve("diagram.md"), markdown);

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("diagram.html"));
    assertTrue(html.contains("<div class=\"mermaid\">"), "Should render mermaid as div");
    assertTrue(html.contains("A[Start]"), "Should contain mermaid diagram content");
    assertFalse(html.contains("```mermaid"), "Should not contain raw markdown mermaid fence");
  }

  @Test
  void generate_shouldIncludeProfessionalCssStyling() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(
        html.contains("<link rel=\"stylesheet\" href=\"truedoctales.css\">"),
        "Should link to external CSS file");
    assertFalse(html.contains("<style>"), "Should not include inline CSS");

    assertTrue(
        Files.exists(htmlOutputDir.resolve("truedoctales.css")),
        "Should write CSS file to output directory");
    String css = Files.readString(htmlOutputDir.resolve("truedoctales.css"));
    assertTrue(css.contains("--primary"), "CSS should include custom properties");
    assertTrue(css.contains(".sidebar"), "CSS should include sidebar styles");
    assertTrue(css.contains(".content"), "CSS should include content styles");
  }

  @Test
  void generate_shouldHandleChapterStructure() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch1 = Files.createDirectories(markdownDir.resolve("01_chapter-basics"));
    Files.writeString(ch1.resolve("00_intro.md"), "# Chapter 1 Intro\n");
    Files.writeString(ch1.resolve("01_story.md"), "# First Story\n");
    Path ch2 = Files.createDirectories(markdownDir.resolve("02_chapter-advanced"));
    Files.writeString(ch2.resolve("00_intro.md"), "# Chapter 2 Intro\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    assertTrue(Files.exists(htmlOutputDir.resolve("00_intro.html")));
    assertTrue(Files.exists(htmlOutputDir.resolve("01_chapter-basics/00_intro.html")));
    assertTrue(Files.exists(htmlOutputDir.resolve("01_chapter-basics/01_story.html")));
    assertTrue(Files.exists(htmlOutputDir.resolve("02_chapter-advanced/00_intro.html")));
    assertTrue(Files.exists(htmlOutputDir.resolve("index.html")));

    // Check that the index.html redirects to first page
    String index = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(index.contains("00_intro.html"), "Index should redirect to first page");
  }

  @Test
  void generate_shouldHighlightActiveNavigationEntry() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Intro\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Story\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String storyHtml = Files.readString(htmlOutputDir.resolve("01_chapter/01_story.html"));
    assertTrue(storyHtml.contains("class=\"active\""), "Should mark active entry");
  }

  @Test
  void generate_shouldCopyNonMarkdownFiles() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Intro\n");
    Files.writeString(markdownDir.resolve("image.png"), "fake-image-data");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    assertTrue(Files.exists(htmlOutputDir.resolve("image.png")), "Should copy non-md files");
    assertFalse(Files.exists(htmlOutputDir.resolve("00_intro.md")), "Should not copy md files");
  }

  @Test
  void generate_shouldCopyBookRootAssetFolder() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Intro\n");
    Path assetsDir = Files.createDirectories(markdownDir.resolve("assets"));
    Files.writeString(assetsDir.resolve("icon.png"), "fake-icon-data");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    assertTrue(
        Files.exists(htmlOutputDir.resolve("assets/icon.png")),
        "Should copy asset folder at book root level");
  }

  @Test
  void generate_shouldCopyChapterLevelAssetFolder() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Intro\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Story\n");
    Path chAssets = Files.createDirectories(ch.resolve("assets"));
    Files.writeString(chAssets.resolve("diagram.png"), "fake-diagram-data");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    assertTrue(
        Files.exists(htmlOutputDir.resolve("01_chapter/assets/diagram.png")),
        "Should copy asset folder at chapter level");
  }

  @Test
  void generate_shouldHandleEmptyDirectory() throws IOException {
    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);

    assertDoesNotThrow(generator::generate);
  }

  @Test
  void generate_shouldHandleMissingSourceDirectory() {
    HtmlBookReportGenerator generator =
        new HtmlBookReportGenerator(tempDir.resolve("nonexistent"), htmlOutputDir);

    assertDoesNotThrow(generator::generate);
  }

  @Test
  void convertMarkdownToHtml_shouldHandleTables() {
    String markdown =
        """
        | id | name   |
        |----|--------|
        | 1  | Tailor |
        """;

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    String html = generator.convertMarkdownToHtml(markdown);

    assertTrue(html.contains("<table>"), "Should render table");
  }

  @Test
  void generate_shouldIncludeResponsiveDesign() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(html.contains("viewport"), "Should include viewport meta tag");

    String css = Files.readString(htmlOutputDir.resolve("truedoctales.css"));
    assertTrue(css.contains("@media"), "CSS should include responsive media queries");
  }

  @Test
  void generate_shouldIncludeProjectIcon() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(
        html.contains("<img src=\"small_icon_full.png\""),
        "Should include project icon in top header");
    assertTrue(
        Files.exists(htmlOutputDir.resolve("small_icon_full.png")),
        "Should write icon file to output directory");
  }

  @Test
  void generate_shouldUseCollapsibleTreeForChapterGroups() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter-basics"));
    Files.writeString(ch.resolve("00_intro.md"), "# Chapter 1\n");
    Files.writeString(ch.resolve("01_story.md"), "# Story\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String introHtml = Files.readString(htmlOutputDir.resolve("00_intro.html"));
    assertTrue(
        introHtml.contains("<details class=\"nav-tree\""),
        "Chapter groups should use <details> for collapsible tree");
    assertTrue(
        introHtml.contains("<summary>"), "Chapter groups should have a <summary> toggle label");
  }

  @Test
  void generate_shouldCollapseChapterGroupsByDefault() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch1 = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch1.resolve("01_story.md"), "# Story 1\n");
    Path ch2 = Files.createDirectories(markdownDir.resolve("02_chapter"));
    Files.writeString(ch2.resolve("01_story.md"), "# Story 2\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    // When viewing the intro (root), no chapter groups should be open
    String introHtml = Files.readString(htmlOutputDir.resolve("00_intro.html"));
    assertFalse(
        introHtml.contains("<details class=\"nav-tree\" open"),
        "Chapter groups should be collapsed by default when not active");
  }

  @Test
  void generate_shouldUseCssDepthPrefixForNestedPages() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Story\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String storyHtml = Files.readString(htmlOutputDir.resolve("01_chapter/01_story.html"));
    assertTrue(
        storyHtml.contains("href=\"../truedoctales.css\""),
        "Nested pages should use relative path to CSS");
    assertTrue(
        storyHtml.contains("src=\"../small_icon_full.png\""),
        "Nested pages should use relative path to icon");
  }

  @Test
  void convertMarkdownToHtml_shouldRewriteMdLinksToHtml() {
    String markdown = "See [Setup](setup.md) and [Data](../shared/data.md) for details.";

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    String html = generator.convertMarkdownToHtml(markdown);

    assertTrue(html.contains("href=\"setup.html\""), "Should rewrite .md link to .html");
    assertTrue(
        html.contains("href=\"../shared/data.html\""),
        "Should rewrite .md link with path to .html");
    assertFalse(html.contains(".md\""), "Should not contain any .md link references");
  }

  @Test
  void convertMarkdownToHtml_shouldRewritePrequelBlockLinks() {
    String markdown =
        """
        > Prequels
        > - [Setup Users](setup-users.md)
        > - [Setup Data](setup-data.md)
        """;

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    String html = generator.convertMarkdownToHtml(markdown);

    assertTrue(
        html.contains("href=\"setup-users.html\""), "Should rewrite prequel .md link to .html");
    assertTrue(
        html.contains("href=\"setup-data.html\""), "Should rewrite prequel .md link to .html");
  }

  @Test
  void convertMarkdownToHtml_shouldNotRewriteNonMdLinks() {
    String markdown = "Visit [Google](https://google.com) or see [Image](photo.png).";

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    String html = generator.convertMarkdownToHtml(markdown);

    assertTrue(html.contains("href=\"https://google.com\""), "Should preserve external links");
    assertTrue(html.contains("href=\"photo.png\""), "Should preserve non-md links");
  }

  @Test
  void generate_shouldExpandChapterGroupContainingActivePage() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch1 = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch1.resolve("01_story.md"), "# Story 1\n");
    Path ch2 = Files.createDirectories(markdownDir.resolve("02_chapter"));
    Files.writeString(ch2.resolve("01_story.md"), "# Story 2\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    // When viewing a story in chapter 1, that chapter group should be expanded
    String storyHtml = Files.readString(htmlOutputDir.resolve("01_chapter/01_story.html"));
    assertTrue(
        storyHtml.contains("<details class=\"nav-tree\" open"),
        "Chapter group containing active page should be expanded");
  }

  @Test
  void generate_shouldIncludeFavicon() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(
        html.contains("<link rel=\"icon\" type=\"image/png\" href=\"small_icon_full.png\">"),
        "Should include favicon link");
  }

  @Test
  void generate_shouldIncludeFaviconWithDepthPrefix() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Story\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String storyHtml = Files.readString(htmlOutputDir.resolve("01_chapter/01_story.html"));
    assertTrue(
        storyHtml.contains("href=\"../small_icon_full.png\""),
        "Nested pages should use relative path to favicon");
  }

  @Test
  void generate_shouldIncludeBrandedFooter() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(html.contains("class=\"report-footer\""), "Should include branded footer");
    assertTrue(html.contains("truedoctales-4j"), "Footer should link to project repository");
  }

  @Test
  void generate_shouldIncludePicoCssFramework() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(html.contains("picocss/pico"), "Should include Pico CSS framework from CDN");
  }

  @Test
  void generate_cssShouldIncludePrintStyles() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String css = Files.readString(htmlOutputDir.resolve("truedoctales.css"));
    assertTrue(css.contains("@media print"), "CSS should include print styles");
  }

  @Test
  void generate_shouldIncludeTopHeaderBar() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(html.contains("class=\"top-header\""), "Should include a top header bar");
    assertTrue(
        html.contains("class=\"top-header-brand\""), "Should include brand link in top header");
  }

  @Test
  void generate_shouldIncludeThemeToggle() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String html = Files.readString(htmlOutputDir.resolve("intro.html"));
    assertTrue(html.contains("id=\"theme-toggle\""), "Should include a theme toggle button");
    assertTrue(html.contains("data-theme"), "Should use Pico data-theme attribute for theming");
  }

  @Test
  void generate_shouldUseTitleFromMetaJsonWhenPresent() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Markdown Title\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Markdown Story Title\n");
    Files.writeString(
        ch.resolve("meta.json"), "{\"path\": \"01_chapter\", \"title\": \"Meta Chapter Title\"}");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String storyHtml = Files.readString(htmlOutputDir.resolve("01_chapter/01_story.html"));
    assertTrue(
        storyHtml.contains("Meta Chapter Title"),
        "Should use title from meta.json instead of parsing markdown heading");
  }

  @Test
  void generate_shouldFallBackToMarkdownTitleWhenNoMetaJson() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Markdown Title\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Markdown Story Title\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String storyHtml = Files.readString(htmlOutputDir.resolve("01_chapter/01_story.html"));
    assertTrue(
        storyHtml.contains("Markdown Story Title"),
        "Should fall back to markdown heading when no meta.json exists");
  }
}
