package dev.truedoctales.api.annotations;

import dev.truedoctales.api.execute.LoggingStoryExecutionListener;
import dev.truedoctales.api.execute.StoryExecutionListener;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## StoryBook Annotation
///
/// Marks a test class as using a story book directory for story-based testing.
///
/// This annotation configures the path to the story book directory and optional execution
/// listeners that will be notified of test execution events.
///
/// ### Usage
/// ```java
/// @StoryBook(path = "book-of-stories")
/// public class MyStoryTest {
///     // Test implementation
/// }
/// ```
///
/// @see StoryExecutionListener
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StoryBook {

  /// Path to the story book directory. Can be absolute or relative to the project root.
  ///
  /// @return the path to the story book
  String path() default "book-of-stories";

  /// Execution listeners to be notified of story execution events.
  ///
  /// @return array of listener classes
  Class<? extends StoryExecutionListener>[] listener() default {
    LoggingStoryExecutionListener.class
  };
}
