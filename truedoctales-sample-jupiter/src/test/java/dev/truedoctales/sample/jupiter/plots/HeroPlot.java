package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
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

  @Step(
      value = "Create hero",
      description = "Creates a new hero with the given id, name, species and age.")
  public void createHero(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "name", description = "Hero name") String name,
      @Variable(value = "species", description = "Species of the hero") String species,
      @Variable(value = "age", description = "Age in years") Integer age) {
    heroService.createHero(id, name, species, age);
  }

  @Step(value = "Hero exists", description = "Asserts that a hero with the given name exists.")
  public void heroExists(
      @Variable(value = "name", description = "Hero name to look up") String name) {
    Assertions.assertTrue(heroService.exists(name), "Hero '" + name + "' should exist");
  }

  @Step(value = "Grant skill", description = "Grants a skill to the hero identified by name.")
  public void grantSkill(
      @Variable(value = "heroName", description = "Name of the hero") String heroName,
      @Variable(value = "skill", description = "Skill to grant") String skill) {
    boolean success = heroService.grantSkill(heroName, skill);
    Assertions.assertTrue(success, "Hero '" + heroName + "' must exist");
  }

  @Step(value = "Has skill", description = "Asserts that the hero has the specified skill.")
  public void hasSkill(
      @Variable(value = "heroName", description = "Name of the hero") String heroName,
      @Variable(value = "skill", description = "Expected skill") String skill) {
    Assertions.assertTrue(
        heroService.hasSkill(heroName, skill),
        "Hero '" + heroName + "' should have skill '" + skill + "'");
  }

  @Step(value = "Trophy earned", description = "Awards a trophy to the hero.")
  public void trophyEarned(
      @Variable(value = "hero", description = "Name of the hero") String hero,
      @Variable(value = "trophy", description = "Trophy to award") String trophy) {
    boolean success = heroService.awardTrophy(hero, trophy);
    Assertions.assertTrue(success, "Hero '" + hero + "' must exist");
  }

  @Step(value = "Level is", description = "Asserts that the hero has reached the expected level.")
  public void levelIs(
      @Variable(value = "hero", description = "Name of the hero") String hero,
      @Variable(value = "expectedLevel", description = "Expected level number") int expectedLevel) {
    Hero heroEntity =
        heroService
            .findByName(hero)
            .orElseThrow(() -> new AssertionError("Hero '" + hero + "' must exist"));
    Assertions.assertEquals(
        expectedLevel, heroEntity.level(), "Hero '" + hero + "' should be level " + expectedLevel);
  }
}
