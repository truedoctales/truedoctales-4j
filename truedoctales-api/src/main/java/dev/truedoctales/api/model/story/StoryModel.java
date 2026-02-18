package dev.truedoctales.api.model.story;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Domain model representing a test story. A Story is equivalent to a TestClass in JUnit.
///
/// A Story contains:
/// - Prequel references (setup stories to run before this story)
/// - Scenes (equivalent to test methods in JUnit)
/// - Plots (reusable method bindings)
/// - Steps executed within Scenes
///
/// A Story may optionally contain an introTitle and summary that provide context and description
/// for the story. These are extracted from the markdown content before the "## Story" marker.
public record StoryModel(
    @NonNull Path path,
    @NonNull String title,
    @Nullable String summary,
    @NonNull List<Path> prequels,
    @NonNull List<SceneModel> scenes) {}
