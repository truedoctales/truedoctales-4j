package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Var;
import dev.truedoctales.sample.domain.FightService;
import dev.truedoctales.sample.domain.FightService.AttackResult;
import org.junit.jupiter.api.Assertions;

/**
 * Fight operations for Greek mythology stories.
 *
 * <p>Demonstrates combat mechanics using service-based approach with constructor injection.
 */
@Plot("Fight")
public class FightPlot {

  private final FightService fightService;

  public FightPlot(FightService fightService) {
    this.fightService = fightService;
  }

  @Step(value = "Attack fails", description = "Performs an attack that is expected to fail.")
  public void attackFails(
      @Var(value = "attacker", description = "Attacking entity") String attacker,
      @Var(value = "defender", description = "Defending entity") String defender,
      @Var(value = "weapon", description = "Weapon used") String weapon,
      @Var(value = "result", description = "Expected result") String result) {
    AttackResult attackResult = fightService.attack(attacker, defender, weapon);
    Assertions.assertFalse(attackResult.success(), "Attack should fail");
    Assertions.assertEquals("FAILED", result, "Attack should fail");
  }

  @Step(
      value = "Defeat with skill",
      description = "The hero defeats the monster using a specific skill.")
  public void defeatWithSkill(
      @Var(value = "hero", description = "Name of the hero") String hero,
      @Var(value = "monster", description = "Name of the monster") String monster,
      @Var(value = "skill", description = "Skill used") String skill,
      @Var(value = "outcome", description = "Expected outcome") String outcome) {
    AttackResult attackResult = fightService.defeatWithSkill(hero, monster, skill);
    Assertions.assertTrue(attackResult.success(), "Hero should defeat monster");
    Assertions.assertEquals("VICTORY", outcome, "Hero should achieve victory");
  }
}
