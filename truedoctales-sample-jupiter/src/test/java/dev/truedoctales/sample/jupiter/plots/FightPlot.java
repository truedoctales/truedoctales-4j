package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
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

  @Step(
      value = "Attack fails",
      description = "Performs an attack that is expected to fail.",
      headers = {"attacker", "defender", "weapon", "result"},
      variableDescriptions = {
        "Attacking entity",
        "Defending entity",
        "Weapon used",
        "Expected result"
      })
  public void attackFails(String attacker, String defender, String weapon, String result) {
    AttackResult attackResult = fightService.attack(attacker, defender, weapon);
    Assertions.assertFalse(attackResult.success(), "Attack should fail");
    Assertions.assertEquals("FAILED", result, "Attack should fail");
  }

  @Step(
      value = "Defeat with skill",
      description = "The hero defeats the monster using a specific skill.",
      headers = {"hero", "monster", "skill", "outcome"},
      variableDescriptions = {
        "Name of the hero",
        "Name of the monster",
        "Skill used",
        "Expected outcome"
      })
  public void defeatWithSkill(String hero, String monster, String skill, String outcome) {
    AttackResult attackResult = fightService.defeatWithSkill(hero, monster, skill);
    Assertions.assertTrue(attackResult.success(), "Hero should defeat monster");
    Assertions.assertEquals("VICTORY", outcome, "Hero should achieve victory");
  }
}
