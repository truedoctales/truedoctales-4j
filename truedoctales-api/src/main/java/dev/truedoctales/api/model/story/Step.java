package dev.truedoctales.api.model.story;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/// Sealed interface representing elements within a scene.
///
/// A Step can be either:
/// - StepTask: An executable binding that calls a plot method
/// - StepDescription: Markdown documentation/commentary between steps
///
/// This sealed hierarchy allows scenes to contain both executable steps and descriptive
/// documentation in the order they appear in the markdown file.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = StepTask.class, name = "task"),
  @JsonSubTypes.Type(value = StepDescription.class, name = "description")
})
public sealed interface Step permits StepTask, StepDescription {}
