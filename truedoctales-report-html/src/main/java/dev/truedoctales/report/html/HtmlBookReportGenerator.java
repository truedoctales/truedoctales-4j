package dev.truedoctales.report.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
  private static final String JS_RESOURCE = "truedoctales.js";
  private static final String ICON_RESOURCE = "small_icon_full.png";
  private static final String TEMPLATE_RESOURCE = "index-template.html";

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

    // 2. Chapter directories (sorted — prequels (00_) are moved behind all regular chapters)
    List<Path> chapterDirs = new ArrayList<>();
    try (var stream = Files.list(jsonReportDirectory)) {
      stream.filter(Files::isDirectory).sorted().forEach(chapterDirs::add);
    }
    // Move prequel directories (starting with "00_") to the end of the list
    List<Path> prequelDirs =
        chapterDirs.stream().filter(p -> p.getFileName().toString().startsWith("00_")).toList();
    List<Path> regularDirs =
        chapterDirs.stream().filter(p -> !p.getFileName().toString().startsWith("00_")).toList();
    chapterDirs = new ArrayList<>(regularDirs);
    chapterDirs.addAll(prequelDirs);

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
                "Plots"));
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
                            "Plots"));
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
    // Move prequel entries (in 00_ chapter directories) behind all regular chapters
    List<NavEntry> prequelEntries =
        entries.stream()
            .filter(e -> e.chapterDirName() != null && e.chapterDirName().startsWith("00_"))
            .toList();
    List<NavEntry> regularEntries =
        entries.stream()
            .filter(e -> e.chapterDirName() == null || !e.chapterDirName().startsWith("00_"))
            .toList();
    List<NavEntry> reordered = new ArrayList<>(regularEntries);
    reordered.addAll(prequelEntries);
    return reordered;
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

  private void copyJsAsset(String defaultPage) throws IOException {
    String jsTemplate = readResourceAsString(JS_RESOURCE);
    String jsContent = jsTemplate.replace("__DEFAULT_PAGE__", defaultPage);
    Files.writeString(htmlOutputDirectory.resolve(JS_RESOURCE), jsContent);
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
    String shellHtml = readResourceAsString(TEMPLATE_RESOURCE);
    Files.writeString(htmlOutputDirectory.resolve("index.html"), shellHtml);
    copyJsAsset(defaultPage);
  }

  /// Returns a lightweight HTML fragment containing only the {@code <article>} element.
  private String buildFragmentHtml(String bodyHtml) {
    return "<article>\n" + bodyHtml + "</article>\n";
  }

  /// Reads a classpath resource as a UTF-8 string.
  private String readResourceAsString(String resourceName) throws IOException {
    try (InputStream in = getClass().getResourceAsStream(resourceName)) {
      if (in == null) {
        throw new IOException("Resource not found: " + resourceName);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
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
