package dev.truedoctales.api.model.story;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Domain model representing a Chapter - a logical grouping of related Stories.
///
/// In the file system, a Chapter is represented by a folder containing story markdown files. For
/// example: `01_chapter-countries/` is a chapter containing multiple story files.
///
/// A Chapter may optionally contain an intro.md file that provides a title and summary for the
/// chapter.
///
/// A Chapter contains StoryModels representing individual story files within the chapter
/// directory.
public record ChapterModel(
    @NonNull Path path,
    @NonNull String title,
    @NonNull List<StoryModel> stories) {}
