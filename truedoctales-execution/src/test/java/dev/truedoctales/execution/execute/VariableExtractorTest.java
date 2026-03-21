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
    String pattern = "Count is ${count}";
    String scenarioName = "Count is *5*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertTrue(extractor.matches(pattern, scenarioName));
    assertNotNull(variables);
    assertEquals(1, variables.size());
    assertEquals("5", variables.get("count"));
  }

  @Test
  void extractVariables_shouldExtractMultipleVariables() {
    String pattern = "Person ${title} is ${age} years old";
    String scenarioName = "Person *John* is *30* years old";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals(2, variables.size());
    assertEquals("John", variables.get("title"));
    assertEquals("30", variables.get("age"));
  }

  @Test
  void extractVariables_shouldExtractVariableWithSpaces() {
    String pattern = "Name is ${title}";
    String scenarioName = "Name is *John Doe*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals("John Doe", variables.get("title"));
  }

  @Test
  void extractVariables_shouldReturnNullWhenNoVariablesInPattern() {
    String pattern = "Simple pattern";
    String scenarioName = "Simple pattern";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNull(variables);
  }

  @Test
  void extractVariables_shouldReturnNullWhenNoMatch() {
    String pattern = "Count is ${count}";
    String scenarioName = "Total is *5*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNull(variables);
  }

  @Test
  void extractVariables_shouldExtractVariableAtStart() {
    String pattern = "${user} logged in";
    String scenarioName = "*Alice* logged in";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals("Alice", variables.get("user"));
  }

  @Test
  void extractVariables_shouldExtractVariableAtEnd() {
    String pattern = "User is ${user}";
    String scenarioName = "User is *Bob*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals("Bob", variables.get("user"));
  }

  @Test
  void extractVariables_shouldExtractVariableWithSpecialCharacters() {
    String pattern = "Price is ${price}";
    String scenarioName = "Price is *$100.50*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals("$100.50", variables.get("price"));
  }

  @Test
  void extractVariables_shouldHandleMultipleConsecutiveVariables() {
    String pattern = "${first}${second}";
    String scenarioName = "*value1**value2*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
  }

  @Test
  void extractVariables_shouldNotThrowOnEmptyVariableValue() {
    String pattern = "Name is ${title} and age is ${age}";
    String scenarioName = "Name is ** and age is *30*";

    assertDoesNotThrow(() -> extractor.extractVariables(pattern, scenarioName));
  }

  @Test
  void extractVariables_shouldHandlePatternWithEscapableCharacters() {
    String pattern = "Value is ${value} (in dollars)";
    String scenarioName = "Value is *100* (in dollars)";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals("100", variables.get("value"));
  }

  @Test
  void extractVariables_shouldHandleSpecialRegexCharactersInValue() {
    String pattern = "Count is ${count}";
    String scenarioName = "Count is *[invalid*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals("[invalid", variables.get("count"));
  }

  @Test
  void matches_shouldRejectBareValuesWithoutItalicMarkers() {
    String pattern = "Count is ${count}";
    String scenarioName = "Count is 5";

    assertFalse(extractor.matches(pattern, scenarioName), "Bare values without *...*  should fail");
  }

  @Test
  void matchesLegacyFormat_shouldDetectLegacyFormat() {
    String pattern = "Count is ${count}";
    String scenarioName = "Count is 5";

    assertTrue(
        extractor.matchesLegacyFormat(pattern, scenarioName),
        "Legacy format should be detected for error messages");
  }

  @Test
  void matchesLegacyFormat_shouldReturnFalseForNonVariablePatterns() {
    String pattern = "Simple pattern";
    String scenarioName = "Different text";

    assertFalse(extractor.matchesLegacyFormat(pattern, scenarioName));
  }

  @Test
  void matches_shouldMatchPlaceholderFormat() {
    String pattern = "Greet ${name}";
    String scenarioName = "Greet *${name}*";

    assertTrue(extractor.matches(pattern, scenarioName), "*${name}* should match as placeholder");
  }

  @Test
  void extractVariables_shouldExtractPlaceholderName() {
    String pattern = "Greet ${name}";
    String scenarioName = "Greet *${name}*";

    Map<String, String> variables = extractor.extractVariables(pattern, scenarioName);

    assertNotNull(variables);
    assertEquals("${name}", variables.get("name"), "Placeholder should be extracted as ${name}");
  }
}
