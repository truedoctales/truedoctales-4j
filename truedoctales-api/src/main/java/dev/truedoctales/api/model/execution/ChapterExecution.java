package dev.truedoctales.api.model.execution;

import java.nio.file.Path;
import java.util.List;

/// Execution model for a chapter, containing the chapter metadata and list of story executions.
///
/// Includes optional intro content (title and summary) parsed from chapter intro.md files.
public record ChapterExecution(Path path, String title, List<StoryExecution> stories) {}
