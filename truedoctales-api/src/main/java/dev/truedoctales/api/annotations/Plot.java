package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Plot Annotation
///
/// Marks a class as a plot containing methods that can be bound and reused in story-based testing.
///
/// This annotation is used to define a set of methods that represent a plot - a reusable collection
/// of test steps or actions. Plots can be linked to Java test classes to provide method bindings
/// for test steps, promoting code reuse and modularity in story-based testing.
///
/// ### Usage
/// ```java
/// @Plot("User Authentication")
/// public class AuthenticationPlot {
///     @Step("a user with username ${username} and password ${password}")
///     void createUser(String username, String password) {
///         // setup logic
///     }
///
///     @Step("the user attempts to log in")
///     void userLogsIn() {
///         // action logic
///     }
///
///     @Step("the user is successfully authenticated")
///     void verifyAuthentication() {
///         // validation logic
///     }
/// }
/// ```
///
/// Apply this annotation to classes that define reusable method bindings for your test stories.
/// Plots can be imported and used across multiple test classes.
///
/// @see Step
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
