package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.sample.domain.MonsterService;
import org.junit.jupiter.api.Assertions;

/**
 * Monster operations for Greek mythology stories.
 *
 * <p>Demonstrates testing monster entities using service-based approach with constructor injection.
 */
@Plot("Monster")
public class MonsterPlot {

  private final MonsterService monsterService;

  public MonsterPlot(MonsterService monsterService) {
    this.monsterService = monsterService;
  }

  @Step(
      value = "Create monster",
      description = "Creates a new monster with the given id, name, threat level and weakness.",
      headers = {"id", "name", "threat", "weakness"},
      variableDescriptions = {
        "Unique identifier",
        "Monster name",
        "Threat level",
        "Known weakness"
      })
  public void createMonster(Long id, String name, String threat, String weakness) {
    monsterService.createMonster(id, name, threat, weakness);
  }

  @Step(
      value = "Monster exists",
      description = "Asserts that a monster with the given name exists.",
      headers = {"name"},
      variableDescriptions = {"Monster name to look up"})
  public void monsterExists(String name) {
    Assertions.assertTrue(monsterService.exists(name), "Monster '" + name + "' should exist");
  }

  @Step(
      value = "Monster is alive",
      description = "Asserts that the monster is still alive.",
      headers = {"name"},
      variableDescriptions = {"Monster name to check"})
  public void monsterIsAlive(String name) {
    Assertions.assertTrue(monsterService.isAlive(name), "Monster '" + name + "' should be alive");
  }

  @Step(
      value = "Monster is dead",
      description = "Asserts that the monster has been defeated.",
      headers = {"name"},
      variableDescriptions = {"Monster name to check"})
  public void monsterIsDead(String name) {
    Assertions.assertFalse(monsterService.isAlive(name), "Monster '" + name + "' should be dead");
  }
}
