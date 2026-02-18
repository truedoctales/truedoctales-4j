package dev.truedoctales.api.model.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StepTaskTest {

  @Test
  void shouldCreateStepTaskWithValidInputs() {
    // Arrange
    int lineNumber = 42;
    StepCall stepCall = new StepCall("Test Plot", "Test Description");
    List<Map<String, String>> inputRows =
        List.of(Map.of("key1", "value1"), Map.of("key2", "value2"));

    // Act
    StepTask stepTask = new StepTask(lineNumber, stepCall, inputRows);

    // Assert
    assertEquals(42, stepTask.lineNumber());
    assertEquals(stepCall, stepTask.call());
    assertEquals(inputRows, stepTask.inputRows());
    assertEquals(2, stepTask.inputRows().size());
  }

  @Test
  void shouldDefaultToEmptyListWhenInputRowsIsNull() {
    // Arrange
    int lineNumber = 10;
    StepCall stepCall = new StepCall("Test Plot", "Test Description");

    // Act
    StepTask stepTask = new StepTask(lineNumber, stepCall, null);

    // Assert
    assertNotNull(stepTask.inputRows());
    assertTrue(stepTask.inputRows().isEmpty());
    assertEquals(10, stepTask.lineNumber());
    assertEquals(stepCall, stepTask.call());
  }

  @Test
  void shouldDefaultToZeroWhenLineNumberIsNegative() {
    // Arrange
    int negativeLineNumber = -5;
    StepCall stepCall = new StepCall("Test Plot", "Test Description");
    List<Map<String, String>> inputRows = List.of(Map.of("key", "value"));

    // Act
    StepTask stepTask = new StepTask(negativeLineNumber, stepCall, inputRows);

    // Assert
    assertEquals(0, stepTask.lineNumber());
    assertEquals(stepCall, stepTask.call());
    assertEquals(inputRows, stepTask.inputRows());
  }

  @Test
  void shouldPreserveZeroWhenLineNumberIsZero() {
    // Arrange
    int lineNumber = 0;
    StepCall stepCall = new StepCall("Test Plot", "Test Description");
    List<Map<String, String>> inputRows = List.of();

    // Act
    StepTask stepTask = new StepTask(lineNumber, stepCall, inputRows);

    // Assert
    assertEquals(0, stepTask.lineNumber());
    assertEquals(stepCall, stepTask.call());
    assertEquals(inputRows, stepTask.inputRows());
  }

  @Test
  void shouldCreateStepTaskWithTwoParameterConstructor() {
    // Arrange
    int lineNumber = 25;
    StepCall stepCall = new StepCall("Test Plot", "Test Description");

    // Act
    StepTask stepTask = new StepTask(lineNumber, stepCall);

    // Assert
    assertEquals(25, stepTask.lineNumber());
    assertEquals(stepCall, stepTask.call());
    assertNotNull(stepTask.inputRows());
    assertTrue(stepTask.inputRows().isEmpty());
  }

  @Test
  void shouldProvideAccessToAllFieldsViaRecordAccessors() {
    // Arrange
    int lineNumber = 100;
    StepCall stepCall = new StepCall("User Operations", "Create user");
    List<Map<String, String>> inputRows =
        List.of(Map.of("name", "John", "email", "john@example.com"));

    // Act
    StepTask stepTask = new StepTask(lineNumber, stepCall, inputRows);

    // Assert
    assertEquals(100, stepTask.lineNumber());
    assertEquals("User Operations", stepTask.call().plotName());
    assertEquals("Create user", stepTask.call().stepValue());
    assertEquals(1, stepTask.inputRows().size());
    assertEquals("John", stepTask.inputRows().get(0).get("name"));
    assertEquals("john@example.com", stepTask.inputRows().get(0).get("email"));
  }

  @Test
  void shouldHandleTwoParameterConstructorWithNegativeLineNumber() {
    // Arrange
    int negativeLineNumber = -10;
    StepCall stepCall = new StepCall("Test Plot", "Test Description");

    // Act
    StepTask stepTask = new StepTask(negativeLineNumber, stepCall);

    // Assert
    assertEquals(0, stepTask.lineNumber());
    assertEquals(stepCall, stepTask.call());
    assertTrue(stepTask.inputRows().isEmpty());
  }

  @Test
  void shouldHandleEmptyInputRowsList() {
    // Arrange
    int lineNumber = 15;
    StepCall stepCall = new StepCall("Test Plot", "Test Description");
    List<Map<String, String>> emptyInputRows = List.of();

    // Act
    StepTask stepTask = new StepTask(lineNumber, stepCall, emptyInputRows);

    // Assert
    assertEquals(15, stepTask.lineNumber());
    assertEquals(stepCall, stepTask.call());
    assertNotNull(stepTask.inputRows());
    assertTrue(stepTask.inputRows().isEmpty());
  }

  @Test
  void shouldHandleInputRowsWithMultipleColumns() {
    // Arrange
    int lineNumber = 50;
    StepCall stepCall = new StepCall("Data Plot", "Process data");
    List<Map<String, String>> inputRows =
        List.of(
            Map.of("col1", "a", "col2", "b", "col3", "c"),
            Map.of("col1", "d", "col2", "e", "col3", "f"));

    // Act
    StepTask stepTask = new StepTask(lineNumber, stepCall, inputRows);

    // Assert
    assertEquals(50, stepTask.lineNumber());
    assertEquals(2, stepTask.inputRows().size());
    assertEquals(3, stepTask.inputRows().get(0).size());
    assertEquals("a", stepTask.inputRows().get(0).get("col1"));
    assertEquals("f", stepTask.inputRows().get(1).get("col3"));
  }
}
