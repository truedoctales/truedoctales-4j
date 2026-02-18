package dev.truedoctales.parser;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.story.Step;
import dev.truedoctales.api.model.story.StepDescription;
import dev.truedoctales.api.model.story.StepTask;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StepParserTest {

  // Tests for simple step task parsing (no table data)

  @Test
  void parse_shouldParseBasicStepTask() {
    // Arrange
    StepParser parser = new StepParser("> **User Management** Create a new user", 1);

    // Act
    Step result = parser.build();

    // Assert
    assertNotNull(result);
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("User Management", stepTask.call().plotName());
    assertEquals("Create a new user", stepTask.call().stepValue());
    assertEquals(1, stepTask.lineNumber());
    assertTrue(stepTask.inputRows().isEmpty());
  }

  @Test
  void parse_shouldParseStepTaskWithMultipleWords() {
    // Arrange
    StepParser parser =
        new StepParser("> **Plot With Spaces** This is a step with multiple words", 5);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("Plot With Spaces", stepTask.call().plotName());
    assertEquals("This is a step with multiple words", stepTask.call().stepValue());
    assertEquals(5, stepTask.lineNumber());
  }

  @Test
  void parse_shouldHandleMissingPlotName() {
    // Arrange
    StepParser parser = new StepParser("> Just a story without plot name", 3);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("", stepTask.call().plotName());
    assertEquals("Just a story without plot name", stepTask.call().stepValue());
  }

  @Test
  void parse_shouldHandleMalformedBoldMarkup() {
    // Arrange
    StepParser parser = new StepParser("> **Unclosed Plot Some step", 2);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("", stepTask.call().plotName());
    assertEquals("**Unclosed Plot Some step", stepTask.call().stepValue());
  }

  // Tests for step description parsing (markdown content between steps)

  @Test
  void parse_shouldParseStepDescription() {
    // Arrange
    StepParser parser = new StepParser("This is a description line", 1);

    // Act
    Step result = parser.build();

    // Assert
    assertNotNull(result);
    assertTrue(result instanceof StepDescription);
    StepDescription description = (StepDescription) result;
    assertEquals("This is a description line", description.markdown());
    assertEquals(1, description.lineNumber());
  }

  @Test
  void parse_shouldReturnNullForEmptyDescription() {
    // Arrange
    StepParser parser = new StepParser("   ", 1);

    // Act
    Step result = parser.build();

    // Assert
    assertNull(result);
  }

  // Tests for table parsing with step tasks

  @Test
  void parse_shouldParseSingleTableRow() {
    // Arrange
    String block =
        """
        > **User Operations** Create users
        >
        > | name | email |
        > |------|-------|
        > | John | john@example.com |

        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals(1, stepTask.inputRows().size());

    Map<String, String> firstRow = stepTask.inputRows().get(0);
    assertEquals("John", firstRow.get("name"));
    assertEquals("john@example.com", firstRow.get("email"));
  }

  @Test
  void parse_shouldParseMultipleTableRows() {
    // Arrange
    String block =
        """
        > **User Operations** Create multiple users
        >
        > | name | email | age |
        > |------|-------|-----|
        > | John | john@example.com | 30 |
        > | Jane | jane@example.com | 25 |
        > | Bob | bob@example.com | 35 |
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals(3, stepTask.inputRows().size());

    assertEquals("John", stepTask.inputRows().get(0).get("name"));
    assertEquals("30", stepTask.inputRows().get(0).get("age"));
    assertEquals("Jane", stepTask.inputRows().get(1).get("name"));
    assertEquals("25", stepTask.inputRows().get(1).get("age"));
    assertEquals("Bob", stepTask.inputRows().get(2).get("name"));
    assertEquals("35", stepTask.inputRows().get(2).get("age"));
  }

  @Test
  void parse_shouldHandleTableWithEmptyCells() {
    // Arrange
    String block =
        """
        > **Operations** Process data
        >
        > | id | name | optional |
        > |----|----- |----------|
        > | 1 | Alice |  |
        > | 2 | Bob | value |
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals(2, stepTask.inputRows().size());

    Map<String, String> firstRow = stepTask.inputRows().get(0);
    assertEquals("1", firstRow.get("id"));
    assertEquals("Alice", firstRow.get("name"));
    // Empty cells are not included in the map, so optional key might not exist
    assertTrue(firstRow.size() >= 2);

    Map<String, String> secondRow = stepTask.inputRows().get(1);
    assertEquals("2", secondRow.get("id"));
    assertEquals("value", secondRow.get("optional"));
  }

  @Test
  void parse_shouldStopParsingWhenNonTableContentEncountered() {
    // Arrange
    String block =
        """
        > **Operations** Do something
        >
        > | id | name |
        > |----|----- |
        > | 1 | Alice |
        """;

    StepParser parser = new StepParser(block.strip().split("\\R")[0], 1);
    String[] lines = block.strip().split("\\R");
    for (int i = 1; i < lines.length; i++) {
      parser.parseLine(lines[i], 1 + i);
    }

    // Act
    boolean continueParsing = parser.parseLine("Next step or content", 10);
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    assertFalse(continueParsing); // Should return false indicating parsing is done
    StepTask stepTask = (StepTask) result;
    assertEquals(1, stepTask.inputRows().size());
  }

  // Tests for multi-line description parsing

  @Test
  void parse_shouldParseMultilineDescription() {
    // Arrange
    StepParser parser = new StepParser("This is the first line", 1);
    parser.parseLine("This is the second line", 2);
    parser.parseLine("This is the third line", 3);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepDescription);
    StepDescription description = (StepDescription) result;
    String expected = "This is the first line\nThis is the second line\nThis is the third line";
    assertEquals(expected, description.markdown());
  }

  @Test
  void parse_shouldParseMarkdownWithTables() {
    // Arrange
    StepParser parser = new StepParser("Some markdown content", 1);
    parser.parseLine("| col1 | col2 |", 2);
    parser.parseLine("|------|------|", 3);
    parser.parseLine("| val1 | val2 |", 4);
    parser.parseLine("", 5);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepDescription);
    StepDescription description = (StepDescription) result;
    assertTrue(description.markdown().contains("| col1 | col2 |"));
    assertTrue(description.markdown().contains("| val1 | val2 |"));
  }

  // Tests for line number handling

  @Test
  void parse_shouldPreserveLineNumber() {
    // Arrange
    StepParser parser = new StepParser("> **Plot** Step", 42);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals(42, stepTask.lineNumber());
  }

  @Test
  void parse_shouldPreserveLineNumberForDescription() {
    // Arrange
    StepParser parser = new StepParser("Description text", 99);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepDescription);
    StepDescription description = (StepDescription) result;
    assertEquals(99, description.lineNumber());
  }

  // Tests for edge cases

  @Test
  void parse_shouldHandleEmptyPlotName() {
    // Arrange
    StepParser parser = new StepParser("> **** Just the step", 1);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("", stepTask.call().plotName());
  }

  @Test
  void parse_shouldHandleSpecialCharactersInStepValue() {
    // Arrange
    StepParser parser = new StepParser("> **Plot** Step with @#$%^& special chars", 1);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("Step with @#$%^& special chars", stepTask.call().stepValue());
  }

  @Test
  void parse_shouldHandleWhitespaceBeforeAndAfterContent() {
    // Arrange
    StepParser parser = new StepParser("   Padded description   ", 1);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepDescription);
    StepDescription description = (StepDescription) result;
    assertEquals("Padded description", description.markdown());
  }

  @Test
  void parseLineShouldReturnFalseWhenParsingEnds() {
    // Arrange
    StepParser parser = new StepParser("> **Plot** Step", 1);

    // Act & Assert
    assertFalse(parser.parseLine("Next section begins", 2));
  }

  @Test
  void parseLineShouldReturnTrueForContinuationContent() {
    // Arrange
    StepParser parser = new StepParser("Description line 1", 1);

    // Act
    boolean result = parser.parseLine("Description line 2", 2);

    // Assert
    assertTrue(result);
  }

  // Tests for table header changes

  @Test
  void parse_shouldHandleMultipleTables() {
    // Arrange
    String block =
        """
        > **Operations** Process data
        >
        > | id | name |
        > |----|----- |
        > | 1 | Alice |
        > | 2 | Bob |
        >
        > | 3 | Charlie |
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    // Empty > lines don't end the step or reset headers, so all rows are collected
    assertEquals(3, stepTask.inputRows().size());
    assertEquals("Alice", stepTask.inputRows().get(0).get("name"));
    assertEquals("Bob", stepTask.inputRows().get(1).get("name"));
    assertEquals("Charlie", stepTask.inputRows().get(2).get("name"));
  }

  @Test
  void parse_shouldParseTableFromTextBlock() {
    // Arrange
    String block =
        """
        > **Greeting** Greet ${name}
        >
        > | name |
        > |------|
        > | John |
        > | Jane |
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals(2, stepTask.inputRows().size());
    assertEquals("John", stepTask.inputRows().get(0).get("name"));
    assertEquals("Jane", stepTask.inputRows().get(1).get("name"));
  }

  @Test
  void parse_shouldParseMultiColumnTableFromTextBlock() {
    // Arrange
    String block =
        """
        > **User Operations** Create users
        >
        > | name | email |
        > |------|-------|
        > | John | john@example.com |
        > | Jane | jane@example.com |
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 10);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals(2, stepTask.inputRows().size());
    assertEquals("John", stepTask.inputRows().get(0).get("name"));
    assertEquals("john@example.com", stepTask.inputRows().get(0).get("email"));
  }

  @Test
  void parse_shouldParseSimpleStepWithoutTable() {
    // Arrange - mirrors: > **Greeting** Say Hello
    String block =
        """
        > **Greeting** Say Hello
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("Greeting", stepTask.call().plotName());
    assertEquals("Say Hello", stepTask.call().stepValue());
    assertTrue(stepTask.inputRows().isEmpty());
  }

  @Test
  void parse_shouldParseStepWithVariable() {
    // Arrange - mirrors: > **Greeting** Greet John
    String block =
        """
        > **Greeting** Greet John
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("Greeting", stepTask.call().plotName());
    assertEquals("Greet John", stepTask.call().stepValue());
    assertTrue(stepTask.inputRows().isEmpty());
  }

  @Test
  void parse_shouldParseStepWithVariablePlaceholder() {
    // Arrange - mirrors: > **Greeting** Greet ${name}
    String block =
        """
        > **Greeting** Greet ${name}
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("Greeting", stepTask.call().plotName());
    assertEquals("Greet ${name}", stepTask.call().stepValue());
    assertTrue(stepTask.inputRows().isEmpty());
  }

  @Test
  void parse_shouldParseGreetingPlotWithMultipleNames() {
    // Arrange - mirrors the exact example from 00_the-first-step.md
    String block =
        """
        > **Greeting** Greet ${name}
        >
        > | name  |
        > |-------|
        > | John  |
        > | Jane  |
        > | Doe   |
        """;

    // Act
    Step result = parseStepFromTextBlock(block, 1);

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("Greeting", stepTask.call().plotName());
    assertEquals("Greet ${name}", stepTask.call().stepValue());
    assertEquals(3, stepTask.inputRows().size());
    assertEquals("John", stepTask.inputRows().get(0).get("name"));
    assertEquals("Jane", stepTask.inputRows().get(1).get("name"));
    assertEquals("Doe", stepTask.inputRows().get(2).get("name"));
  }

  private Step parseStepFromTextBlock(String block, int startLine) {
    String trimmed = block.strip();
    String[] lines = trimmed.split("\\R");
    StepParser parser = new StepParser(lines[0], startLine);
    for (int i = 1; i < lines.length; i++) {
      parser.parseLine(lines[i], startLine + i);
    }
    return parser.build();
  }

  @Test
  void parse_shouldParseProblemLineFromMarkdown() {
    // Arrange - The exact problematic line from 00_the-first-step.md line 23
    String line = "> **Greeting** Say Hello";
    StepParser parser = new StepParser(line, 23);

    // Act
    Step result = parser.build();

    // Assert
    assertTrue(result instanceof StepTask);
    StepTask stepTask = (StepTask) result;
    assertEquals("Greeting", stepTask.call().plotName(), "Plot name should be extracted correctly");
    assertEquals(
        "Say Hello", stepTask.call().stepValue(), "Step value should not contain the plot name");
  }
}
