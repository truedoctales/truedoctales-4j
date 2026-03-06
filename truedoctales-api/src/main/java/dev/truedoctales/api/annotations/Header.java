package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Header Annotation
///
/// Binds a header name and description to a method parameter that receives data from a table
/// column.
///
/// Use this on parameters that receive individual column values from tabular step data. The
/// header name must match the column header used in the markdown table that accompanies the
/// step invocation.
///
/// ### Usage
/// ```java
/// @Step("Create hero")
/// void createHero(
///     @Header(name = "id", description = "Unique identifier") Long id,
///     @Header(name = "name", description = "Hero name") String name) {
///     // step logic
/// }
/// ```
///
/// @see Variable
/// @see Table
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Header {

  /// The name of the header to bind.
  ///
  /// Must match the column header used in the markdown table that accompanies the step.
  ///
  /// @return the header name
  String name();

  /// A description of the header for documentation purposes.
  ///
  /// Rendered in the Headers table of the generated plot glossary.
  ///
  /// @return the header description, defaults to empty string
  String description() default "";
}
