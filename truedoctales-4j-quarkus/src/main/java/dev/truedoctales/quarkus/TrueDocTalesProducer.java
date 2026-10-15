package dev.truedoctales.quarkus;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.execution.execute.SimplePlotRegistry;
import dev.truedoctales.execution.jupiter.JupiterStoryTestExecutor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import java.util.Comparator;
import java.util.stream.StreamSupport;

/** Produces the True Doc Tales services from Quarkus-managed CDI beans. */
@ApplicationScoped
public class TrueDocTalesProducer {

  @Produces
  @ApplicationScoped
  public PlotRegistry trueDocTalesPlotRegistry(@Any Instance<Object> beans) {
    SimplePlotRegistry registry = new SimplePlotRegistry();
    StreamSupport.stream(beans.handles().spliterator(), false)
        .sorted(Comparator.comparing(handle -> handle.getBean().getBeanClass().getName()))
        .forEach(
            handle -> {
              try (handle) {
                if (handle.getBean().getBeanClass().isAnnotationPresent(Plot.class)) {
                  registry.register(handle.get(), handle.getBean().getBeanClass());
                }
              }
            });
    return registry;
  }

  @Produces
  @ApplicationScoped
  public StoryExecutionListener trueDocTalesStoryExecutionListener() {
    return new StoryExecutionListener.DelegateStoryExecutionListener();
  }

  @Produces
  @ApplicationScoped
  public JupiterStoryTestExecutor trueDocTalesJupiterStoryTestExecutor(
      PlotRegistry plotRegistry, StoryExecutionListener storyExecutionListener) {
    return new JupiterStoryTestExecutor(plotRegistry, storyExecutionListener);
  }
}
