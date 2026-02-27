package dev.truedoctales.execution.execute;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.model.execution.InputType;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MethodInvokerTest {

  private MethodInvoker invoker;
  private TestTarget target;

  @BeforeEach
  void setUp() {
    invoker = new MethodInvoker();
    target = new TestTarget();
  }

  @Test
  void invokeWithoutParameters_shouldCallMethod() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("noParams");

    // Act
    invoker.invokeWithoutParameters(target, method);

    // Assert
    assertTrue(target.noParamsCalled);
  }

  @Test
  void invokeWithVariables_shouldPassStringParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withStringParam", String.class);
    Map<String, String> variables = Map.of("name", "John");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals("John", target.lastStringValue);
  }

  @Test
  void invokeWithVariables_shouldConvertIntParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withIntParam", int.class);
    Map<String, String> variables = Map.of("age", "30");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals(30, target.lastIntValue);
  }

  @Test
  void invokeWithVariables_shouldConvertLongParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withLongParam", long.class);
    Map<String, String> variables = Map.of("id", "123456789");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals(123456789L, target.lastLongValue);
  }

  @Test
  void invokeWithVariables_shouldConvertFloatParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withFloatParam", float.class);
    Map<String, String> variables = Map.of("price", "19.99");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals(19.99f, target.lastFloatValue, 0.01);
  }

  @Test
  void invokeWithVariables_shouldConvertDoubleParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withDoubleParam", double.class);
    Map<String, String> variables = Map.of("value", "99.99");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals(99.99, target.lastDoubleValue, 0.01);
  }

  @Test
  void invokeWithVariables_shouldConvertBooleanParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withBooleanParam", boolean.class);
    Map<String, String> variables = Map.of("flag", "true");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertTrue(target.lastBooleanValue);
  }

  @Test
  void invokeWithVariables_shouldConvertLocalDateParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withDateParam", LocalDate.class);
    Map<String, String> variables = Map.of("date", "2023-12-13");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals(LocalDate.parse("2023-12-13"), target.lastDateValue);
  }

  @Test
  void invokeWithVariables_shouldConvertEnumParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withEnumParam", TestEnum.class);
    Map<String, String> variables = Map.of("status", "ACTIVE");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals(TestEnum.ACTIVE, target.lastEnumValue);
  }

  @Test
  void invokeWithVariables_shouldHandleNullForObjectType() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withStringParam", String.class);
    Map<String, String> variables = new HashMap<>();
    variables.put("name", null);

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertNull(target.lastStringValue);
  }

  @Test
  void invokeWithDataRow_shouldPassTableRowParameters() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withStringParam", String.class);
    Map<String, String> row = Map.of("name", "Jane");

    // Act
    invoker.invokeWithDataRow(target, method, row);

    // Assert
    assertEquals("Jane", target.lastStringValue);
  }

  @Test
  void invokeWithDataRow_shouldHandleCaseInsensitiveParameters() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withStringParam", String.class);
    Map<String, String> row = Map.of("NAME", "Bob");

    // Act
    invoker.invokeWithDataRow(target, method, row);

    // Assert
    assertEquals("Bob", target.lastStringValue);
  }

  @Test
  void invokeWithDataCollection_shouldPassEntireCollection() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withListParam", List.class);
    List<Map<String, String>> rows =
        List.of(Map.of("name", "Alice"), Map.of("name", "Bob"), Map.of("name", "Charlie"));

    // Act
    invoker.invokeWithDataCollection(target, method, rows, Map.of());

    // Assert
    assertNotNull(target.lastListValue);
    assertEquals(3, target.lastListValue.size());
    assertEquals("Alice", target.lastListValue.get(0).get("name"));
    assertEquals("Bob", target.lastListValue.get(1).get("name"));
    assertEquals("Charlie", target.lastListValue.get(2).get("name"));
  }

  @Test
  void invoke_shouldHandleNoParametersAndNoData() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("noParams");
    List<Map<String, String>> emptyData = List.of();

    // Act
    invoker.invoke(target, method, InputType.SEQUENCE, emptyData, Map.of());

    // Assert
    assertTrue(target.noParamsCalled);
  }

  @Test
  void invoke_shouldHandleParametersWithData() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withStringParam", String.class);
    List<Map<String, String>> data = List.of(Map.of("name", "Test"));

    // Act
    invoker.invoke(target, method, InputType.SEQUENCE, data, Map.of());

    // Assert
    assertEquals("Test", target.lastStringValue);
  }

  @Test
  void invoke_shouldHandleCollectionParameter() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withListParam", List.class);
    List<Map<String, String>> data = List.of(Map.of("name", "Test"));

    // Act
    invoker.invoke(target, method, InputType.BATCH, data, Map.of());

    // Assert
    assertNotNull(target.lastListValue);
    assertEquals(1, target.lastListValue.size());
  }

  @Test
  void invokeWithBoxedInteger_shouldConvertCorrectly() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withBoxedIntParam", Integer.class);
    Map<String, String> variables = Map.of("count", "42");

    // Act
    invoker.invokeWithVariables(target, method, variables);

    // Assert
    assertEquals(42, target.lastBoxedIntValue);
  }

  @Test
  void invoke_withVariablesAndTableData_shouldMergeAndPassToMethod() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withTwoParams", String.class, String.class);
    List<Map<String, String>> tableData = List.of(Map.of("name", "Alice"));
    Map<String, String> variables = Map.of("skill", "Math");

    // Act
    invoker.invoke(target, method, InputType.BATCH, tableData, variables);

    // Assert
    assertEquals("Alice", target.lastStringValue);
    assertEquals("Math", target.lastSecondStringValue);
  }

  @Test
  void invoke_withVariablesAndNoTableData_shouldInvokeWithVariablesOnly() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withStringParam", String.class);
    List<Map<String, String>> tableData = List.of();
    Map<String, String> variables = Map.of("name", "Bob");

    // Act
    invoker.invoke(target, method, InputType.SEQUENCE, tableData, variables);

    // Assert
    assertEquals("Bob", target.lastStringValue);
  }

  @Test
  void invoke_withVariablesAndTableDataForCollection_shouldPassBothToMethod() throws Exception {
    // Arrange
    Method method = TestTarget.class.getMethod("withSkillAndList", String.class, List.class);
    List<Map<String, String>> tableData = List.of(Map.of("name", "Alice"), Map.of("name", "Bob"));
    Map<String, String> variables = Map.of("skill", "Programming");

    // Act
    invoker.invoke(target, method, InputType.SEQUENCE, tableData, variables);

    // Assert
    assertEquals("Programming", target.lastStringValue);
    assertNotNull(target.lastListValue);
    assertEquals(2, target.lastListValue.size());
    assertEquals("Alice", target.lastListValue.get(0).get("name"));
    assertEquals("Bob", target.lastListValue.get(1).get("name"));
  }

  public static class TestTarget {
    boolean noParamsCalled = false;
    String lastStringValue;
    String lastSecondStringValue;
    int lastIntValue;
    long lastLongValue;
    float lastFloatValue;
    double lastDoubleValue;
    boolean lastBooleanValue;
    LocalDate lastDateValue;
    TestEnum lastEnumValue;
    Integer lastBoxedIntValue;
    List<Map<String, String>> lastListValue;

    public void noParams() {
      noParamsCalled = true;
    }

    public void withStringParam(String name) {
      lastStringValue = name;
    }

    public void withTwoParams(String name, String skill) {
      lastStringValue = name;
      lastSecondStringValue = skill;
    }

    public void withIntParam(int age) {
      lastIntValue = age;
    }

    public void withLongParam(long id) {
      lastLongValue = id;
    }

    public void withFloatParam(float price) {
      lastFloatValue = price;
    }

    public void withDoubleParam(double value) {
      lastDoubleValue = value;
    }

    public void withBooleanParam(boolean flag) {
      lastBooleanValue = flag;
    }

    public void withDateParam(LocalDate date) {
      lastDateValue = date;
    }

    public void withEnumParam(TestEnum status) {
      lastEnumValue = status;
    }

    public void withBoxedIntParam(Integer count) {
      lastBoxedIntValue = count;
    }

    public void withListParam(List<Map<String, String>> data) {
      lastListValue = data;
    }

    public void withSkillAndList(String skill, List<Map<String, String>> data) {
      lastStringValue = skill;
      lastListValue = data;
    }
  }

  public enum TestEnum {
    ACTIVE,
    INACTIVE
  }
}
