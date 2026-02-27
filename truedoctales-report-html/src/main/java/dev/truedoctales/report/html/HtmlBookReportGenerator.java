package dev.truedoctales.report.html;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
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
              Files.copy(file, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return FileVisitResult.CONTINUE;
          }
        });
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

  private String extractTitle(Path file) {
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
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>%s - True Doc Tales</title>
          <style>
        %s
          </style>
        </head>
        <body>
          <nav class="sidebar" id="sidebar">
            <div class="sidebar-header">
              <h2>📖 True Doc Tales</h2>
            </div>
            <div class="sidebar-content">
        %s
            </div>
          </nav>
          <button class="sidebar-toggle" id="sidebar-toggle" aria-label="Toggle navigation">☰</button>
          <main class="content">
            <article>
        %s
            </article>
          </main>
          <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
          <script>
            mermaid.initialize({ startOnLoad: true, theme: 'default' });
            document.getElementById('sidebar-toggle').addEventListener('click', function() {
              document.getElementById('sidebar').classList.toggle('open');
            });
          </script>
        </body>
        </html>
        """
        .formatted(
            escapeHtml(title), getCss(), navHtml.replace("DEPTH_PREFIX/", depthPrefix), bodyHtml);
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

  private String getCss() {
    return """
        :root {
          --sidebar-width: 280px;
          --primary: #2563eb;
          --primary-light: #3b82f6;
          --bg: #ffffff;
          --bg-sidebar: #f8fafc;
          --text: #1e293b;
          --text-muted: #64748b;
          --border: #e2e8f0;
          --success: #16a34a;
          --error: #dc2626;
          --warning: #d97706;
          --skip: #6366f1;
          --code-bg: #f1f5f9;
          --shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html { font-size: 16px; }
        body {
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
          color: var(--text);
          background: var(--bg);
          display: flex;
          min-height: 100vh;
        }
        .sidebar {
          width: var(--sidebar-width);
          background: var(--bg-sidebar);
          border-right: 1px solid var(--border);
          position: fixed;
          top: 0;
          left: 0;
          height: 100vh;
          overflow-y: auto;
          transition: transform 0.3s ease;
          z-index: 100;
        }
        .sidebar-header {
          padding: 1.5rem;
          border-bottom: 1px solid var(--border);
          background: var(--primary);
          color: white;
        }
        .sidebar-header h2 { font-size: 1.1rem; font-weight: 600; }
        .sidebar-content { padding: 1rem 0; }
        .sidebar-content h3 {
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          color: var(--text-muted);
          padding: 0.75rem 1.5rem 0.25rem;
        }
        .sidebar-content .nav-group-label {
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          color: var(--text-muted);
          padding: 0.75rem 1.5rem 0.25rem;
        }
        .sidebar-content .nav-tree {
          border-bottom: 1px solid var(--border);
        }
        .sidebar-content .nav-tree summary {
          display: block;
          padding: 0.5rem 1.5rem;
          font-size: 0.8rem;
          font-weight: 600;
          color: var(--text);
          cursor: pointer;
          list-style: none;
          user-select: none;
          transition: background 0.15s;
        }
        .sidebar-content .nav-tree summary::-webkit-details-marker { display: none; }
        .sidebar-content .nav-tree summary::before {
          content: '▶';
          display: inline-block;
          margin-right: 0.5rem;
          font-size: 0.6rem;
          transition: transform 0.2s;
          vertical-align: middle;
        }
        .sidebar-content .nav-tree[open] > summary::before {
          transform: rotate(90deg);
        }
        .sidebar-content .nav-tree summary:hover {
          background: var(--border);
        }
        .sidebar-content .nav-tree ul {
          padding-left: 0.5rem;
        }
        .sidebar-content ul { list-style: none; }
        .sidebar-content li a {
          display: block;
          padding: 0.4rem 1.5rem;
          color: var(--text);
          text-decoration: none;
          font-size: 0.875rem;
          border-left: 3px solid transparent;
          transition: background 0.15s, border-color 0.15s;
        }
        .sidebar-content li a:hover {
          background: #e2e8f0;
          border-left-color: var(--primary-light);
        }
        .sidebar-content li a.active {
          background: #dbeafe;
          border-left-color: var(--primary);
          font-weight: 600;
          color: var(--primary);
        }
        .sidebar-toggle {
          display: none;
          position: fixed;
          top: 1rem;
          left: 1rem;
          z-index: 200;
          background: var(--primary);
          color: white;
          border: none;
          border-radius: 6px;
          padding: 0.5rem 0.75rem;
          font-size: 1.25rem;
          cursor: pointer;
          box-shadow: var(--shadow);
        }
        .content {
          margin-left: var(--sidebar-width);
          flex: 1;
          min-width: 0;
        }
        article {
          max-width: 52rem;
          margin: 0 auto;
          padding: 2.5rem 3rem;
          line-height: 1.7;
        }
        h1 { font-size: 2rem; font-weight: 700; margin: 2rem 0 1rem; color: var(--text); border-bottom: 2px solid var(--border); padding-bottom: 0.5rem; }
        h2 { font-size: 1.5rem; font-weight: 600; margin: 1.75rem 0 0.75rem; color: var(--text); }
        h3 { font-size: 1.25rem; font-weight: 600; margin: 1.5rem 0 0.5rem; }
        h4 { font-size: 1.1rem; font-weight: 600; margin: 1.25rem 0 0.5rem; }
        p { margin: 0.75rem 0; }
        a { color: var(--primary); text-decoration: none; }
        a:hover { text-decoration: underline; }
        blockquote {
          border-left: 4px solid var(--primary);
          background: #f0f7ff;
          padding: 0.75rem 1rem;
          margin: 1rem 0;
          border-radius: 0 6px 6px 0;
        }
        blockquote p { margin: 0.25rem 0; }
        code {
          font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
          background: var(--code-bg);
          padding: 0.15rem 0.4rem;
          border-radius: 4px;
          font-size: 0.875em;
        }
        pre {
          background: var(--code-bg);
          border: 1px solid var(--border);
          border-radius: 8px;
          padding: 1rem 1.25rem;
          overflow-x: auto;
          margin: 1rem 0;
        }
        pre code { background: none; padding: 0; }
        table {
          border-collapse: collapse;
          width: 100%;
          margin: 1rem 0;
          font-size: 0.9rem;
        }
        th, td {
          border: 1px solid var(--border);
          padding: 0.5rem 0.75rem;
          text-align: left;
        }
        th { background: var(--bg-sidebar); font-weight: 600; }
        tr:nth-child(even) { background: #fafafa; }
        img { max-width: 100%; height: auto; border-radius: 8px; margin: 1rem 0; }
        ul, ol { margin: 0.5rem 0 0.5rem 1.5rem; }
        li { margin: 0.25rem 0; }
        hr { border: none; border-top: 1px solid var(--border); margin: 2rem 0; }
        .mermaid {
          background: white;
          padding: 1.5rem;
          border: 1px solid var(--border);
          border-radius: 8px;
          margin: 1rem 0;
          text-align: center;
        }
        @media (max-width: 768px) {
          .sidebar { transform: translateX(-100%); }
          .sidebar.open { transform: translateX(0); }
          .sidebar-toggle { display: block; }
          .content { margin-left: 0; }
          article { padding: 1.5rem 1rem; }
        }
        """;
  }

  /// Represents a navigation entry for the sidebar.
  record NavEntry(Path sourcePath, String relativePath, String htmlRelativePath, String title) {}
}
