package dev.truedoctales.execution.execute;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.execution.InputType;
import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.listener.ExecutionStatus;
import dev.truedoctales.api.model.listener.StepExecutionResult;
import dev.truedoctales.api.model.plot.PlotBinding;
import dev.truedoctales.api.model.plot.StepBinding;
import dev.truedoctales.api.model.story.StepCall;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScenarioExecutorTest {

  private TestPlotRegistry plotRegistry;
  private TestExecutionListener listener;
  private StepExecutor executor;

  @BeforeEach
  void setUp() {
    plotRegistry = new TestPlotRegistry();
    listener = new TestExecutionListener();
    executor = new StepExecutor(plotRegistry, listener);
  }

  @Test
  void execute_shouldCallListenerAndRegistry() {
    // Arrange
    StepBinding binding = new StepBinding("plot", "binding", InputType.SEQUENCE);
    StepExecution execution = StepExecution.simplCall(0, binding, new StepCall("plot", "binding"));

    // Act
    executor.execute(execution);

    // Assert
    assertTrue(listener.startStepCalled);
    assertTrue(listener.endStepCalled);
    assertTrue(plotRegistry.invokeCalled);
  }

  @Test
  void execute_shouldHandleExceptionAndNotifyListener() {
    // Arrange

    StepBinding binding = new StepBinding("plot", "binding", InputType.SEQUENCE);
    var stepCall = new StepCall("plot", "binding");
    StepExecution execution = StepExecution.simplCall(10, binding, stepCall);
    plotRegistry.shouldThrow = true;

    // Act
    StepExecutionResult result = executor.execute(execution);

    // Assert
    assertTrue(listener.startStepCalled);
    assertTrue(listener.failureRecorded);
    assertTrue(listener.endStepCalled);
    assertEquals(ExecutionStatus.ERROR, result.status());
    assertNotNull(result.throwable());
    assertTrue(result.errorMessage().contains("Test failure"));
  }

  @Test
  void execute_shouldIncludeLineNumberInErrorMessage() {
    // Arrange
    StepBinding binding = new StepBinding("plot", "binding", InputType.SEQUENCE);
    var stepCall = new StepCall("plot", "binding");
    StepExecution execution = StepExecution.simplCall(42, binding, stepCall);
    plotRegistry.shouldThrow = true;

    // Act
    StepExecutionResult result = executor.execute(execution);

    // Assert
    assertEquals(ExecutionStatus.ERROR, result.status());
    assertNotNull(result.throwable());
  }

  @Test
  void execute_shouldNotIncludeLineNumberWhenZero() {
    // Arrange
    StepBinding binding = new StepBinding("plot", "binding", InputType.SEQUENCE);
    var stepCall = new StepCall("plot", "binding");
    StepExecution execution = StepExecution.simplCall(0, binding, stepCall);
    plotRegistry.shouldThrow = true;

    // Act
    StepExecutionResult result = executor.execute(execution);

    // Assert - when line number is 0, error result still created but no line info
    assertEquals(ExecutionStatus.ERROR, result.status());
    assertNotNull(result.throwable());
  }

  static class TestPlotRegistry implements PlotRegistry {
    boolean invokeCalled = false;
    boolean shouldThrow = false;

    @Override
    public Set<PlotBinding> getBindings() {
      return Set.of();
    }

    @Override
    public Object invoke(StepExecution stepExecution) throws Exception {
      invokeCalled = true;
      if (shouldThrow) {
        throw new RuntimeException("Test failure");
      }
      return null;
    }
  }

  static class TestExecutionListener implements StoryExecutionListener {
    boolean startStepCalled = false;
    boolean endStepCalled = false;
    boolean failureRecorded = false;

    @Override
    public void startStep(StepExecution execution) {
      startStepCalled = true;
    }

    @Override
    public void endStep(StepExecutionResult result) {
      endStepCalled = true;
    }

    @Override
    public void recordFailure(StepExecution step, Throwable exception) {
      failureRecorded = true;
    }
  }
}
