package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Header Annotation
///
/// Optional annotation to document the expected table columns for a binding method.
///
/// This annotation is purely for documentation purposes and is not required for the framework to
/// function. It helps developers understand what table columns are expected when a binding method
/// accepts table data alongside variable placeholders.
///
/// ### Usage
/// ```java
/// @Step("Find lifeforms with skill ${skill}")
/// @Header({"name", "species"})
/// public void findLifeformsBySkill(String skill, List<Map<String, String>> expectedResults) {
///     // Method implementation
/// }
/// ```
///
/// In the markdown story, this would correspond to:
/// ```markdown
/// ### Lifeform: Find lifeforms with skill Mathematics
///
/// | name | species |
/// |------|---------|
/// | Ada  | Human   |
/// | Alan | Human   |
/// ```
///
/// @see Step
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Header {
  /// The expected column names in the table data.
  ///
  /// @return array of column names
  String[] value();
}
