package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Scene Annotation
///
/// Marks a test method as a scene within a code-based story.
///
/// Scenes are the main sections of a story, similar to markdown `## Scene:` tableVariables.
/// Each scene can have a title and description, and contains the test logic that
/// will be executed and documented.
///
/// ### Usage
/// ```java
/// @Story(book = "My Book", chapter = "Chapter One", name = "My Story")
/// public class MyStoryTest {
///
///     @Scene(
///         title = "User Login",
///         description = "The user attempts to log in with valid credentials"
///     )
///     @Test
///     void userLoginScene() {
///         // Test logic
///     }
///
///     @Scene(title = "Verify Dashboard")
///     @ParameterizedTest
///     @CsvSource({"admin,true", "user,false"})
///     void verifyDashboard(String role, boolean canEdit) {
///         // Parameterized test logic - will generate a table in the documentation
///     }
/// }
/// ```
///
/// Apply this annotation to test methods within a @Story class.
/// For parameterized tests, a table will be automatically generated in the documentation.
///
/// @see Story
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scene {
  /// The title of the scene.
  ///
  /// This will appear as a heading in the generated documentation.
  ///
  /// @return the scene title
  String title();

  /// Optional description of the scene in markdown format.
  ///
  /// This can include markdown formatting and will be rendered in the documentation
  /// under the scene title.
  ///
  /// @return the scene description, defaults to empty string
  String description() default "";
}
