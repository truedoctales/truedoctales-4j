package dev.truedoctales.api.model.plot;

import java.util.List;

/// Represents the binding between a plot and its binding methods.
///
/// A plot binding associates a plot identifier with a list of binding bindings that can be invoked
/// during story execution.
public record PlotBinding(String plotId, List<StepBinding> steps) {}
