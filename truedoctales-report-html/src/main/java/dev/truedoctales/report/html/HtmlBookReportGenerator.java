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

/// Generates a professional HTML report from enriched markdown files.
///
/// The generator converts each markdown file to a styled HTML page and creates an
/// {@code index.html} with sidebar navigation linking all chapters and stories.
/// Mermaid diagrams are rendered client-side via the mermaid.js library.
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
      String fullHtml = wrapInTemplate(entry.title(), bodyHtml, navigation, entry);
      Path outputPath = htmlOutputDirectory.resolve(entry.htmlRelativePath());
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, fullHtml);
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
    // Redirect to first page
    String firstPage = navigation.getFirst().htmlRelativePath();
    String indexHtml =
        "<!DOCTYPE html>\n<html><head><meta http-equiv=\"refresh\" content=\"0; url="
            + firstPage
            + "\"><title>True Doc Tales Report</title></head>"
            + "<body><p>Redirecting to <a href=\""
            + firstPage
            + "\">report</a>...</p></body></html>\n";
    Files.writeString(htmlOutputDirectory.resolve("index.html"), indexHtml);
  }

  private String wrapInTemplate(
      String title, String bodyHtml, List<NavEntry> navigation, NavEntry current) {
    String navHtml = buildNavigationHtml(navigation, current);
    String depthPrefix = computeDepthPrefix(current.htmlRelativePath());

    return """
        <!DOCTYPE html>
        <html lang="en" data-theme="light">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>%s - True Doc Tales</title>
          <link rel="icon" type="image/png" href="%ssmall_icon_full.png">
          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css">
          <link rel="stylesheet" href="%struedoctales.css">
        </head>
        <body>
          <header class="top-header">
            <a class="top-header-brand" href="%sindex.html">
              <img src="%ssmall_icon_full.png" alt="True Doc Tales">
              <span>True Doc Tales</span>
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
            <article>
        %s
            </article>
            <footer class="report-footer">
              <span class="footer-brand">True Doc Tales</span> &mdash; Living Documentation Framework<br>
              Generated by <a href="https://github.com/truedoctales/truedoctales-4j">truedoctales-4j</a>
            </footer>
          </main>
          <script>
            document.getElementById('sidebar-toggle').addEventListener('click', function() {
              document.getElementById('sidebar').classList.toggle('open');
            });
            (function() {
              var toggle = document.getElementById('theme-toggle');
              var html = document.documentElement;
              var stored = localStorage.getItem('truedoctales-theme');
              if (stored) {
                html.setAttribute('data-theme', stored);
                toggle.textContent = stored === 'dark' ? '☀️' : '🌙';
              }
              toggle.addEventListener('click', function() {
                var current = html.getAttribute('data-theme') || 'light';
                var next = current === 'dark' ? 'light' : 'dark';
                html.setAttribute('data-theme', next);
                localStorage.setItem('truedoctales-theme', next);
                toggle.textContent = next === 'dark' ? '☀️' : '🌙';
              });
            })();
          </script>
          <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
          <script>
            if (typeof mermaid !== 'undefined') {
              var mermaidTheme = (document.documentElement.getAttribute('data-theme') === 'dark') ? 'dark' : 'default';
              mermaid.initialize({ startOnLoad: true, theme: mermaidTheme });
            }
          </script>
        </body>
        </html>
        """
        .formatted(
            escapeHtml(title),
            depthPrefix,
            depthPrefix,
            depthPrefix,
            depthPrefix,
            navHtml.replace("DEPTH_PREFIX/", depthPrefix),
            bodyHtml);
  }

  private String computeDepthPrefix(String htmlRelativePath) {
    long depth = htmlRelativePath.chars().filter(c -> c == '/').count();
    if (depth == 0) {
      return "";
    }
    return "../".repeat((int) depth);
  }

  private String buildNavigationHtml(List<NavEntry> navigation, NavEntry current) {
    StringBuilder sb = new StringBuilder();
    String currentGroup = null;
    String activeGroup = getGroupLabel(current.relativePath());

    for (NavEntry entry : navigation) {
      String group = getGroupLabel(entry.relativePath());
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
          String openAttr = group.equals(activeGroup) ? " open" : "";
          sb.append("      <details class=\"nav-tree\"").append(openAttr).append(">\n");
          sb.append("        <summary>").append(escapeHtml(group)).append("</summary>\n");
          sb.append("        <ul>\n");
        }
        currentGroup = group;
      }
      String activeClass = entry.equals(current) ? " class=\"active\"" : "";
      sb.append("          <li><a href=\"DEPTH_PREFIX/")
          .append(entry.htmlRelativePath())
          .append("\"")
          .append(activeClass)
          .append(">")
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
