package dev.truedoctales.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// ## Var Annotation
///
/// Binds a variable name and description directly to a step method parameter.
///
/// Use this annotation on method parameters to declare the column/variable name and its
/// description in one place. The name is used as the column header in generated documentation
/// and the description appears in the variable/header table of the plot glossary.
///
/// ### Usage
/// ```java
/// @Step(value = "Create hero")
/// void createHero(
///     @Var(value = "id", description = "Unique identifier") Long id,
///     @Var(value = "name", description = "Hero name") String name) {
///     // step logic
/// }
/// ```
///
/// When {@code @Var} annotations are present on parameters, they take precedence over
/// {@link Step#headers()} for determining column names.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Table {

  Variable[] headers() default {};
}
