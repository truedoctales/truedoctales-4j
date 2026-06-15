package dev.truedoctales.sample.spring;

import dev.truedoctales.api.annotations.StoryBook;
import dev.truedoctales.api.execute.PlotRegistry;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.story.StoryBookModel;
import dev.truedoctales.execution.jupiter.JupiterStoryTestExecutor;
import dev.truedoctales.execution.jupiter.StoryTestProvider;
import dev.truedoctales.report.json.JsonStoryListener;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.spring.plots.TeamMemberSpringPlot;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ClassTemplate
@SpringBootTest
@ExtendWith({SpringExtension.class, StoryTestProvider.class})
@StoryBook(
    path = "src/test/resources/spring-doc-tales",
    listener = {JsonStoryListener.class})
class SpringStoryBookTest {

  private final StoryExecutionListener listener;
  private final StoryBookModel book;
  private final Path storyPath;

  @Autowired private PlotRegistry plotRegistry;

  SpringStoryBookTest(StoryExecutionListener listener, StoryBookModel book, Path storyPath) {
    this.listener = listener;
    this.book = book;
    this.storyPath = storyPath;
  }

  @TestFactory
  Stream<DynamicNode> runStory() {
    return new JupiterStoryTestExecutor(plotRegistry, listener).buildDynamicTests(book, storyPath);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class PlotBeans {

    @Bean
    HeroService heroService() {
      return new HeroService();
    }

    @Bean
    TeamMemberSpringPlot teamMemberSpringPlot(HeroService heroService) {
      return new TeamMemberSpringPlot(heroService);
    }
  }
}
