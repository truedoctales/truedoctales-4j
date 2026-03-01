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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
/// <p>Mermaid diagrams are rendered client-side via the mermaid.js library.
public class HtmlBookReportGenerator {

  private static final Logger logger = Logger.getLogger(HtmlBookReportGenerator.class.getName());

  private static final String CSS_RESOURCE = "truedoctales.css";
  private static final String ICON_RESOURCE = "small_icon_full.png";

  private final Path markdownDirectory;
  private final Path htmlOutputDirectory;
  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;

  /// Creates a new HTML report generator.
  ///
  /// @param markdownDirectory directory containing enriched markdown files
  /// @param htmlOutputDirectory directory where the HTML report will be written
  public HtmlBookReportGenerator(Path markdownDirectory, Path htmlOutputDirectory) {
    this.markdownDirectory = markdownDirectory;
    this.htmlOutputDirectory = htmlOutputDirectory;
    List<Extension> extensions = List.of(TablesExtension.create());
    this.markdownParser = Parser.builder().extensions(extensions).build();
    this.htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
  }

  /// Generates the full HTML report with navigation.
  ///
  /// <p>Each markdown file is converted to a lightweight {@code <article>} HTML fragment.
  /// A single {@code index.html} SPA shell contains the full Bootstrap layout and a
  /// JavaScript router that fetches and injects the fragments on demand.
  ///
  /// @throws IOException if reading or writing files fails
  public void generate() throws IOException {
    logger.info("Generating HTML report from: " + markdownDirectory);
    Files.createDirectories(htmlOutputDirectory);

    List<NavEntry> navigation = buildNavigation();
    copyNonMarkdownFiles();
    copyStaticAssets();

    for (NavEntry entry : navigation) {
      String markdown = Files.readString(entry.sourcePath());
      String bodyHtml = convertMarkdownToHtml(markdown);
      String fragmentHtml = buildFragmentHtml(bodyHtml);
      Path outputPath = htmlOutputDirectory.resolve(entry.htmlRelativePath());
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, fragmentHtml);
      logger.info("  HTML: " + entry.htmlRelativePath());
    }

    if (!navigation.isEmpty()) {
      generateIndexHtml(navigation);
    }
    logger.info("HTML report generated in: " + htmlOutputDirectory);
  }

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

  private List<NavEntry> buildNavigation() throws IOException {
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
              String title = extractTitle(file);
              entries.add(new NavEntry(file, relativePath, htmlPath, title));
            }
            return FileVisitResult.CONTINUE;
          }
        });
    entries.sort(Comparator.comparing(NavEntry::relativePath));
    return entries;
  }

  private static final Pattern META_TITLE_PATTERN =
      Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");

  private String extractTitle(Path file) {
    // Try reading title from a sibling meta.json (produced by the execution pipeline)
    String metaTitle = readTitleFromMetaJson(file.getParent());
    if (metaTitle != null) {
      return metaTitle;
    }
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
    return name.replace("-", " ").replace("_", " ");
  }

  /// Reads the title from a {@code meta.json} file in the given directory.
  /// Returns {@code null} if the file does not exist or cannot be parsed.
  private String readTitleFromMetaJson(Path directory) {
    if (directory == null) {
      return null;
    }
    Path metaJson = directory.resolve("meta.json");
    if (!Files.isRegularFile(metaJson)) {
      return null;
    }
    try {
      String content = Files.readString(metaJson);
      Matcher matcher = META_TITLE_PATTERN.matcher(content);
      if (matcher.find()) {
        return matcher.group(1);
      }
    } catch (IOException e) {
      logger.warning("Could not read meta.json: " + metaJson);
    }
    return null;
  }

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
        // Output as an HTML block — commonmark will pass it through unchanged.
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
    // No post-processing needed; mermaid divs are already in the HTML.
    return html;
  }

  private void generateIndexHtml(List<NavEntry> navigation) throws IOException {
    String navHtml = buildNavigationHtml(navigation);
    String defaultPage = navigation.getFirst().htmlRelativePath();
    String shellHtml = buildShellHtml(navHtml, defaultPage);
    Files.writeString(htmlOutputDirectory.resolve("index.html"), shellHtml);
  }

  /// Returns a lightweight HTML fragment containing only the {@code <article>} element.
  /// These fragments are fetched and injected into the SPA shell by the JS router.
  private String buildFragmentHtml(String bodyHtml) {
    return "<article>\n" + bodyHtml + "</article>\n";
  }

  /// Builds the SPA shell HTML by substituting the pre-built navigation and default page
  /// path into the static template.  A {@code %} in the navigation HTML is escaped so it
  /// is not misinterpreted as a format specifier.
  private String buildShellHtml(String navHtml, String defaultPage) {
    return shellTemplate().formatted(navHtml.replace("%", "%%"), defaultPage);
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
            <div class="sidebar-content">
        %s
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
          <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
          <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
          <script>
            (function () {
              var defaultPage = '%s';
              var currentPath = '';

              // Resolve a relative path against a base path (handles ../ segments)
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

              // Mark the current page active in the sidebar and expand its chapter group
              function updateActiveNav(path) {
                document.querySelectorAll('.sidebar-content a').forEach(function (a) {
                  a.classList.toggle('active', a.getAttribute('href') === '#' + path);
                });
                document.querySelectorAll('details.nav-tree').forEach(function (d) {
                  if (d.querySelector('a.active')) { d.open = true; }
                });
              }

              // Save mermaid diagram source before first render so it can be restored on theme change
              function saveMermaidSources(container) {
                container.querySelectorAll('.mermaid').forEach(function (el) {
                  if (!el.dataset.mermaidSource) {
                    el.dataset.mermaidSource = el.textContent.trim();
                  }
                });
              }

              // Restore source text and clear processed flag so mermaid can re-render with new theme
              function resetMermaidElements(container) {
                container.querySelectorAll('.mermaid').forEach(function (el) {
                  if (el.dataset.mermaidSource) {
                    el.removeAttribute('data-processed');
                    el.textContent = el.dataset.mermaidSource;
                  }
                });
              }

              // Fetch an HTML fragment and inject it into the content area
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

              // Intercept internal link clicks within the content area
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

              // Hover-open / hover-close for chapter groups in the sidebar
              document.querySelectorAll('details.nav-tree').forEach(function (details) {
                var hoverOpened = false;
                details.addEventListener('mouseenter', function () {
                  if (!details.open) { details.open = true; hoverOpened = true; }
                });
                details.addEventListener('mouseleave', function () {
                  if (hoverOpened) { details.open = false; hoverOpened = false; }
                });
              });

              // Chapter-link click navigates without toggling the <details>
              document.querySelectorAll('.nav-tree summary .chapter-link').forEach(function (link) {
                link.addEventListener('click', function (e) { e.stopPropagation(); });
              });

              // Mobile sidebar toggle
              document.getElementById('sidebar-toggle').addEventListener('click', function () {
                document.getElementById('sidebar').classList.toggle('open');
              });

              // Theme toggle — uses Bootstrap data-bs-theme; no page reload needed
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

              // Mermaid initialisation (startOnLoad: false; we call run() after each load)
              if (typeof mermaid !== 'undefined') {
                mermaid.initialize({
                  startOnLoad: false,
                  theme: htmlEl.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'default'
                });
              }

              // Load the initial page from the URL hash or fall back to the first page
              loadContent(location.hash ? location.hash.slice(1) : null);
            }());
          </script>
        </body>
        </html>
        """;
  }

  private String buildNavigationHtml(List<NavEntry> navigation) {
    StringBuilder sb = new StringBuilder();
    String currentGroup = null;

    // Pre-scan: find the chapter intro (first 00_-prefixed file) for each non-Book group
    Map<String, NavEntry> chapterIntros = new LinkedHashMap<>();
    for (NavEntry entry : navigation) {
      String group = getGroupLabel(entry.relativePath());
      if (!"Book".equals(group) && !chapterIntros.containsKey(group) && isChapterIntro(entry)) {
        chapterIntros.put(group, entry);
      }
    }

    for (NavEntry entry : navigation) {
      String group = getGroupLabel(entry.relativePath());
      NavEntry intro = chapterIntros.get(group);
      boolean isIntroEntry = entry.equals(intro);

      if (!group.equals(currentGroup)) {
        if (currentGroup != null) {
          sb.append("        </ul>\n");
          if (!"Book".equals(currentGroup)) {
            sb.append("      </details>\n");
          }
        }
        if ("Book".equals(group)) {
          sb.append("      <div class=\"nav-group-label\">")
              .append(escapeHtml(group))
              .append("</div>\n");
          sb.append("        <ul>\n");
        } else {
          sb.append("      <details class=\"nav-tree\">\n");
          sb.append("        <summary>");
          if (intro != null) {
            sb.append("<a href=\"#")
                .append(intro.htmlRelativePath())
                .append("\" class=\"chapter-link\">")
                .append(escapeHtml(group))
                .append("</a>");
          } else {
            sb.append(escapeHtml(group));
          }
          sb.append("<span class=\"toggle-arrow\"></span>");
          sb.append("</summary>\n");
          sb.append("        <ul>\n");
        }
        currentGroup = group;
      }

      // Skip the chapter intro entry — it is already shown as the summary link
      if (isIntroEntry) {
        continue;
      }

      sb.append("          <li><a href=\"#")
          .append(entry.htmlRelativePath())
          .append("\">")
          .append(escapeHtml(entry.title()))
          .append("</a></li>\n");
    }
    if (currentGroup != null) {
      sb.append("        </ul>\n");
      if (!"Book".equals(currentGroup)) {
        sb.append("      </details>\n");
      }
    }
    return sb.toString();
  }

  /// Returns {@code true} if the entry is a chapter intro file (filename starts with {@code 00_}
  /// inside a subdirectory). These files are shown as the clickable summary link for the chapter
  /// group instead of as a separate list item.
  private boolean isChapterIntro(NavEntry entry) {
    String path = entry.relativePath();
    int slashIdx = path.lastIndexOf('/');
    if (slashIdx < 0) {
      return false;
    }
    String fileName = path.substring(slashIdx + 1);
    return fileName.startsWith("00_");
  }

  private String getGroupLabel(String relativePath) {
    if (!relativePath.contains("/")) {
      return "Book";
    }
    String dir = relativePath.substring(0, relativePath.indexOf('/'));
    // Remove leading number prefix (e.g., "01_chapter-basics" -> "Chapter Basics")
    String label = dir.replaceFirst("^\\d+_", "");
    label = label.replace("-", " ").replace("_", " ");
    // Capitalize first letter of each word
    StringBuilder capitalized = new StringBuilder();
    for (String word : label.split(" ")) {
      if (!word.isEmpty()) {
        if (!capitalized.isEmpty()) {
          capitalized.append(" ");
        }
        capitalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
    }
    return capitalized.toString();
  }

  private String escapeHtml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  /// Represents a navigation entry for the sidebar.
  record NavEntry(Path sourcePath, String relativePath, String htmlRelativePath, String title) {}
}
