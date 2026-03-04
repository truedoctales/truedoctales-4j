package dev.truedoctales.report.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/// Generates a Bootstrap-based single-page-application HTML report from enriched markdown files.
///
/// <p>The generator produces one {@code index.html} SPA shell (Bootstrap navbar + sidebar +
/// JS router) and one lightweight {@code <article>} HTML fragment per story. Navigation links
/// use hash-based routing so the browser never leaves {@code index.html}; content is fetched
/// and injected on demand without a full page reload.
///
/// <p>When a {@code jsonReportDirectory} is supplied the navigation structure is read directly
/// from the JSON report files (chapter {@code meta.json} + per-story {@code .json} files) and
/// serialised into a single {@code report-nav.json} file that is loaded by the SPA at startup.
/// This avoids re-parsing the markdown files just to extract titles and chapter structure.
///
/// <p>Mermaid diagrams are rendered client-side via the mermaid.js library.
public class HtmlBookReportGenerator {

  private static final Logger logger = Logger.getLogger(HtmlBookReportGenerator.class.getName());

  private static final String CSS_RESOURCE = "truedoctales.css";
  private static final String ICON_RESOURCE = "small_icon_full.png";

  private final Path markdownDirectory;
  private final Path jsonReportDirectory;
  private final Path htmlOutputDirectory;
  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;

  /// Creates a new HTML report generator that reads navigation from the JSON report directory.
  ///
  /// @param markdownDirectory directory containing enriched markdown files (used for HTML content)
  /// @param jsonReportDirectory directory containing the JSON execution report (used for
  // navigation)
  /// @param htmlOutputDirectory directory where the HTML report will be written
  public HtmlBookReportGenerator(
      Path markdownDirectory, Path jsonReportDirectory, Path htmlOutputDirectory) {
    this.markdownDirectory = markdownDirectory;
    this.jsonReportDirectory = jsonReportDirectory;
    this.htmlOutputDirectory = htmlOutputDirectory;
    List<Extension> extensions = List.of(TablesExtension.create());
    this.markdownParser = Parser.builder().extensions(extensions).build();
    this.htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
  }

  /// Creates a new HTML report generator without a JSON report directory.
  ///
  /// Navigation is derived from the markdown files themselves (legacy behaviour).
  ///
  /// @param markdownDirectory directory containing enriched markdown files
  /// @param htmlOutputDirectory directory where the HTML report will be written
  public HtmlBookReportGenerator(Path markdownDirectory, Path htmlOutputDirectory) {
    this(markdownDirectory, null, htmlOutputDirectory);
  }

  // -------------------------------------------------------------------------
  // Public entry point
  // -------------------------------------------------------------------------

  /// Generates the full HTML report with navigation.
  ///
  /// <p>Each markdown file is converted to a lightweight {@code <article>} HTML fragment.
  /// A single {@code index.html} SPA shell contains the full Bootstrap layout and a
  /// JavaScript router that fetches and injects the fragments on demand.
  /// Navigation data is emitted as {@code report-nav.json} and loaded by the SPA at startup.
  ///
  /// @throws IOException if reading or writing files fails
  public void generate() throws IOException {
    logger.info("Generating HTML report from: " + markdownDirectory);
    Files.createDirectories(htmlOutputDirectory);

    List<NavEntry> navigation = buildNavigation();
    copyNonMarkdownFiles();
    copyStaticAssets();

    for (NavEntry entry : navigation) {
      Path mdFile = entry.sourcePath();
      if (mdFile != null && Files.isRegularFile(mdFile)) {
        String markdown = Files.readString(mdFile);
        String bodyHtml = convertMarkdownToHtml(markdown);
        String fragmentHtml = buildFragmentHtml(bodyHtml);
        Path outputPath = htmlOutputDirectory.resolve(entry.htmlRelativePath());
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, fragmentHtml);
        logger.info("  HTML: " + entry.htmlRelativePath());
      }
    }

    if (!navigation.isEmpty()) {
      writeReportNavJson(navigation);
      generateIndexHtml(navigation);
    }
    logger.info("HTML report generated in: " + htmlOutputDirectory);
  }

  // -------------------------------------------------------------------------
  // Navigation building
  // -------------------------------------------------------------------------

  /// Builds the flat list of {@link NavEntry} objects that drive both the HTML fragment
  /// generation and the navigation JSON.
  ///
  /// <p>When a {@code jsonReportDirectory} is available the hierarchy is read from the JSON
  /// files.  Otherwise the markdown directory is walked as before.
  private List<NavEntry> buildNavigation() throws IOException {
    if (jsonReportDirectory != null && Files.isDirectory(jsonReportDirectory)) {
      return buildNavigationFromJson();
    }
    return buildNavigationFromMarkdown();
  }

  /// Walks the JSON report directory and builds {@link NavEntry} objects from
  /// chapter {@code meta.json} and per-story {@code .json} files.
  ///
  /// <p>Structure expected:
  /// <pre>
  /// jsonReportDirectory/
  ///   meta.json                            (book-level: title, hasIntro)
  ///   00_prequels/
  ///     meta.json                          (chapter metadata)
  ///     01_create-heroes.json              (prequel story result)
  ///   01_chapter-name/
  ///     meta.json                          (chapter metadata: number, path, title)
  ///     01_story-name.json                 (story result: number, path, title, …)
  /// </pre>
  private List<NavEntry> buildNavigationFromJson() throws IOException {
    List<NavEntry> entries = new ArrayList<>();

    // 1. Book-level intro (00_intro.md at the markdown root)
    String hasIntroStr = readJsonField(jsonReportDirectory.resolve("meta.json"), "hasIntro");
    boolean hasBookIntro = "true".equalsIgnoreCase(hasIntroStr);
    if (!hasBookIntro && markdownDirectory != null) {
      // Fallback: check the markdown dir directly even if meta.json doesn't say so
      hasBookIntro = Files.isRegularFile(markdownDirectory.resolve("00_intro.md"));
    }
    if (hasBookIntro && markdownDirectory != null) {
      Path introMd = markdownDirectory.resolve("00_intro.md");
      if (Files.isRegularFile(introMd)) {
        String introTitle = readJsonField(jsonReportDirectory.resolve("meta.json"), "title");
        if (introTitle == null) introTitle = extractTitleFromMarkdown(introMd);
        entries.add(
            new NavEntry(introMd, "00_intro.md", "00_intro.html", introTitle, false, null, "Book"));
      }
    }

    // 2. Chapter directories (sorted — 00_prequels comes first naturally)
    List<Path> chapterDirs = new ArrayList<>();
    try (var stream = Files.list(jsonReportDirectory)) {
      stream.filter(Files::isDirectory).sorted().forEach(chapterDirs::add);
    }

    for (Path chapterDir : chapterDirs) {
      String chapterTitle = readJsonField(chapterDir.resolve("meta.json"), "title");
      if (chapterTitle == null) {
        chapterTitle = toLabel(chapterDir.getFileName().toString());
      }

      String chapterDirName = chapterDir.getFileName().toString();
      Path introMd = findIntroMarkdown(chapterDirName);
      String introHtmlPath = introMd != null ? chapterDirName + "/00_intro.html" : null;

      if (introMd != null) {
        entries.add(
            new NavEntry(
                introMd,
                chapterDirName + "/00_intro.md",
                introHtmlPath,
                chapterTitle,
                true,
                chapterDirName,
                chapterTitle));
      }

      // Story JSON files (sorted, skip meta.json and any non-json)
      List<Path> storyJsonFiles = new ArrayList<>();
      try (var stream = Files.list(chapterDir)) {
        stream
            .filter(
                p ->
                    p.getFileName().toString().endsWith(".json")
                        && !p.getFileName().toString().equals("meta.json"))
            .sorted()
            .forEach(storyJsonFiles::add);
      }

      for (Path storyJson : storyJsonFiles) {
        String storyPathField = readJsonField(storyJson, "path");
        String storyTitle = readJsonField(storyJson, "title");
        if (storyTitle == null) {
          storyTitle = toLabel(storyJson.getFileName().toString().replace(".json", ""));
        }

        String mdRelativePath =
            storyPathField != null
                ? storyPathField
                : chapterDirName + "/" + storyJson.getFileName().toString().replace(".json", ".md");
        String htmlRelativePath = mdRelativePath.replaceAll("\\.md$", ".html");
        Path mdSource = resolveMarkdownSource(mdRelativePath);

        entries.add(
            new NavEntry(
                mdSource,
                mdRelativePath,
                htmlRelativePath,
                storyTitle,
                false,
                chapterDirName,
                chapterTitle));
      }
    }

    // Add plot glossary index + per-plot pages if plots/ directory exists in markdown output
    if (markdownDirectory != null) {
      Path glossaryMd = markdownDirectory.resolve("plot-glossary.md");
      if (Files.isRegularFile(glossaryMd)) {
        entries.add(
            new NavEntry(
                glossaryMd,
                "plot-glossary.md",
                "plot-glossary.html",
                "Plot Glossary",
                true,
                "plots",
                "Reference"));
      }
      Path plotsDir = markdownDirectory.resolve("plots");
      if (Files.isDirectory(plotsDir)) {
        try (var stream = Files.list(plotsDir)) {
          stream
              .filter(p -> p.getFileName().toString().endsWith(".md"))
              .sorted()
              .forEach(
                  plotMd -> {
                    String fileName = plotMd.getFileName().toString();
                    String plotId = fileName.replaceAll("\\.md$", "");
                    entries.add(
                        new NavEntry(
                            plotMd,
                            "plots/" + fileName,
                            "plots/" + plotId + ".html",
                            plotId,
                            false,
                            "plots",
                            "Reference"));
                  });
        }
      }
    }

    return entries;
  }

  /// Tries to find the {@code 00_intro.md} for a chapter in the markdown directory.
  private Path findIntroMarkdown(String chapterDirName) {
    if (markdownDirectory == null) return null;
    Path candidate = markdownDirectory.resolve(chapterDirName).resolve("00_intro.md");
    return Files.isRegularFile(candidate) ? candidate : null;
  }

  /// Resolves the actual markdown source file for a relative path.
  private Path resolveMarkdownSource(String mdRelativePath) {
    if (markdownDirectory == null) return null;
    Path candidate = markdownDirectory.resolve(mdRelativePath);
    return Files.isRegularFile(candidate) ? candidate : null;
  }

  /// Fallback: walk the markdown directory to collect navigation entries (no JSON available).
  private List<NavEntry> buildNavigationFromMarkdown() throws IOException {
    List<NavEntry> entries = new ArrayList<>();
    if (!Files.exists(markdownDirectory)) {
      return entries;
    }
    Files.walkFileTree(
        markdownDirectory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.toString().endsWith(".md")) {
              String relativePath = markdownDirectory.relativize(file).toString();
              String htmlPath = relativePath.replaceAll("\\.md$", ".html");
              // Title: prefer meta.json in the same directory, fall back to markdown heading
              String title = readJsonField(file.getParent().resolve("meta.json"), "title");
              if (title == null) {
                title = extractTitleFromMarkdown(file);
              }
              String chapterDir =
                  relativePath.contains("/")
                      ? relativePath.substring(0, relativePath.indexOf('/'))
                      : null;
              // Chapter label: prefer meta.json in chapter directory
              String chapterLabel;
              if (chapterDir != null) {
                String metaTitle =
                    readJsonField(
                        markdownDirectory.resolve(chapterDir).resolve("meta.json"), "title");
                chapterLabel = metaTitle != null ? metaTitle : toLabel(chapterDir);
              } else {
                chapterLabel = "Book";
              }
              boolean isIntro = isIntroPath(relativePath);
              entries.add(
                  new NavEntry(
                      file, relativePath, htmlPath, title, isIntro, chapterDir, chapterLabel));
            }
            return FileVisitResult.CONTINUE;
          }
        });
    entries.sort(Comparator.comparing(NavEntry::relativePath));
    return entries;
  }

  // -------------------------------------------------------------------------
  // report-nav.json
  // -------------------------------------------------------------------------

  /// Serialises the navigation entries into a {@code report-nav.json} file that the SPA
  /// loads at startup to build the sidebar without needing server-side rendering.
  private void writeReportNavJson(List<NavEntry> navigation) throws IOException {
    // Determine book title from JSON meta or fall back to directory name
    String bookTitle = readBookTitle();

    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"title\": ").append(jsonString(bookTitle)).append(",\n");
    sb.append("  \"defaultPage\": ")
        .append(jsonString(navigation.getFirst().htmlRelativePath()))
        .append(",\n");
    sb.append("  \"chapters\": [\n");

    // Group entries by chapter
    List<ChapterGroup> groups = groupByChapter(navigation);
    for (int gi = 0; gi < groups.size(); gi++) {
      ChapterGroup group = groups.get(gi);
      sb.append("    {\n");
      sb.append("      \"label\": ").append(jsonString(group.label())).append(",\n");
      sb.append("      \"dirName\": ")
          .append(jsonString(group.dirName() != null ? group.dirName() : ""))
          .append(",\n");
      if (group.introHtmlPath() != null) {
        sb.append("      \"introPage\": ").append(jsonString(group.introHtmlPath())).append(",\n");
      } else {
        sb.append("      \"introPage\": null,\n");
      }
      sb.append("      \"stories\": [\n");
      List<NavEntry> stories = group.stories();
      for (int si = 0; si < stories.size(); si++) {
        NavEntry e = stories.get(si);
        sb.append("        { \"title\": ")
            .append(jsonString(e.title()))
            .append(", \"htmlPath\": ")
            .append(jsonString(e.htmlRelativePath()))
            .append(" }");
        if (si < stories.size() - 1) sb.append(",");
        sb.append("\n");
      }
      sb.append("      ]\n");
      sb.append("    }");
      if (gi < groups.size() - 1) sb.append(",");
      sb.append("\n");
    }
    sb.append("  ]\n");
    sb.append("}\n");

    Files.writeString(htmlOutputDirectory.resolve("report-nav.json"), sb.toString());
    logger.info("  JSON nav: report-nav.json");
  }

  private String readBookTitle() {
    if (jsonReportDirectory != null) {
      String t = readJsonField(jsonReportDirectory.resolve("meta.json"), "title");
      if (t != null) return t;
    }
    return "True Doc Tales";
  }

  private List<ChapterGroup> groupByChapter(List<NavEntry> navigation) {
    List<ChapterGroup> groups = new ArrayList<>();
    String currentDir = null;
    String currentLabel = null;
    String currentIntro = null;
    List<NavEntry> currentStories = null;

    for (NavEntry e : navigation) {
      String dir = e.chapterDirName();
      if (!e.chapterLabel().equals(currentLabel)
          || (dir == null ? currentDir != null : !dir.equals(currentDir))) {
        if (currentLabel != null) {
          groups.add(
              new ChapterGroup(
                  currentLabel, currentDir, currentIntro, List.copyOf(currentStories)));
        }
        currentLabel = e.chapterLabel();
        currentDir = dir;
        currentIntro = null;
        currentStories = new ArrayList<>();
      }
      if (e.isChapterIntro()) {
        currentIntro = e.htmlRelativePath();
      } else {
        currentStories.add(e);
      }
    }
    if (currentLabel != null) {
      groups.add(
          new ChapterGroup(currentLabel, currentDir, currentIntro, List.copyOf(currentStories)));
    }
    return groups;
  }

  // -------------------------------------------------------------------------
  // Static file helpers
  // -------------------------------------------------------------------------

  private void copyNonMarkdownFiles() throws IOException {
    if (!Files.exists(markdownDirectory)) {
      return;
    }
    Files.walkFileTree(
        markdownDirectory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path target = htmlOutputDirectory.resolve(markdownDirectory.relativize(dir));
            Files.createDirectories(target);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (!file.toString().endsWith(".md")) {
              Path target = htmlOutputDirectory.resolve(markdownDirectory.relativize(file));
              Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private void copyStaticAssets() throws IOException {
    for (String resource : List.of(CSS_RESOURCE, ICON_RESOURCE)) {
      try (InputStream in = getClass().getResourceAsStream(resource)) {
        if (in != null) {
          Files.copy(
              in, htmlOutputDirectory.resolve(resource), StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  // -------------------------------------------------------------------------
  // Markdown / HTML conversion
  // -------------------------------------------------------------------------

  String convertMarkdownToHtml(String markdown) {
    String preprocessed = preprocessMermaid(markdown);
    Node document = markdownParser.parse(preprocessed);
    String html = htmlRenderer.render(document);
    html = rewriteMarkdownLinks(html);
    return postprocessMermaid(html);
  }

  /// Rewrites {@code .md} links in the rendered HTML to point to the corresponding {@code .html}
  /// files so that prequel and cross-story links work correctly in the HTML report.
  private String rewriteMarkdownLinks(String html) {
    return html.replaceAll("(<a\\s[^>]*href=\"[^\"]*?)\\.md(\")", "$1.html$2");
  }

  /// Converts mermaid fenced code blocks to HTML div blocks before commonmark parsing.
  /// The div is output as an HTML block which commonmark passes through verbatim.
  private String preprocessMermaid(String markdown) {
    StringBuilder sb = new StringBuilder();
    String[] lines = markdown.split("\n", -1);
    boolean inMermaid = false;
    StringBuilder mermaidBlock = new StringBuilder();

    for (String line : lines) {
      if (!inMermaid && line.trim().equals("```mermaid")) {
        inMermaid = true;
        mermaidBlock.setLength(0);
      } else if (inMermaid && line.trim().equals("```")) {
        inMermaid = false;
        sb.append("<div class=\"mermaid\">\n");
        sb.append(mermaidBlock);
        sb.append("</div>\n\n");
      } else if (inMermaid) {
        mermaidBlock.append(line).append("\n");
      } else {
        sb.append(line).append("\n");
      }
    }
    return sb.toString();
  }

  private String postprocessMermaid(String html) {
    return html;
  }

  // -------------------------------------------------------------------------
  // HTML generation
  // -------------------------------------------------------------------------

  private void generateIndexHtml(List<NavEntry> navigation) throws IOException {
    String defaultPage = navigation.getFirst().htmlRelativePath();
    // Use replace() instead of formatted() to avoid % in embedded JS being treated as format specs
    String shellHtml = shellTemplate().replace("__DEFAULT_PAGE__", defaultPage);
    Files.writeString(htmlOutputDirectory.resolve("index.html"), shellHtml);
  }

  /// Returns a lightweight HTML fragment containing only the {@code <article>} element.
  private String buildFragmentHtml(String bodyHtml) {
    return "<article>\n" + bodyHtml + "</article>\n";
  }

  private static String shellTemplate() {
    return """
        <!DOCTYPE html>
        <html lang="en" data-bs-theme="light">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>True Doc Tales</title>
          <link rel="icon" type="image/png" href="small_icon_full.png">
          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
          <link rel="stylesheet" href="truedoctales.css">
        </head>
        <body>
          <header class="top-header">
            <a class="top-header-brand" href="index.html">
              <img src="small_icon_full.png" alt="True Doc Tales">
              <span class="top-header-brand-text">
                <span class="brand-title">True Doc Tales</span>
                <span class="brand-subtitle">Fairy tales become reality</span>
              </span>
            </a>
            <button class="theme-toggle" id="theme-toggle" aria-label="Toggle theme">🌙</button>
          </header>
          <nav class="sidebar" id="sidebar">
            <button class="sidebar-handle" id="sidebar-handle" aria-label="Toggle navigation">
            </button>
            <div class="sidebar-content" id="sidebar-content">
              <!-- Navigation is built dynamically from report-nav.json -->
            </div>
          </nav>
          <button class="sidebar-toggle" id="sidebar-toggle" aria-label="Toggle navigation">☰</button>
          <main class="content">
            <div id="page-content"></div>
            <footer class="report-footer">
              <span class="footer-brand">True Doc Tales</span> &mdash; Fairy tales become reality<br>
              Generated by <a href="https://github.com/truedoctales/truedoctales-4j">truedoctales-4j</a>
            </footer>
          </main>
          <!-- Global flyout panel lives outside the sidebar so position:fixed is viewport-relative -->
          <ul id="nav-flyout" class="flyout-menu" role="menu" aria-label="Chapter stories"></ul>
          <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
          <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
          <script>
            (function () {
              var fallbackDefaultPage = '__DEFAULT_PAGE__';
              var currentPath = '';
              var sidebar = document.getElementById('sidebar');
              var navFlyout = document.getElementById('nav-flyout');
              var flyoutHideTimer = null;
              var activeChapterEl = null;

              // ---------------------------------------------------------------
              // Navigation JSON loader — builds the sidebar from report-nav.json
              // ---------------------------------------------------------------
              function escHtml(s) {
                return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
              }

              function numberBadge(n) {
                return n != null ? '<span class="nav-number" aria-hidden="true">' + escHtml(n) + '</span>' : '';
              }

              function buildSidebarFromNav(nav) {
                var sb = '';
                var chapters = nav.chapters || [];
                chapters.forEach(function (ch) {
                  var stories = ch.stories || [];
                  if (!ch.dirName) {
                    // Book-level entries (no chapter folder)
                    sb += '<div class="nav-group-label">' + escHtml(ch.label) + '</div>\\n';
                    sb += '<ul>\\n';
                    stories.forEach(function (s) {
                      sb += '<li><a href="#' + escHtml(s.htmlPath) + '">' + escHtml(s.title) + '</a></li>\\n';
                    });
                    sb += '</ul>\\n';
                  } else {
                    var num = ch.dirName ? parseInt(ch.dirName.split('_')[0], 10) : null;
                    var badge = isNaN(num) ? '' : numberBadge(num);
                    sb += '<div class="nav-chapter">\\n';
                    sb += '  <div class="nav-chapter-row">';
                    if (ch.introPage) {
                      sb += '<a href="#' + escHtml(ch.introPage) + '" class="chapter-link">' + badge + escHtml(ch.label) + '</a>';
                    } else {
                      sb += badge + escHtml(ch.label);
                    }
                    sb += '<span class="flyout-arrow" aria-hidden="true">&#x203A;</span>';
                    sb += '</div>\\n';
                    sb += '  <ul class="chapter-stories">\\n';
                    stories.forEach(function (s, idx) {
                      var storyNum = idx + 1;
                      sb += '    <li><a href="#' + escHtml(s.htmlPath) + '">' + numberBadge(storyNum) + escHtml(s.title) + '</a></li>\\n';
                    });
                    sb += '  </ul>\\n';
                    sb += '</div>\\n';
                  }
                });
                return sb;
              }

              // ---------------------------------------------------------------
              // Path utilities
              // ---------------------------------------------------------------
              function resolvePath(base, rel) {
                if (!rel || rel.charAt(0) === '#' || rel.indexOf('//') !== -1 || rel.indexOf(':') !== -1) {
                  return rel;
                }
                var stack = base.split('/');
                stack.pop();
                rel.split('/').forEach(function (s) {
                  if (s === '..') { stack.pop(); } else if (s !== '.') { stack.push(s); }
                });
                return stack.join('/');
              }

              // ---------------------------------------------------------------
              // Active nav
              // ---------------------------------------------------------------
              function updateActiveNav(path) {
                document.querySelectorAll('.sidebar-content a').forEach(function (a) {
                  a.classList.toggle('active', a.getAttribute('href') === '#' + path);
                });
                document.querySelectorAll('#nav-flyout a').forEach(function (a) {
                  a.classList.toggle('active', a.getAttribute('href') === '#' + path);
                });
              }

              // ---------------------------------------------------------------
              // Mermaid helpers
              // ---------------------------------------------------------------
              function saveMermaidSources(container) {
                container.querySelectorAll('.mermaid').forEach(function (el) {
                  if (!el.dataset.mermaidSource) {
                    el.dataset.mermaidSource = el.textContent.trim();
                  }
                });
              }

              function resetMermaidElements(container) {
                container.querySelectorAll('.mermaid').forEach(function (el) {
                  if (el.dataset.mermaidSource) {
                    el.removeAttribute('data-processed');
                    el.textContent = el.dataset.mermaidSource;
                  }
                });
              }

              // ---------------------------------------------------------------
              // Content loader
              // ---------------------------------------------------------------
              var defaultPage = fallbackDefaultPage;

              function loadContent(path) {
                var reqPath = path || defaultPage;
                currentPath = reqPath;
                fetch(reqPath)
                  .then(function (r) {
                    return r.ok
                      ? r.text()
                      : '<article><h1>Page not found</h1><p>Could not load <code>' + reqPath
                          + '</code>. <a href="#' + defaultPage + '">Return to home</a></p></article>';
                  })
                  .then(function (html) {
                    var container = document.getElementById('page-content');
                    container.innerHTML = html;
                    saveMermaidSources(container);
                    updateActiveNav(currentPath);
                    if (typeof mermaid !== 'undefined') {
                      mermaid.run({ querySelector: '#page-content .mermaid' });
                    }
                    document.querySelector('.content').scrollTop = 0;
                  });
              }

              // ---------------------------------------------------------------
              // Flyout menu
              // ---------------------------------------------------------------
              function positionAndShow(chapter) {
                clearTimeout(flyoutHideTimer);
                var source = chapter.querySelector('.chapter-stories');
                if (!source || !source.children.length) { return; }
                navFlyout.innerHTML = source.innerHTML;
                navFlyout.querySelectorAll('a').forEach(function (a) {
                  a.classList.toggle('active', a.getAttribute('href') === '#' + currentPath);
                });
                activeChapterEl = chapter;
                sidebar.classList.add('flyout-open');
                var rowRect = chapter.querySelector('.nav-chapter-row').getBoundingClientRect();
                var sidebarWidth = parseFloat(
                  getComputedStyle(document.documentElement).getPropertyValue('--sidebar-width')
                ) || 290;
                navFlyout.style.top = rowRect.top + 'px';
                navFlyout.style.left = sidebarWidth + 'px';
                navFlyout.classList.add('flyout-visible');
              }

              function hideFlyout() {
                clearTimeout(flyoutHideTimer);
                navFlyout.classList.remove('flyout-visible');
                sidebar.classList.remove('flyout-open');
                activeChapterEl = null;
              }

              function scheduleFlyoutHide() {
                flyoutHideTimer = setTimeout(hideFlyout, 100);
              }

              // ---------------------------------------------------------------
              // Bind flyout, keyboard, toggle events (called after nav is ready)
              // ---------------------------------------------------------------
              function bindNavEvents() {
                document.querySelectorAll('.nav-chapter').forEach(function (chapter) {
                  chapter.querySelector('.nav-chapter-row').addEventListener('mouseenter', function () {
                    positionAndShow(chapter);
                  });
                });

                sidebar.addEventListener('mouseleave', function (e) {
                  if (navFlyout.contains(e.relatedTarget)) { return; }
                  scheduleFlyoutHide();
                });
                navFlyout.addEventListener('mouseenter', function () { clearTimeout(flyoutHideTimer); });
                navFlyout.addEventListener('mouseleave', function (e) {
                  if (sidebar.contains(e.relatedTarget)) { return; }
                  scheduleFlyoutHide();
                });

                sidebar.addEventListener('click', function (e) {
                  if (e.target.closest('a[href]')) { hideFlyout(); }
                });
                navFlyout.addEventListener('click', function (e) {
                  if (e.target.closest('a[href]')) { hideFlyout(); sidebar.classList.remove('open'); }
                });
              }

              // ---------------------------------------------------------------
              // Keyboard navigation
              // ---------------------------------------------------------------
              document.addEventListener('keydown', function (e) {
                var tag = document.activeElement ? document.activeElement.tagName : '';
                if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') { return; }

                if (e.key === ':') {
                  e.preventDefault();
                  var first = sidebar.querySelector('.nav-chapter-row .chapter-link, .sidebar-content li a');
                  if (first) { first.focus(); }
                  return;
                }

                var flyoutVisible = navFlyout.classList.contains('flyout-visible');

                if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                  e.preventDefault();
                  var items = flyoutVisible
                    ? Array.from(navFlyout.querySelectorAll('a'))
                    : Array.from(sidebar.querySelectorAll('.nav-chapter-row .chapter-link, .sidebar-content > ul > li > a'));
                  if (!items.length) { return; }
                  var idx = items.indexOf(document.activeElement);
                  idx = e.key === 'ArrowDown' ? (idx + 1) % items.length : (idx - 1 + items.length) % items.length;
                  items[idx].focus();
                  return;
                }

                if (e.key === 'ArrowRight') {
                  e.preventDefault();
                  var chapter = document.activeElement && document.activeElement.closest('.nav-chapter');
                  if (chapter) {
                    positionAndShow(chapter);
                    var fi = navFlyout.querySelector('a');
                    if (fi) { fi.focus(); }
                  }
                  return;
                }

                if (e.key === 'ArrowLeft' && flyoutVisible) {
                  e.preventDefault();
                  var chLink = activeChapterEl && activeChapterEl.querySelector('.chapter-link');
                  hideFlyout();
                  if (chLink) { chLink.focus(); }
                  return;
                }

                if (e.key === 'Escape') {
                  hideFlyout();
                  if (document.activeElement && sidebar.contains(document.activeElement)) {
                    document.activeElement.blur();
                  }
                }
              });

              // ---------------------------------------------------------------
              // Sidebar toggles
              // ---------------------------------------------------------------
              document.getElementById('sidebar-handle').addEventListener('click', function () {
                sidebar.classList.toggle('open');
              });
              document.getElementById('sidebar-toggle').addEventListener('click', function () {
                sidebar.classList.toggle('open');
              });

              // ---------------------------------------------------------------
              // Content link intercept
              // ---------------------------------------------------------------
              document.addEventListener('click', function (e) {
                var a = e.target.closest('#page-content a[href]');
                if (!a) { return; }
                var href = a.getAttribute('href');
                if (href && href.charAt(0) !== '#' && href.indexOf('//') === -1 && href.indexOf(':') === -1) {
                  e.preventDefault();
                  location.hash = '#' + resolvePath(currentPath, href);
                }
              });

              window.addEventListener('hashchange', function () {
                loadContent(location.hash.slice(1));
              });

              // ---------------------------------------------------------------
              // Theme toggle
              // ---------------------------------------------------------------
              var toggle = document.getElementById('theme-toggle');
              var htmlEl = document.documentElement;
              var stored = localStorage.getItem('truedoctales-theme');
              if (stored) {
                htmlEl.setAttribute('data-bs-theme', stored);
                toggle.textContent = stored === 'dark' ? '☀️' : '🌙';
              }
              toggle.addEventListener('click', function () {
                var cur = htmlEl.getAttribute('data-bs-theme') || 'light';
                var next = cur === 'dark' ? 'light' : 'dark';
                htmlEl.setAttribute('data-bs-theme', next);
                localStorage.setItem('truedoctales-theme', next);
                toggle.textContent = next === 'dark' ? '☀️' : '🌙';
                if (typeof mermaid !== 'undefined') {
                  mermaid.initialize({ startOnLoad: false, theme: next === 'dark' ? 'dark' : 'default' });
                  resetMermaidElements(document.getElementById('page-content'));
                  mermaid.run({ querySelector: '#page-content .mermaid' });
                }
              });

              // ---------------------------------------------------------------
              // Mermaid init
              // ---------------------------------------------------------------
              if (typeof mermaid !== 'undefined') {
                mermaid.initialize({
                  startOnLoad: false,
                  theme: htmlEl.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'default'
                });
              }

              // ---------------------------------------------------------------
              // Bootstrap: load report-nav.json, build sidebar, then load page
              // ---------------------------------------------------------------
              fetch('report-nav.json')
                .then(function (r) { return r.ok ? r.json() : null; })
                .then(function (nav) {
                  if (nav) {
                    if (nav.defaultPage) { defaultPage = nav.defaultPage; }
                    if (nav.title) {
                      var bt = document.querySelector('.brand-title');
                      if (bt) { bt.textContent = nav.title; }
                    }
                    var sc = document.getElementById('sidebar-content');
                    if (sc) { sc.innerHTML = buildSidebarFromNav(nav); }
                  }
                  bindNavEvents();
                  loadContent(location.hash ? location.hash.slice(1) : null);
                })
                .catch(function () {
                  bindNavEvents();
                  loadContent(location.hash ? location.hash.slice(1) : null);
                });
            }());
          </script>
        </body>
        </html>
        """;
  }

  // -------------------------------------------------------------------------
  // JSON utilities (no Jackson dependency in this module)
  // -------------------------------------------------------------------------

  private static final Pattern JSON_FIELD_PATTERN =
      Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"");

  /// Reads a single string field value from a JSON file without a JSON library dependency.
  /// Returns {@code null} if the file does not exist, cannot be read, or the field is not found.
  private String readJsonField(Path jsonFile, String fieldName) {
    if (!Files.isRegularFile(jsonFile)) {
      return null;
    }
    try {
      String content = Files.readString(jsonFile);
      Matcher matcher = JSON_FIELD_PATTERN.matcher(content);
      while (matcher.find()) {
        if (fieldName.equals(matcher.group(1))) {
          return matcher.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
        }
      }
    } catch (IOException e) {
      logger.warning("Could not read JSON field '" + fieldName + "' from: " + jsonFile);
    }
    return null;
  }

  // -------------------------------------------------------------------------
  // Label / title helpers
  // -------------------------------------------------------------------------

  /// Extracts the title from a markdown file by looking for the first {@code # } heading.
  private String extractTitleFromMarkdown(Path file) {
    try {
      for (String line : Files.readAllLines(file)) {
        String trimmed = line.trim();
        if (trimmed.startsWith("# ")) {
          return trimmed.substring(2).trim();
        }
      }
    } catch (IOException e) {
      logger.warning("Could not read title from: " + file);
    }
    String name = file.getFileName().toString().replace(".md", "");
    return toLabel(name);
  }

  /// Converts a directory/file name like {@code "01_chapter-basics"} to {@code "Chapter Basics"}.
  private static String toLabel(String name) {
    String label = name.replaceFirst("^\\d+_", "").replace("-", " ").replace("_", " ");
    StringBuilder sb = new StringBuilder();
    for (String word : label.split(" ")) {
      if (!word.isEmpty()) {
        if (!sb.isEmpty()) sb.append(" ");
        sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
    }
    return sb.toString();
  }

  private static boolean isIntroPath(String relativePath) {
    int slashIdx = relativePath.lastIndexOf('/');
    String fileName = slashIdx >= 0 ? relativePath.substring(slashIdx + 1) : relativePath;
    return fileName.startsWith("00_");
  }

  /// Minimal JSON string escaping.
  private static String jsonString(String value) {
    if (value == null) return "null";
    return "\""
        + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        + "\"";
  }

  // -------------------------------------------------------------------------
  // Data types
  // -------------------------------------------------------------------------

  /// Represents a navigation entry: one HTML page derived from one markdown file.
  record NavEntry(
      Path sourcePath,
      String relativePath,
      String htmlRelativePath,
      String title,
      boolean isChapterIntro,
      String chapterDirName,
      String chapterLabel) {}

  /// A group of {@link NavEntry} items belonging to the same chapter directory.
  record ChapterGroup(String label, String dirName, String introHtmlPath, List<NavEntry> stories) {}
}
