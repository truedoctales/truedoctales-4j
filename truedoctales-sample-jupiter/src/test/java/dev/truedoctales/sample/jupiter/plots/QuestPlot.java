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

  @Step(
      value = "Create quest",
      description = "Creates a new quest with the given id, name, description and initial status.",
      headers = {"id", "name", "description", "status"})
  public void createQuest(Long id, String name, String description, String status) {
    questService.createQuest(id, name, description, status);
  }

  @Step(
      value = "Assign to hero",
      description = "Assigns a quest to a hero. Both must exist.",
      headers = {"hero", "quest"})
  public void assignToHero(String hero, String quest) {
    Assertions.assertTrue(heroService.exists(hero), "Hero '" + hero + "' must exist");
    Assertions.assertTrue(questService.exists(quest), "Quest '" + quest + "' must exist");
    boolean success = questService.assignToHero(hero, quest);
    Assertions.assertTrue(success, "Failed to assign quest to hero");
  }

  @Step(
      value = "Status is",
      description = "Asserts that the quest has the expected status.",
      headers = {"quest", "expectedStatus"})
  public void statusIs(String quest, String expectedStatus) {
    Quest q =
        questService
            .findByName(quest)
            .orElseThrow(() -> new AssertionError("Quest '" + quest + "' must exist"));
    Assertions.assertEquals(expectedStatus, q.status(), "Quest status should be " + expectedStatus);
  }

  @Step(
      value = "Complete quest",
      description = "Completes the quest assigned to the hero.",
      headers = {"hero", "quest"})
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
