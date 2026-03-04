package dev.truedoctales.execution.execute;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.model.execution.InputType;
import dev.truedoctales.api.model.execution.PlotBinding;
import dev.truedoctales.api.model.execution.StepBinding;
import dev.truedoctales.api.model.execution.StepExecution;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Registry that manages plot instances and their binding methods.
///
/// Handles instantiation of Plot classes and provides method invocation for steps based on
/// @Plot and @Step annotations.
public class SimplePlotRegistry implements PlotRegistry {

  private final MethodInvoker methodInvoker = new MethodInvoker();

  record StepCall(Object instance, Method method) {}

  private static final Logger LOGGER = Logger.getLogger(SimplePlotRegistry.class.getName());
  private final Map<String, Map<String, StepCall>> stepRegistry;

  /// Creates a new simple plot registry.
  public SimplePlotRegistry() {
    stepRegistry = new HashMap<>();
  }

  @Override
  public Set<PlotBinding> getBindings() {
    return stepRegistry.entrySet().stream()
        .map(
            entry ->
                new PlotBinding(
                    entry.getKey(),
                    entry.getValue().entrySet().stream()
                        .map(
                            e ->
                                new StepBinding(
                                    entry.getKey(),
                                    e.getKey(),
                                    getInputType(e.getValue().method()),
                                    getStepDescription(e.getValue().method()),
                                    getStepHeaders(e.getValue().method())))
                        .toList()))
        .collect(Collectors.toSet());
  }

  public InputType getInputType(Method method) {
    Step annotation = method.getAnnotation(Step.class);
    if (annotation != null && annotation.type() != InputType.AUTO) {
      return annotation.type();
    }
    return Stream.of(method.getParameterTypes())
        .filter(Collection.class::isAssignableFrom)
        .findFirst()
        .map(x -> InputType.BATCH)
        .orElse(InputType.SEQUENCE);
  }

  private String getStepDescription(Method method) {
    Step annotation = method.getAnnotation(Step.class);
    return annotation != null ? annotation.description() : "";
  }

  private List<String> getStepHeaders(Method method) {
    Step annotation = method.getAnnotation(Step.class);
    return annotation != null && annotation.headers().length > 0
        ? List.of(annotation.headers())
        : List.of();
  }

  @Override
  public Object invoke(StepExecution stepExecution) throws Exception {
    Map<String, StepCall> plot = stepRegistry.get(stepExecution.binding().plot());
    if (plot == null) {
      throw new IllegalArgumentException(
          "No plot registered with name: " + stepExecution.binding().plot());
    }
    StepCall call = plot.get(stepExecution.binding().pattern());
    if (call == null) {
      throw new IllegalArgumentException(
          "No binding registered with name: "
              + stepExecution.binding().pattern()
              + " in plot: "
              + stepExecution.binding().plot());
    }
    try {
      return methodInvoker.invoke(
          call.instance(),
          call.method(),
          getInputType(call.method()),
          stepExecution.stepData(),
          stepExecution.variables());
    } catch (InvocationTargetException e) {
      Throwable cause = e.getTargetException();
      // Always throw as RuntimeException to ensure it can be caught and wrapped properly
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else if (cause instanceof Error) {
        throw (Error) cause;
      } else {
        throw new RuntimeException("Unexpected exception type", cause);
      }
    }
  }

  /// Registers a plot instance and its binding methods.
  ///
  /// @param plot the plot instance annotated with @Plot
  /// @return this registry for fluent chaining
  /// @throws IllegalArgumentException if the plot is not annotated with @Plot
  public SimplePlotRegistry register(Object plot) {
    String plotName =
        Optional.ofNullable(plot.getClass().getAnnotation(Plot.class))
            .map(Plot::value)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Plot instance must be annotated with @Plot: " + plot));

    Arrays.stream(plot.getClass().getMethods())
        .filter(m -> m.isAnnotationPresent(Step.class))
        .forEach(m -> registerStepMethod(plot, plotName, m));
    return this;
  }

  private void registerStepMethod(Object plot, String plotName, Method method) {
    String stepValue =
        Optional.ofNullable(method.getAnnotation(Step.class))
            .map(Step::value)
            .orElseThrow(
                () -> new IllegalArgumentException("Step method must be annotated with @Step"));
    StepCall call = new StepCall(plot, method);
    LOGGER.info(
        "Registering plot binding: "
            + plotName
            + " -> "
            + stepValue
            + " mapped to method: "
            + method);
    stepRegistry.computeIfAbsent(plotName, key -> new HashMap<>()).put(stepValue, call);
  }
}
