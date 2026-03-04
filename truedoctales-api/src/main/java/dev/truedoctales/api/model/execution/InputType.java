package dev.truedoctales.api.model.execution;

/// Defines how input data is supplied to a {@code @Step} method.
///
/// <p>The input type determines how tabular data rows from a story are mapped to method
/// invocations:
/// <ul>
///   <li><b>SEQUENCE</b> – the step is invoked once per data row. Each row supplies the
///       method parameters individually. Use sequence when the method accepts simple
///       scalar parameters (e.g., {@code String}, {@code Integer}).</li>
///   <li><b>BATCH</b> – the step is invoked once with a {@link java.util.Collection} that
///       contains all data rows. Use batch when the method accepts a
///       {@link java.util.Collection} parameter.</li>
/// </ul>
public enum InputType {
  /// Auto-detect the input type from the method's parameter types.
  ///
  /// When a {@link java.util.Collection} parameter is present, the type resolves to
  /// {@link #BATCH}; otherwise it resolves to {@link #SEQUENCE}.
  /// This is the default for {@code @Step.type()}.
  AUTO,

  /// The step accepts no input data.
  NONE,

  /// The step is invoked once per data row; each row supplies the method parameters
  /// individually. Use this for methods with scalar parameters.
  SEQUENCE,

  /// The step is invoked once with all data rows passed as a {@link java.util.Collection}.
  /// Use this for methods that accept a collection parameter.
  BATCH
}
