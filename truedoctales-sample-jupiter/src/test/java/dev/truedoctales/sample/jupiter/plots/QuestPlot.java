package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.domain.QuestService;
import dev.truedoctales.sample.domain.QuestService.Quest;
import org.junit.jupiter.api.Assertions;

/**
 * Quest operations for Greek mythology stories.
 *
 * <p>Demonstrates quest lifecycle testing using service-based approach with constructor injection.
 */
@Plot("Quest")
public class QuestPlot {

  private final QuestService questService;
  private final HeroService heroService;

  public QuestPlot(QuestService questService, HeroService heroService) {
    this.questService = questService;
    this.heroService = heroService;
  }

  @Step("Create quest")
  public void createQuest(Long id, String name, String description, String status) {
    questService.createQuest(id, name, description, status);
  }

  @Step("Assign to hero")
  public void assignToHero(String hero, String quest) {
    Assertions.assertTrue(heroService.exists(hero), "Hero '" + hero + "' must exist");
    Assertions.assertTrue(questService.exists(quest), "Quest '" + quest + "' must exist");
    boolean success = questService.assignToHero(hero, quest);
    Assertions.assertTrue(success, "Failed to assign quest to hero");
  }

  @Step("Status is")
  public void statusIs(String quest, String expectedStatus) {
    Quest q =
        questService
            .findByName(quest)
            .orElseThrow(() -> new AssertionError("Quest '" + quest + "' must exist"));
    Assertions.assertEquals(expectedStatus, q.status(), "Quest status should be " + expectedStatus);
  }

  @Step("Complete quest")
  public void completeQuest(String hero, String quest) {
    String heroQuest =
        questService
            .getHeroQuest(hero)
            .orElseThrow(() -> new AssertionError("Hero '" + hero + "' must have a quest"));
    Assertions.assertEquals(
        quest, heroQuest, "Hero '" + hero + "' must have quest '" + quest + "'");
    boolean success = questService.completeQuest(quest);
    Assertions.assertTrue(success, "Failed to complete quest");
  }
}
