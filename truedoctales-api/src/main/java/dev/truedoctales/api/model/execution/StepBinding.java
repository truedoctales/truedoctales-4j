package dev.truedoctales.api.model.execution;

/// Represents the binding between a binding pattern and its execution method.
///
/// Associates a plot name and binding pattern with the Java method that implements the binding.
public record StepBinding(String plot, String pattern, InputType inputType) {}
