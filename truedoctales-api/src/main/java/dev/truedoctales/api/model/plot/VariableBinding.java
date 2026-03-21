package dev.truedoctales.api.model.plot;

/// Represents the metadata of a single variable or table-header binding.
///
/// Serialised into {@code plot-registry.json} as part of the {@code inplaceVariables} or
/// {@code tableVariables} arrays of each step binding.
///
/// @param name        the variable / column name (matches the pattern placeholder or table header)
/// @param type        the Java type simple name (e.g. {@code "String"}, {@code "Long"})
/// @param description human-readable description for documentation
public record VariableBinding(String name, String type, String description) {}
