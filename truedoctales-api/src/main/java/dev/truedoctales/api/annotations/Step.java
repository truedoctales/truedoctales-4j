package dev.truedoctales.api.annotations;

import dev.truedoctales.api.model.execution.InputType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Step Annotation
///
/// Marks a method as a binding in a story-based test scenario.
///
/// This annotation is used to define a test binding within a story. A binding can represent a
/// setup, action, or validation in the flow of the test. Steps are the fundamental building
/// blocks of test scenarios that are bound to markdown story files.
///
/// ### Usage
/// ```java
/// @Step("The user logs in with valid credentials")
/// void userLogsIn() {
///     // binding logic
/// }
/// ```
///
/// Apply this annotation to methods that represent important steps in your test story.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Step {
  String value();

  /// Optional markdown description of what this step does.
  ///
  /// The description is included in the final generated report, rendered as a blockquote line
  /// directly below the step annotation. Supports standard markdown formatting.
  String description() default "";

  /// The input type for this step. Defaults to {@link InputType#AUTO}, which auto-detects the
  /// type from the method's parameter types (BATCH when a {@link java.util.Collection} parameter
  /// is present, SEQUENCE otherwise).
  ///
  /// Set this explicitly to override the auto-detected type.
  InputType type() default InputType.AUTO;
}
