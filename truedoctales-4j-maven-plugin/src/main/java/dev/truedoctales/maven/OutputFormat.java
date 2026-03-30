package dev.truedoctales.maven;

/// Supported report output formats.
///
/// Used by the Maven plugin to control which report types are generated.
public enum OutputFormat {
  /// Enriched Markdown report with execution status badges.
  MARKDOWN,

  /// Professional HTML report with navigation, styling, and Mermaid diagram rendering.
  HTML
}
