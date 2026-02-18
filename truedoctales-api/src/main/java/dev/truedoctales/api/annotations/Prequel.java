package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Prequel Annotation
///
/// Marks a reference to a prequel story that should be executed before the main story.
///
/// This annotation is used in the "## Intro:" section of a story to load and execute
/// prequel stories that set up the necessary test context. Prequels are stories that
/// run before the main story to establish preconditions.
///
/// ### Usage
/// ```markdown
/// ## Intro:
///
/// @Prequel "setup-countries.md"
/// @Plot "User Operations"
/// ```
///
/// Apply this annotation in markdown stories to reference prequel setup stories.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Prequel {
  /// The path or reference to the prequel story file.
  ///
  /// @return the prequel story reference
  String value();
}
