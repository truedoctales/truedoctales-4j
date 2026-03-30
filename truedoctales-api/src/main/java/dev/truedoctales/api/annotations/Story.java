package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Story Annotation
///
/// Marks a JUnit test class as a code-based story in the story-driven testing framework.
///
/// This annotation allows you to write tests directly in Java code instead of markdown,
/// while still producing the same narrative documentation. The story will be included in
/// the specified book and chapter alongside markdown-based stories.
///
/// ### Usage
/// ```java
/// @Story(book = "My Book", chapter = "Chapter One", name = "My First Story")
/// public class MyStoryTest {
///     @Scene(title = "Setup", description = "Initialize the test environment")
///     @Test
///     void setupScene() {
///         // Test logic
///     }
/// }
/// ```
///
/// Apply this annotation to test classes that represent complete stories.
/// The story will be organized in the documentation by book and chapter.
///
/// @see Scene
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Story {
  /// The name of the book this story belongs to.
  ///
  /// Stories with the same book name will be grouped together in the generated documentation.
  ///
  /// @return the book name
  String book();

  /// The chapter name within the book.
  ///
  /// Stories with the same book and chapter will be grouped together.
  ///
  /// @return the chapter name
  String storyPath();

  /// The name of this story.
  ///
  /// This will be the title of the story in the generated documentation.
  ///
  /// @return the story name
  String title();

  /// Optional summary or description of the story.
  ///
  /// @return the story summary, defaults to empty string
  String markdown() default "";
}
