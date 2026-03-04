package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Var;
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
      @Var(value = "id", description = "Unique identifier") Long id,
      @Var(value = "name", description = "Monster name") String name,
      @Var(value = "threat", description = "Threat level") String threat,
      @Var(value = "weakness", description = "Known weakness") String weakness) {
    monsterService.createMonster(id, name, threat, weakness);
  }

  @Step(
      value = "Monster exists",
      description = "Asserts that a monster with the given name exists.")
  public void monsterExists(
      @Var(value = "name", description = "Monster name to look up") String name) {
    Assertions.assertTrue(monsterService.exists(name), "Monster '" + name + "' should exist");
  }

  @Step(value = "Monster is alive", description = "Asserts that the monster is still alive.")
  public void monsterIsAlive(
      @Var(value = "name", description = "Monster name to check") String name) {
    Assertions.assertTrue(monsterService.isAlive(name), "Monster '" + name + "' should be alive");
  }

  @Step(value = "Monster is dead", description = "Asserts that the monster has been defeated.")
  public void monsterIsDead(
      @Var(value = "name", description = "Monster name to check") String name) {
    Assertions.assertFalse(monsterService.isAlive(name), "Monster '" + name + "' should be dead");
  }
}
