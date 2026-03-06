package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
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
      description = "Creates a new monster with the given id, name, threat level and weakness.")
  public void createMonster(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "name", description = "Monster name") String name,
      @Variable(value = "threat", description = "Threat level") String threat,
      @Variable(value = "weakness", description = "Known weakness") String weakness) {
    monsterService.createMonster(id, name, threat, weakness);
  }

  @Step(
      value = "Monster exists",
      description = "Asserts that a monster with the given name exists.")
  public void monsterExists(
      @Variable(value = "name", description = "Monster name to look up") String name) {
    Assertions.assertTrue(monsterService.exists(name), "Monster '" + name + "' should exist");
  }

  @Step(value = "Monster is alive", description = "Asserts that the monster is still alive.")
  public void monsterIsAlive(
      @Variable(value = "name", description = "Monster name to check") String name) {
    Assertions.assertTrue(monsterService.isAlive(name), "Monster '" + name + "' should be alive");
  }

  @Step(value = "Monster is dead", description = "Asserts that the monster has been defeated.")
  public void monsterIsDead(
      @Variable(value = "name", description = "Monster name to check") String name) {
    Assertions.assertFalse(monsterService.isAlive(name), "Monster '" + name + "' should be dead");
  }
}
