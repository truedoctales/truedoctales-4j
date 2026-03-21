package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Table Annotation
///
/// Marks a {@code List<Map<String, String>>} parameter as the receiver of tabular step data.
///
/// When a step is invoked with a markdown table, the rows are collected into the parameter
/// annotated with {@code @Table}. The presence of this annotation also switches the step's
/// input type to {@link dev.truedoctales.api.model.execution.InputType#BATCH BATCH}, meaning
/// all rows are delivered at once rather than one-at-a-time.
///
/// Optionally, {@link #headers()} can declare the expected column names and their descriptions
/// using nested {@link Variable} annotations. These are serialised into the {@code tableVariables}
/// array of the step binding in {@code plot-registry.json} and rendered in the generated plot
/// glossary.
///
/// ### Usage
/// ```java
/// @Step(value = "Greet ${name} ${count} times",
///       description = "Greets the person the given number of times and verifies the output.")
/// void greetSomeoneMultipleTimes(
///     @Variable(value = "name", description = "Name of the person to greet") String name,
///     @Variable(value = "count", description = "How many times to greet") Integer count,
///     @Table(tableVariables = { @Variable(value = "expected", description = "Expected greeting
// output")
// })
///         List<Map<String, String>> expected) {
///     // step logic
/// }
/// ```
///
/// @see Variable
/// @see Step
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Table {

  /// Optional column tableVariables for the table data.
  ///
  /// Each {@link Variable} declares the column name and an optional description. The metadata
  /// is included in the generated plot glossary under the Headers section of the step.
  ///
  /// @return array of column header definitions
  Variable[] headers() default {};
}
