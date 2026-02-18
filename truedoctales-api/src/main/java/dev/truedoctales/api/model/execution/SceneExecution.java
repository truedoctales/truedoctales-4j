package dev.truedoctales.api.model.execution;

import dev.truedoctales.api.model.story.Step;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Execution model for a scene within a story.
///
/// Contains the scene metadata, optional description, the list of binding executions to be
// performed,
/// and the original binding list (including both StepTask and StepDescription instances).
public record SceneExecution(
    String title,
    Integer integer,
    @Nullable String description,
    List<StepExecution> steps,
    List<Step> originalSteps) {}
