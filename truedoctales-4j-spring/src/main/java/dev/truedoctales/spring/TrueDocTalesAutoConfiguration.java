package dev.truedoctales.spring;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.execution.execute.SimplePlotRegistry;
import dev.truedoctales.execution.jupiter.JupiterStoryTestExecutor;
import java.util.Comparator;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/** Auto-configures True Doc Tales for Spring Boot tests and applications. */
@AutoConfiguration
public class TrueDocTalesAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public PlotRegistry trueDocTalesPlotRegistry(ApplicationContext applicationContext) {
    SimplePlotRegistry registry = new SimplePlotRegistry();
    applicationContext.getBeansWithAnnotation(Plot.class).entrySet().stream()
        .sorted(Comparator.comparing(entry -> entry.getKey()))
        .map(Map.Entry::getValue)
        .forEach(registry::register);
    return registry;
  }

  @Bean
  @ConditionalOnMissingBean
  public StoryExecutionListener trueDocTalesStoryExecutionListener() {
    return new StoryExecutionListener.DelegateStoryExecutionListener();
  }

  @Bean
  @ConditionalOnMissingBean
  public JupiterStoryTestExecutor trueDocTalesJupiterStoryTestExecutor(
      PlotRegistry plotRegistry, StoryExecutionListener storyExecutionListener) {
    return new JupiterStoryTestExecutor(plotRegistry, storyExecutionListener);
  }
}
