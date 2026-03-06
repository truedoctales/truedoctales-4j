package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Variable Annotation
///
/// Binds a variable name and description directly to a step method parameter.
///
/// Each parameter of a {@link Step}-annotated method that is extracted from the step pattern
/// (e.g. {@code ${name}}) should carry a {@code @Variable} annotation so that the generated
/// plot glossary can display its name, Java type, and a human-readable description.
///
/// The annotation's {@link #value()} is the variable or column header name (must match the
/// placeholder name in the pattern), and {@link #description()} provides an optional
/// documentation string rendered in the Variables table of the generated report.
///
/// ### Usage
/// ```java
/// @Step(value = "Create hero",
///       description = "Creates a new hero with the given attributes.")
/// void createHero(
///     @Variable(value = "id", description = "Unique identifier") Long id,
///     @Variable(value = "name", description = "Hero name") String name,
///     @Variable(value = "species", description = "Species of the hero") String species,
///     @Variable(value = "age", description = "Age in years") Integer age) {
///     // step logic
/// }
/// ```
///
/// The extracted metadata is serialised into {@code plot-registry.json} as an array of
/// {@code {"name", "type", "description"}} objects under the {@code variables} key of each
/// step binding.
///
/// @see Step
/// @see Table
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Variable {

  /// The variable or column header name.
  ///
  /// Must match the placeholder name in the step pattern (e.g. {@code ${name}}) or
  /// the column header when used inside a {@link Table} annotation.
  ///
  /// @return the variable name
  String value();

  /// Optional description of the variable.
  ///
  /// The description is included in the generated plot glossary documentation in the
  /// Description column of the variable/header table.
  ///
  /// @return the variable description, defaults to empty string
  String description() default "";
}
