package dev.truedoctales.api.model.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExecutionStatusCalculator")
class ExecutionStatusCalculatorTest {

  @Nested
  @DisplayName("computeStatus()")
  class ComputeStatus {

    @Test
    @DisplayName("should return SUCCESS when list is empty")
    void shouldReturnSuccessWhenListIsEmpty() {
      // Arrange
      List<TestResult> results = List.of();

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should return SUCCESS when all results are SUCCESS")
    void shouldReturnSuccessWhenAllResultsAreSuccess() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should return SUCCESS when single result is SUCCESS")
    void shouldReturnSuccessWhenSingleResultIsSuccess() {
      // Arrange
      List<TestResult> results = List.of(new TestResult(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should return FAILURE when single result is FAILURE")
    void shouldReturnFailureWhenSingleResultIsFailure() {
      // Arrange
      List<TestResult> results = List.of(new TestResult(ExecutionStatus.FAILURE));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.FAILURE, status);
    }

    @Test
    @DisplayName("should return FAILURE when one result is FAILURE and rest are SUCCESS")
    void shouldReturnFailureWhenOneResultIsFailureAndRestAreSuccess() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.FAILURE),
              new TestResult(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.FAILURE, status);
    }

    @Test
    @DisplayName("should return FAILURE when all results are FAILURE")
    void shouldReturnFailureWhenAllResultsAreFailure() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.FAILURE),
              new TestResult(ExecutionStatus.FAILURE),
              new TestResult(ExecutionStatus.FAILURE));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.FAILURE, status);
    }

    @Test
    @DisplayName("should return ERROR when single result is ERROR")
    void shouldReturnErrorWhenSingleResultIsError() {
      // Arrange
      List<TestResult> results = List.of(new TestResult(ExecutionStatus.ERROR));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should return ERROR when one result is ERROR and rest are SUCCESS")
    void shouldReturnErrorWhenOneResultIsErrorAndRestAreSuccess() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.ERROR),
              new TestResult(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should return ERROR when all results are ERROR")
    void shouldReturnErrorWhenAllResultsAreError() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.ERROR),
              new TestResult(ExecutionStatus.ERROR),
              new TestResult(ExecutionStatus.ERROR));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should prioritize ERROR over FAILURE")
    void shouldPrioritizeErrorOverFailure() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.FAILURE),
              new TestResult(ExecutionStatus.ERROR),
              new TestResult(ExecutionStatus.FAILURE));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should prioritize ERROR over SUCCESS")
    void shouldPrioritizeErrorOverSuccess() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.ERROR),
              new TestResult(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should prioritize FAILURE over SUCCESS")
    void shouldPrioritizeFailureOverSuccess() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.FAILURE),
              new TestResult(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.FAILURE, status);
    }

    @Test
    @DisplayName("should prioritize ERROR when mixed with FAILURE and SUCCESS")
    void shouldPrioritizeErrorWhenMixedWithFailureAndSuccess() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.FAILURE),
              new TestResult(ExecutionStatus.ERROR),
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.FAILURE));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }

    @Test
    @DisplayName("should handle SKIPPED status as SUCCESS")
    void shouldHandleSkippedStatusAsSuccess() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SUCCESS),
              new TestResult(ExecutionStatus.SKIPPED),
              new TestResult(ExecutionStatus.SUCCESS));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.SUCCESS, status);
    }

    @Test
    @DisplayName("should prioritize FAILURE over SKIPPED")
    void shouldPrioritizeFailureOverSkipped() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SKIPPED),
              new TestResult(ExecutionStatus.FAILURE),
              new TestResult(ExecutionStatus.SKIPPED));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.FAILURE, status);
    }

    @Test
    @DisplayName("should prioritize ERROR over SKIPPED")
    void shouldPrioritizeErrorOverSkipped() {
      // Arrange
      List<TestResult> results =
          List.of(
              new TestResult(ExecutionStatus.SKIPPED),
              new TestResult(ExecutionStatus.ERROR),
              new TestResult(ExecutionStatus.SKIPPED));

      // Act
      ExecutionStatus status = ExecutionStatusCalculator.computeStatus(results);

      // Assert
      assertEquals(ExecutionStatus.ERROR, status);
    }
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should throw AssertionError when instantiated")
    void shouldThrowAssertionErrorWhenInstantiated() throws Exception {
      // Arrange
      var constructor = ExecutionStatusCalculator.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      // Act & Assert
      var exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
      assertEquals(AssertionError.class, exception.getCause().getClass());
      assertEquals("Utility class should not be instantiated", exception.getCause().getMessage());
    }
  }

  // Test implementation of HasExecutionStatus for testing purposes
  private record TestResult(ExecutionStatus status) implements HasExecutionStatus {}
}
