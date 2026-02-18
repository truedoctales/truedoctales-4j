package dev.truedoctales.report.html;

/// Simple markdown to HTML converter for intro content.
/// Supports basic markdown features without external dependencies.
final class MarkdownRenderer {

  private MarkdownRenderer() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /// Converts markdown text to HTML.
  ///
  /// Supported features:
  /// - Headings (`##`, `###`, `####`)
  /// - Unordered lists (`-`, `*`)
  /// - Ordered lists (`1.`, `2.`, etc.)
  /// - Code blocks (triple backticks)
  /// - Inline formatting (`**bold**`, `*italic*`, `` `code` ``)
  /// - Images (`![alt](url)`)
  /// - Mermaid diagrams (```mermaid)
  ///
  /// @param markdown the markdown text to convert
  /// @return HTML representation of the markdown
  static String toHtml(String markdown) {
    if (markdown == null || markdown.isBlank()) {
      return "";
    }

    StringBuilder html = new StringBuilder();
    String[] lines = markdown.split("\n");
    boolean inList = false;
    boolean inCodeBlock = false;
    boolean inMermaidBlock = false;
    StringBuilder paragraph = new StringBuilder();
    StringBuilder mermaidContent = new StringBuilder();

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String trimmed = line.trim();

      // Handle code blocks and mermaid blocks
      if (trimmed.startsWith("```")) {
        if (inCodeBlock) {
          html.append("</code></pre>\n");
          inCodeBlock = false;
        } else if (inMermaidBlock) {
          // End mermaid block
          html.append("<div class=\"mermaid\">\n");
          html.append(HtmlEscaper.escape(mermaidContent.toString()));
          html.append("</div>\n");
          inMermaidBlock = false;
          mermaidContent.setLength(0);
        } else {
          flushParagraph(html, paragraph);
          // Check if it's a mermaid block
          if (trimmed.equals("```mermaid")) {
            inMermaidBlock = true;
          } else {
            html.append("<pre><code>");
            inCodeBlock = true;
          }
        }
        continue;
      }

      if (inCodeBlock) {
        html.append(HtmlEscaper.escape(line)).append("\n");
        continue;
      }

      if (inMermaidBlock) {
        mermaidContent.append(line).append("\n");
        continue;
      }

      // Handle standalone images on their own line
      if (trimmed.matches("^!\\[.*?\\]\\(.*?\\)$")) {
        flushParagraph(html, paragraph);
        closeLists(html, inList);
        inList = false;
        html.append(processImage(trimmed)).append("\n");
        continue;
      }

      // Handle headings (H2-H6, H1 is reserved for title)
      if (trimmed.startsWith("## ")) {
        flushParagraph(html, paragraph);
        closeLists(html, inList);
        inList = false;
        html.append("<h3>").append(processInlineMarkdown(trimmed.substring(3))).append("</h3>\n");
      } else if (trimmed.startsWith("### ")) {
        flushParagraph(html, paragraph);
        closeLists(html, inList);
        inList = false;
        html.append("<h4>").append(processInlineMarkdown(trimmed.substring(4))).append("</h4>\n");
      } else if (trimmed.startsWith("#### ")) {
        flushParagraph(html, paragraph);
        closeLists(html, inList);
        inList = false;
        html.append("<h5>").append(processInlineMarkdown(trimmed.substring(5))).append("</h5>\n");
      }
      // Handle unordered lists
      else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
        flushParagraph(html, paragraph);
        if (!inList) {
          html.append("<ul>\n");
          inList = true;
        }
        html.append("<li>").append(processInlineMarkdown(trimmed.substring(2))).append("</li>\n");
      }
      // Handle ordered lists
      else if (trimmed.matches("^\\d+\\.\\s+.*")) {
        flushParagraph(html, paragraph);
        if (!inList) {
          html.append("<ol>\n");
          inList = true;
        }
        String content = trimmed.replaceFirst("^\\d+\\.\\s+", "");
        html.append("<li>").append(processInlineMarkdown(content)).append("</li>\n");
      }
      // Handle empty lines
      else if (trimmed.isEmpty()) {
        if (paragraph.length() > 0) {
          flushParagraph(html, paragraph);
        }
        closeLists(html, inList);
        inList = false;
      }
      // Regular paragraph text
      else {
        closeLists(html, inList);
        inList = false;
        if (paragraph.length() > 0) {
          paragraph.append(" ");
        }
        paragraph.append(trimmed);
      }
    }

    // Flush any remaining content
    flushParagraph(html, paragraph);
    closeLists(html, inList);

    return html.toString();
  }

  private static void flushParagraph(StringBuilder html, StringBuilder paragraph) {
    if (paragraph.length() > 0) {
      html.append("<p>").append(processInlineMarkdown(paragraph.toString())).append("</p>\n");
      paragraph.setLength(0);
    }
  }

  private static void closeLists(StringBuilder html, boolean inList) {
    if (inList) {
      // Check if it's an ordered or unordered list by looking at the last few characters
      String recent = html.substring(Math.max(0, html.length() - 50));
      if (recent.contains("<ol>")) {
        html.append("</ol>\n");
      } else if (recent.contains("<ul>")) {
        html.append("</ul>\n");
      }
    }
  }

  /// Process inline markdown like `**bold**`, `*italic*`, `` `code` ``, images, etc.
  ///
  /// @param text the text to process
  /// @return HTML with inline markdown converted
  private static String processInlineMarkdown(String text) {
    String result = HtmlEscaper.escape(text);

    // Images: ![alt](url) - process before bold/italic to avoid conflicts with escaping
    result =
        result.replaceAll(
            "!\\[([^\\]]*)\\]\\(([^)]+)\\)", "<img src=\"$2\" alt=\"$1\" class=\"story-image\" />");

    // Bold: **text** or __text__
    result = result.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
    result = result.replaceAll("__(.+?)__", "<strong>$1</strong>");

    // Italic: *text* or _text_
    result = result.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
    result = result.replaceAll("_(.+?)_", "<em>$1</em>");

    // Inline code: `code`
    result = result.replaceAll("`(.+?)`", "<code>$1</code>");

    return result;
  }

  /// Process standalone images (images on their own line)
  ///
  /// @param markdown the markdown image syntax
  /// @return HTML image tag with proper styling
  private static String processImage(String markdown) {
    // Extract alt text and URL from ![alt](url)
    java.util.regex.Pattern pattern =
        java.util.regex.Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    java.util.regex.Matcher matcher = pattern.matcher(markdown);
    if (matcher.find()) {
      String alt = matcher.group(1);
      String url = matcher.group(2);
      return "<div class=\"image-container\"><img src=\""
          + HtmlEscaper.escape(url)
          + "\" alt=\""
          + HtmlEscaper.escape(alt)
          + "\" class=\"story-image\" /></div>";
    }
    return "";
  }
}
