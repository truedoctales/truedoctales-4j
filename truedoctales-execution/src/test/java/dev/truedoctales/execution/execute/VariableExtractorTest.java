package dev.truedoctales.execution.execute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VariableExtractorTest {

  private VariableExtractor extractor;

  @BeforeEach
  void setUp() {
    extractor = new VariableExtractor();
  }

  @Test
  void extractVariables_shouldExtractSingleVariable() {
    // Arrange
    String pattern = "Count is ${count}";
    String scenarioName = "Count is 5";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertTrue(extractor.matches(pattern, scenarioName));
    assertNotNull(variables);
    assertEquals(1, variables.size());
    assertEquals("5", variables.get("count"));
  }

  @Test
  void extractVariables_shouldExtractMultipleVariables() {
    // Arrange
    String pattern = "Person ${title} is ${age} years old";
    String scenarioName = "Person John is 30 years old";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
    assertEquals(2, variables.size());
    assertEquals("John", variables.get("title"));
    assertEquals("30", variables.get("age"));
  }

  @Test
  void extractVariables_shouldExtractVariableWithSpaces() {
    // Arrange
    String pattern = "Name is ${title}";
    String scenarioName = "Name is John Doe";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
    assertEquals("John Doe", variables.get("title"));
  }

  @Test
  void extractVariables_shouldReturnNullWhenNoVariablesInPattern() {
    // Arrange
    String pattern = "Simple pattern";
    String scenarioName = "Simple pattern";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNull(variables);
  }

  @Test
  void extractVariables_shouldReturnNullWhenNoMatch() {
    // Arrange
    String pattern = "Count is ${count}";
    String scenarioName = "Total is 5";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNull(variables);
  }

  @Test
  void extractVariables_shouldExtractVariableAtStart() {
    // Arrange
    String pattern = "${user} logged in";
    String scenarioName = "Alice logged in";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
    assertEquals("Alice", variables.get("user"));
  }

  @Test
  void extractVariables_shouldExtractVariableAtEnd() {
    // Arrange
    String pattern = "User is ${user}";
    String scenarioName = "User is Bob";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
    assertEquals("Bob", variables.get("user"));
  }

  @Test
  void extractVariables_shouldExtractVariableWithSpecialCharacters() {
    // Arrange
    String pattern = "Price is ${price}";
    String scenarioName = "Price is $100.50";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
    assertEquals("$100.50", variables.get("price"));
  }

  @Test
  void extractVariables_shouldHandleMultipleConsecutiveVariables() {
    // Arrange
    String pattern = "${first}${second}";
    String scenarioName = "value1value2";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
  }

  @Test
  void extractVariables_shouldNotThrowOnEmptyVariableValue() {
    // Arrange
    String pattern = "Name is ${title} and age is ${age}";
    String scenarioName = "Name is  and age is 30";

    // Act & Assert
    assertDoesNotThrow(() -> extractor.extractVariables(pattern, scenarioName));
  }

  @Test
  void extractVariables_shouldHandlePatternWithEscapableCharacters() {
    // Arrange
    String pattern = "Value is ${value} (in dollars)";
    String scenarioName = "Value is 100 (in dollars)";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
    assertEquals("100", variables.get("value"));
  }

  @Test
  void extractVariables_shouldHandleSpecialRegexCharactersInValue() {
    // Arrange
    String pattern = "Count is ${count}";
    String scenarioName = "Count is [invalid";

    // Act
    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    // Assert
    assertNotNull(variables);
    assertEquals("[invalid", variables.get("count"));
  }
}
