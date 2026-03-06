package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Step Annotation
///
/// Marks a method as a step binding in a story-based test scenario.
///
/// A step represents a single action, setup, or assertion in the flow of a test story.
/// Steps are the fundamental building blocks of test scenarios and are bound to markdown
/// story files via their pattern string.
///
/// Method parameters annotated with {@link Variable} become the step's variable bindings,
/// and any parameter annotated with {@link Table} is the receiver of tabular input data.
/// The extracted metadata (variables, headers, descriptions, types) is serialised into
/// {@code plot-registry.json} and rendered in the generated plot glossary and HTML report.
///
/// ### Usage
/// ```java
/// @Step(value = "Create hero",
///       description = "Creates a new hero with the given attributes.")
/// void createHero(
///     @Variable(value = "id", description = "Unique identifier") Long id,
///     @Variable(value = "name", description = "Hero name") String name) {
///     // step logic
/// }
/// ```
///
/// Apply this annotation to methods inside a {@link Plot}-annotated class.
///
/// @see Plot
/// @see Variable
/// @see Table
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Step {
  /// The step pattern string.
  ///
  /// The pattern can contain {@code ${variable}} placeholders that are matched against
  /// the step text in the markdown story file. Each placeholder must have a corresponding
  /// method parameter annotated with {@link Variable}.
  ///
  /// @return the step pattern
  String value();

  /// Optional markdown description of what this step does.
  ///
  /// The description is included in the generated plot glossary and HTML report, rendered
  /// below the step heading. Supports standard markdown formatting.
  ///
  /// @return the step description, defaults to empty string
  String description() default "";
}
