package dev.truedoctales.sample.jupiter;

import dev.truedoctales.api.annotations.StoryBook;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.story.StoryBookModel;
import dev.truedoctales.execution.execute.SimplePlotRegistry;
import dev.truedoctales.execution.jupiter.JupiterStoryTestExecutor;
import dev.truedoctales.execution.jupiter.StoryTestProvider;
import dev.truedoctales.report.json.JsonStoryListener;
import dev.truedoctales.sample.domain.FightService;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.domain.MonsterService;
import dev.truedoctales.sample.domain.ProjectService;
import dev.truedoctales.sample.domain.QuestService;
import dev.truedoctales.sample.domain.SpecificationService;
import dev.truedoctales.sample.domain.SprintService;
import dev.truedoctales.sample.jupiter.plots.*;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Story-based test execution for the True Doc Tales framework.
 *
 * <p>This class demonstrates best practices for: - Shared service management across plots -
 * Constructor dependency injection - Clean plot registration - Story execution with JSON reporting
 *
 * <p><strong>Pattern:</strong> Create shared service instances and inject them into plots to ensure
 * state consistency across test scenarios. Each story execution receives a fresh set of services —
 * stories must be self-contained and set up their own preconditions.
 */
@ClassTemplate
@ExtendWith({StoryTestProvider.class})
@StoryBook(
    path = "../fairy-doc-tales",
    listener = {JsonStoryListener.class})
public class StoryBookTest {
  private final StoryExecutionListener listener;
  private final StoryBookModel book;
  private final Path storyPath;

  public StoryBookTest(StoryExecutionListener listener, StoryBookModel book, Path storyPath) {
    this.listener = listener;
    this.book = book;
    this.storyPath = storyPath;
  }

  @TestFactory
  public Stream<DynamicNode> runStory() {
    // Each story gets its own fresh service instances — state does not leak between stories
    var heroService = new HeroService();
    var monsterService = new MonsterService();
    var questService = new QuestService();
    var fightService = new FightService(heroService, monsterService);
    var specificationService = new SpecificationService();
    var sprintService = new SprintService();
    var projectService = new ProjectService();

    SimplePlotRegistry plotRegistry = new SimplePlotRegistry();
    plotRegistry.register(new GreetingPlot());

    // Business-domain plots — used in the four FinTrack business chapters
    plotRegistry.register(new TeamMemberPlot(heroService));
    plotRegistry.register(new RiskPlot(monsterService));
    plotRegistry.register(new TicketPlot(questService, heroService));
    plotRegistry.register(new SpecificationPlot(specificationService));
    plotRegistry.register(new AttemptPlot(fightService));
    plotRegistry.register(new SprintPlot(sprintService));
    plotRegistry.register(new ProjectPlot(projectService, specificationService));
    plotRegistry.register(new AchievementPlot());

    // Framework-demo plot — used in 01_framework-basics

    return new JupiterStoryTestExecutor(plotRegistry, listener).buildDynamicTests(book, storyPath);
  }
}
