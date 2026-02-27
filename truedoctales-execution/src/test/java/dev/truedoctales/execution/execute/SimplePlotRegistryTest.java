package dev.truedoctales.execution.execute;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.model.execution.InputType;
import dev.truedoctales.api.model.execution.PlotBinding;
import dev.truedoctales.api.model.execution.StepBinding;
import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.story.StepCall;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimplePlotRegistryTest {

  private SimplePlotRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new SimplePlotRegistry();
  }

  @Test
  void register_shouldAcceptPlotWithAnnotation() {
    // Arrange
    TestPlot plot = new TestPlot();

    // Act & Assert
    assertDoesNotThrow(() -> registry.register(plot));
  }

  @Test
  void register_shouldThrowWhenMissingPlotAnnotation() {
    // Arrange
    Object notAPlot = new Object();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> registry.register(notAPlot));
  }

  @Test
  void getBindings_shouldReturnRegisteredSteps() {
    // Arrange
    registry.register(new TestPlot());

    // Act
    Set<PlotBinding> bindings = registry.getBindings();

    // Assert
    assertEquals(1, bindings.size());
    PlotBinding binding = bindings.iterator().next();
    assertEquals("TestPlot", binding.plotId());
    assertEquals(2, binding.steps().size());
  }

  @Test
  void invoke_shouldExecuteMatchingStep() throws Exception {
    // Arrange
    TestPlot plot = new TestPlot();
    registry.register(plot);

    StepBinding step = new StepBinding("TestPlot", "Simple binding", InputType.SEQUENCE);
    var call = new StepCall("TestPlot", "Simple binding");
    StepExecution execution = new StepExecution(step, call, List.of(), 0);

    // Act
    registry.invoke(execution);

    // Assert
    assertTrue(plot.simpleCalled);
  }

  @Test
  void invoke_shouldPassParameters() throws Exception {
    // Arrange
    TestPlot plot = new TestPlot();
    registry.register(plot);

    StepBinding binding = new StepBinding("TestPlot", "Step with ${param}", InputType.SEQUENCE);
    var call = new StepCall("TestPlot", "Step with test-value");
    List<Map<String, String>> data = List.of(Map.of("param", "test-value"));
    StepExecution execution = new StepExecution(binding, call, data, 0);

    // Act
    registry.invoke(execution);

    // Assert
    assertEquals("test-value", plot.lastParam);
  }

  @Test
  void invoke_shouldThrowWhenPlotNotFound() {
    // Arrange
    StepBinding step = new StepBinding("NonExistentPlot", "Some binding", InputType.SEQUENCE);
    var call = new StepCall("NonExistentPlot", "Some binding");
    StepExecution execution = new StepExecution(step, call, List.of(), 0);

    // Act & Assert
    Exception exception =
        assertThrows(IllegalArgumentException.class, () -> registry.invoke(execution));
    assertTrue(exception.getMessage().contains("No plot registered"));
  }

  @Test
  void invoke_shouldThrowWhenStepNotFound() {
    // Arrange
    registry.register(new TestPlot());
    StepBinding step = new StepBinding("TestPlot", "Non-existent binding", InputType.SEQUENCE);
    var call = new StepCall("TestPlot", "Non-existent binding");
    StepExecution execution = new StepExecution(step, call, List.of(), 0);

    // Act & Assert
    Exception exception =
        assertThrows(IllegalArgumentException.class, () -> registry.invoke(execution));
    assertTrue(exception.getMessage().contains("No binding registered"));
  }

  @Test
  void invoke_shouldHandleMultiplePlots() throws Exception {
    // Arrange
    TestPlot plot1 = new TestPlot();
    AnotherTestPlot plot2 = new AnotherTestPlot();
    registry.register(plot1).register(plot2);

    // Act
    StepBinding step1 = new StepBinding("TestPlot", "Simple binding", InputType.SEQUENCE);
    var call1 = new StepCall("TestPlot", "Simple binding");
    registry.invoke(new StepExecution(step1, call1, List.of(), 0));

    StepBinding step2 = new StepBinding("AnotherPlot", "Another binding", InputType.SEQUENCE);
    var call2 = new StepCall("AnotherPlot", "Another binding");
    registry.invoke(new StepExecution(step2, call2, List.of(), 0));

    // Assert
    assertTrue(plot1.simpleCalled);
    assertTrue(plot2.anotherCalled);
  }

  @Plot("TestPlot")
  public static class TestPlot {
    boolean simpleCalled = false;
    String lastParam;

    @Step("Simple binding")
    public void simpleStep() {
      simpleCalled = true;
    }

    @Step("Step with ${param}")
    public void stepWithParam(String param) {
      lastParam = param;
    }
  }

  @Plot("AnotherPlot")
  public static class AnotherTestPlot {
    boolean anotherCalled = false;

    @Step("Another binding")
    public void anotherStep() {
      anotherCalled = true;
    }
  }
}
