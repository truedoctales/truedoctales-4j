package dev.truedoctales.spring;

import static org.assertj.core.api.Assertions.assertThat;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.execution.execute.SimplePlotRegistry;
import dev.truedoctales.execution.jupiter.JupiterStoryTestExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class TrueDocTalesAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TrueDocTalesAutoConfiguration.class));

  @Test
  void registersPlotBeansInPlotRegistry() {
    contextRunner
        .withUserConfiguration(PlotConfiguration.class)
        .run(
            context -> {
              PlotRegistry registry = context.getBean(PlotRegistry.class);

              assertThat(registry.getBindings())
                  .singleElement()
                  .satisfies(
                      plot -> {
                        assertThat(plot.plotId()).isEqualTo("Greeting");
                        assertThat(plot.steps())
                            .singleElement()
                            .satisfies(step -> assertThat(step.pattern()).isEqualTo("Say hello"));
                      });
            });
  }

  @Test
  void providesJupiterExecutorAndDefaultListener() {
    contextRunner
        .withUserConfiguration(PlotConfiguration.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(JupiterStoryTestExecutor.class);
              assertThat(context).hasSingleBean(StoryExecutionListener.class);
            });
  }

  @Test
  void backsOffWhenPlotRegistryBeanExists() {
    contextRunner
        .withUserConfiguration(CustomRegistryConfiguration.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(PlotRegistry.class);
              assertThat(context.getBean(PlotRegistry.class).getBindings()).isEmpty();
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class PlotConfiguration {

    @Bean
    GreetingPlot greetingPlot() {
      return new GreetingPlot();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomRegistryConfiguration {

    @Bean
    PlotRegistry plotRegistry() {
      return new SimplePlotRegistry();
    }
  }

  @Plot("Greeting")
  static class GreetingPlot {

    @Step("Say hello")
    public void sayHello() {}
  }
}
