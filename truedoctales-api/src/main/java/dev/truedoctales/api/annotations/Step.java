package dev.truedoctales.api.annotations;

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
}
