package dev.truedoctales.api.model.execution;

import java.util.List;

/// Execution model for a scene within a story.
///
/// Contains the scene metadata, optional description, the list of binding executions to be
// performed,
/// and the original binding list (including both StepTask and StepDescription instances).
public record SceneExecution(String title, Integer lineNumber, List<StepExecution> steps) {}
