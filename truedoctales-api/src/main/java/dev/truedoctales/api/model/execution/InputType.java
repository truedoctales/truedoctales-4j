package dev.truedoctales.api.model.execution;

public enum InputType {
  /// Auto-detect the input type from the method's parameter types. Default for {@code
  // @Step.type()}.
  AUTO,
  NONE,
  SEQUENCE,
  BATCH
}
