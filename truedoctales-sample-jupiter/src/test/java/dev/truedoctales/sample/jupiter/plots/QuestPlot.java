package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Var;
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
      description = "Creates a new quest with the given id, name, description and initial status.")
  public void createQuest(
      @Var(value = "id", description = "Unique identifier") Long id,
      @Var(value = "name", description = "Quest name") String name,
      @Var(value = "description", description = "Quest description") String description,
      @Var(value = "status", description = "Initial status") String status) {
    questService.createQuest(id, name, description, status);
  }

  @Step(value = "Assign to hero", description = "Assigns a quest to a hero. Both must exist.")
  public void assignToHero(
      @Var(value = "hero", description = "Name of the hero") String hero,
      @Var(value = "quest", description = "Name of the quest") String quest) {
    Assertions.assertTrue(heroService.exists(hero), "Hero '" + hero + "' must exist");
    Assertions.assertTrue(questService.exists(quest), "Quest '" + quest + "' must exist");
    boolean success = questService.assignToHero(hero, quest);
    Assertions.assertTrue(success, "Failed to assign quest to hero");
  }

  @Step(value = "Status is", description = "Asserts that the quest has the expected status.")
  public void statusIs(
      @Var(value = "quest", description = "Name of the quest") String quest,
      @Var(value = "expectedStatus", description = "Expected status value") String expectedStatus) {
    Quest q =
        questService
            .findByName(quest)
            .orElseThrow(() -> new AssertionError("Quest '" + quest + "' must exist"));
    Assertions.assertEquals(expectedStatus, q.status(), "Quest status should be " + expectedStatus);
  }

  @Step(value = "Complete quest", description = "Completes the quest assigned to the hero.")
  public void completeQuest(
      @Var(value = "hero", description = "Name of the hero") String hero,
      @Var(value = "quest", description = "Name of the quest") String quest) {
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
