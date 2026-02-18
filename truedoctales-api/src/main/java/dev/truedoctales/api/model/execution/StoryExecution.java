package dev.truedoctales.api.model.execution;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Execution model for a story, containing all scenes and metadata.
///
/// Represents a complete story ready for execution with its scenes, prequels, and metadata.
public record StoryExecution(
    Path path,
    String title,
    @Nullable String summary,
    List<Path> prequels,
    List<SceneExecution> scenes) {}
