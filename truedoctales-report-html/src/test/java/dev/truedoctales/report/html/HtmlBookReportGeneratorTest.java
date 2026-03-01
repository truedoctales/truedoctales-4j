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

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("<nav class=\"sidebar\""), "Shell should have sidebar navigation");
    assertTrue(shellHtml.contains("Book Intro"), "Shell nav should contain page title");
    assertTrue(shellHtml.contains("My Story"), "Shell nav should contain other pages");
    assertTrue(
        shellHtml.contains("01_chapter/01_story.html"), "Shell nav should link to chapter page");
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

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(shellHtml.contains("mermaid"), "Shell should include mermaid.js reference");
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

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("<link rel=\"stylesheet\" href=\"truedoctales.css\">"),
        "Shell should link to external CSS file");
    assertFalse(shellHtml.contains("<style>"), "Shell should not include inline CSS");

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

    // With SPA, active state is managed by the JS router, not baked into the HTML
    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("toggle('active'"),
        "Shell JS should manage active navigation state dynamically");
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

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(shellHtml.contains("viewport"), "Shell should include viewport meta tag");

    String css = Files.readString(htmlOutputDir.resolve("truedoctales.css"));
    assertTrue(css.contains("@media"), "CSS should include responsive media queries");
  }

  @Test
  void generate_shouldIncludeProjectIcon() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("<img src=\"small_icon_full.png\""),
        "Shell should include project icon in top header");
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

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("<details class=\"nav-tree\""),
        "Shell nav should use <details> for collapsible chapter groups");
    assertTrue(
        shellHtml.contains("<summary>"), "Shell nav chapter groups should have a <summary> toggle");
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

    // With SPA, all groups start collapsed; the JS router opens the active one at runtime
    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertFalse(
        shellHtml.contains("<details class=\"nav-tree\" open"),
        "Chapter groups should be collapsed by default in the SPA shell");
  }

  @Test
  void generate_shellShouldUseRootLevelAssetPaths() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Story\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    // The SPA shell always lives at the output root, so no depth prefix is needed
    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("href=\"truedoctales.css\""),
        "SPA shell should reference CSS at root level");
    assertTrue(
        shellHtml.contains("src=\"small_icon_full.png\""),
        "SPA shell should reference icon at root level");
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

    // With SPA, the JS router opens the chapter group that contains the active page at runtime
    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("d.open = true"),
        "Shell JS should expand the chapter group that contains the active page");
  }

  @Test
  void generate_shouldIncludeFavicon() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("<link rel=\"icon\" type=\"image/png\" href=\"small_icon_full.png\">"),
        "Shell should include favicon link");
  }

  @Test
  void generate_shellFaviconAtRootLevel() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Book Intro\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Story\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    // The SPA shell is always at the root; no depth prefix required for the favicon
    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("href=\"small_icon_full.png\""),
        "SPA shell should reference favicon at root level without depth prefix");
  }

  @Test
  void generate_shouldIncludeBrandedFooter() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("class=\"report-footer\""), "Shell should include branded footer");
    assertTrue(shellHtml.contains("truedoctales-4j"), "Footer should link to project repository");
  }

  @Test
  void generate_shouldIncludeBootstrapCssFramework() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("bootstrap"), "Shell should include Bootstrap framework from CDN");
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

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(shellHtml.contains("class=\"top-header\""), "Shell should include a top header bar");
    assertTrue(
        shellHtml.contains("class=\"top-header-brand\""),
        "Shell should include brand link in top header");
  }

  @Test
  void generate_shouldIncludeThemeToggle() throws IOException {
    Files.writeString(markdownDir.resolve("intro.md"), "# Intro\n\nHello.");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("id=\"theme-toggle\""), "Shell should include a theme toggle button");
    assertTrue(
        shellHtml.contains("data-bs-theme"),
        "Shell should use Bootstrap data-bs-theme attribute for theming");
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

    // With SPA, the title appears in the shell nav; the story fragment contains the markdown
    // heading
    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("Meta Chapter Title"), "Shell nav should use title from meta.json");
  }

  @Test
  void generate_shouldFallBackToMarkdownTitleWhenNoMetaJson() throws IOException {
    Files.writeString(markdownDir.resolve("00_intro.md"), "# Markdown Title\n");
    Path ch = Files.createDirectories(markdownDir.resolve("01_chapter"));
    Files.writeString(ch.resolve("01_story.md"), "# Markdown Story Title\n");

    HtmlBookReportGenerator generator = new HtmlBookReportGenerator(markdownDir, htmlOutputDir);
    generator.generate();

    // The story title falls back to the markdown heading and appears in the shell nav
    String shellHtml = Files.readString(htmlOutputDir.resolve("index.html"));
    assertTrue(
        shellHtml.contains("Markdown Story Title"),
        "Shell nav should fall back to markdown heading when no meta.json exists");
  }
}
