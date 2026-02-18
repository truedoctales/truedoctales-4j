package dev.truedoctales.api.model.story;

/// Represents a call to a binding with its plot name and binding value.
///
/// Used to bind a binding invocation to a specific plot and binding method during execution.
public record StepCall(String plotName, String stepValue) {}
