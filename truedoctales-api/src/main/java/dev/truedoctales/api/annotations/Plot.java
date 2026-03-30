package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Plot Annotation
///
/// Marks a class as a plot — a reusable collection of {@link Step}-annotated methods that
/// can be bound to markdown story files.
///
/// Plots group related step bindings together and provide them to the story executor
/// via the {@link dev.truedoctales.api.execute.PlotRegistry PlotRegistry}. The plot's
/// {@link #value()} is used as its identifier in the generated {@code plot-registry.json}
/// and in the plot glossary.
///
/// ### Usage
/// ```java
/// @Plot("Hero")
/// public class HeroPlot {
///
///     @Step(value = "Create hero",
///           description = "Creates a new hero with the given attributes.")
///     void createHero(
///         @Variable(value = "id", description = "Unique identifier") Long id,
///         @Variable(value = "name", description = "Hero name") String name) {
///         // setup logic
///     }
///
///     @Step("Hero exists")
///     void heroExists(
///         @Variable(value = "name", description = "Hero name to look up") String name) {
///         // validation logic
///     }
/// }
/// ```
///
/// Apply this annotation to classes that define reusable method bindings for your test stories.
///
/// @see Step
/// @see Variable
/// @see Table
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plot {
  /// The title or identifier of the plot.
  ///
  /// Use a descriptive title to indicate the purpose or theme of this plot.
  ///
  /// @return the plot title
  String value();
}
