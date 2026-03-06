package dev.truedoctales.execution.mapping;

import static dev.truedoctales.api.model.execution.InputType.SEQUENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.plot.PlotBinding;
import dev.truedoctales.api.model.plot.StepBinding;
import dev.truedoctales.api.model.story.StepCall;
import dev.truedoctales.api.model.story.StepTask;
import dev.truedoctales.execution.execute.StoryBookExecutionMapperImpl;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoryBookExecutionMapperImplTest {

  private StoryBookExecutionMapperImpl mapper;

  @BeforeEach
  void setUp() {
    Set<PlotBinding> bindings =
        Set.of(
            new PlotBinding(
                "plot1",
                List.of(
                    new StepBinding("plot1", "Without variable", SEQUENCE),
                    new StepBinding("plot1", "With ${variable}", SEQUENCE))));
    mapper = new StoryBookExecutionMapperImpl(bindings);
  }

  @Test
  void mapStep_shouldMapStepWithoutVariable() {
    // Arrange
    StepTask stepModel = new StepTask(10, new StepCall("plot1", "Without variable"), List.of());
    StepExecution expected =
        new StepExecution(
            new StepBinding("plot1", "Without variable", SEQUENCE),
            new StepCall("plot1", "Without variable"),
            List.of(),
            10);

    // Act
    StepExecution result = mapper.mapStep(stepModel);

    // Assert
    assertEquals(expected, result);
  }

  @Test
  void mapStep_shouldMapStepWithVariable() {
    // Arrange
    StepCall call = new StepCall("plot1", "With TestValue");
    StepTask stepModel = new StepTask(10, call);
    StepExecution expected =
        new StepExecution(
            new StepBinding("plot1", "With ${variable}", SEQUENCE),
            call,
            List.of(),
            10,
            Map.of("variable", "TestValue"));

    // Act
    StepExecution result = mapper.mapStep(stepModel);

    // Assert
    assertEquals(expected, result);
  }
}
