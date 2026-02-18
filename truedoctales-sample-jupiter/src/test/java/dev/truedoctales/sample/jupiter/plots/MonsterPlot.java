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

  @Step("Create monster")
  public void createMonster(Long id, String name, String threat, String weakness) {
    monsterService.createMonster(id, name, threat, weakness);
  }

  @Step("Monster exists")
  public void monsterExists(String name) {
    Assertions.assertTrue(monsterService.exists(name), "Monster '" + name + "' should exist");
  }

  @Step("Monster is alive")
  public void monsterIsAlive(String name) {
    Assertions.assertTrue(monsterService.isAlive(name), "Monster '" + name + "' should be alive");
  }

  @Step("Monster is dead")
  public void monsterIsDead(String name) {
    Assertions.assertFalse(monsterService.isAlive(name), "Monster '" + name + "' should be dead");
  }
}
