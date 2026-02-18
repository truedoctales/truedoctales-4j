package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.domain.HeroService.Hero;
import org.junit.jupiter.api.Assertions;

/**
 * Hero operations for Greek mythology stories.
 *
 * <p>Demonstrates testing hero entities with skills, levels, and achievements using service-based
 * approach with constructor injection.
 */
@Plot("Hero")
public class HeroPlot {

  private final HeroService heroService;

  public HeroPlot(HeroService heroService) {
    this.heroService = heroService;
  }

  @Step("Create hero")
  public void createHero(Long id, String name, String species, Integer age) {
    heroService.createHero(id, name, species, age);
  }

  @Step("Hero exists")
  public void heroExists(String name) {
    Assertions.assertTrue(heroService.exists(name), "Hero '" + name + "' should exist");
  }

  @Step("Grant skill")
  public void grantSkill(String heroName, String skill) {
    boolean success = heroService.grantSkill(heroName, skill);
    Assertions.assertTrue(success, "Hero '" + heroName + "' must exist");
  }

  @Step("Has skill")
  public void hasSkill(String heroName, String skill) {
    Assertions.assertTrue(
        heroService.hasSkill(heroName, skill),
        "Hero '" + heroName + "' should have skill '" + skill + "'");
  }

  @Step("Trophy earned")
  public void trophyEarned(String hero, String trophy) {
    boolean success = heroService.awardTrophy(hero, trophy);
    Assertions.assertTrue(success, "Hero '" + hero + "' must exist");
  }

  @Step("Level is")
  public void levelIs(String hero, int expectedLevel) {
    Hero heroEntity =
        heroService
            .findByName(hero)
            .orElseThrow(() -> new AssertionError("Hero '" + hero + "' must exist"));
    Assertions.assertEquals(
        expectedLevel, heroEntity.level(), "Hero '" + hero + "' should be level " + expectedLevel);
  }
}
