package dev.truedoctales.sample.domain;

import java.util.*;

/**
 * Service for managing fights between heroes and monsters.
 *
 * <p>Demonstrates domain service pattern for combat mechanics.
 */
public class FightService {

  private final HeroService heroService;
  private final MonsterService monsterService;
  private final Map<String, List<String>> fightHistory = new HashMap<>();

  public FightService(HeroService heroService, MonsterService monsterService) {
    this.heroService = heroService;
    this.monsterService = monsterService;
  }

  /**
   * Result of an attack attempt.
   *
   * @param success whether the attack succeeded
   * @param message description of what happened
   */
  public record AttackResult(boolean success, String message) {}

  /**
   * Attempts an attack with a weapon.
   *
   * @param attackerName the attacker's name
   * @param defenderName the defender's name
   * @param weapon the weapon used
   * @return the attack result
   */
  public AttackResult attack(String attackerName, String defenderName, String weapon) {
    var monster = monsterService.findByName(defenderName);
    if (monster.isEmpty()) {
      return new AttackResult(false, "Monster not found");
    }

    // Record the attack
    recordFightAction(attackerName, "attacked " + defenderName + " with " + weapon);

    // All weapon attacks fail against monsters with impenetrable hides
    return new AttackResult(false, "Attack failed - weapon ineffective");
  }

  /**
   * Defeats a monster using a skill.
   *
   * @param heroName the hero's name
   * @param monsterName the monster's name
   * @param skill the skill used
   * @return the attack result
   */
  public AttackResult defeatWithSkill(String heroName, String monsterName, String skill) {
    var hero = heroService.findByName(heroName);
    var monster = monsterService.findByName(monsterName);

    if (hero.isEmpty()) {
      return new AttackResult(false, "Hero not found");
    }
    if (monster.isEmpty()) {
      return new AttackResult(false, "Monster not found");
    }

    if (!hero.get().skills().contains(skill)) {
      return new AttackResult(false, "Hero does not have skill: " + skill);
    }

    // Record the fight action
    recordFightAction(heroName, "defeated " + monsterName + " using " + skill);

    // Only level up if monster is still alive (first defeat)
    boolean wasAlive = monsterService.isAlive(monsterName);

    // Kill the monster
    monsterService.killMonster(monsterName);

    // Level up the hero only if this was the killing blow
    if (wasAlive) {
      int newLevel = hero.get().level() + 1;
      heroService.updateLevel(heroName, newLevel);
    }

    return new AttackResult(true, "Victory!");
  }

  /**
   * Records a fight action for history tracking.
   *
   * @param participant the participant's name
   * @param action the action taken
   */
  private void recordFightAction(String participant, String action) {
    fightHistory.computeIfAbsent(participant, k -> new ArrayList<>()).add(action);
  }

  /**
   * Gets the fight history for a participant.
   *
   * @param participant the participant's name
   * @return list of actions taken
   */
  public List<String> getFightHistory(String participant) {
    return fightHistory.getOrDefault(participant, Collections.emptyList());
  }

  /** Clears all fight history. */
  public void clear() {
    fightHistory.clear();
  }
}
