package dev.truedoctales.sample.domain;

import java.util.*;

/**
 * Service for managing heroes in Greek mythology stories.
 *
 * <p>Demonstrates domain service pattern with in-memory storage.
 */
public class HeroService {

  private final Map<Long, Hero> heroes = new HashMap<>();
  private final Map<String, Hero> heroByName = new HashMap<>();

  /** Hero record representing a team member with skills, weaknesses, level, and trophies. */
  public record Hero(
      Long id,
      String name,
      String species,
      int age,
      Set<String> skills,
      Set<String> weaknesses,
      int level,
      Set<String> trophies) {

    public Hero withLevel(int newLevel) {
      return new Hero(id, name, species, age, skills, weaknesses, newLevel, trophies);
    }

    public Hero withSkill(String skill) {
      var newSkills = new HashSet<>(skills);
      newSkills.add(skill);
      return new Hero(id, name, species, age, newSkills, weaknesses, level, trophies);
    }

    public Hero withWeakness(String weakness) {
      var newWeaknesses = new HashSet<>(weaknesses);
      newWeaknesses.add(weakness);
      return new Hero(id, name, species, age, skills, newWeaknesses, level, trophies);
    }

    public Hero withTrophy(String trophy) {
      var newTrophies = new HashSet<>(trophies);
      newTrophies.add(trophy);
      return new Hero(id, name, species, age, skills, weaknesses, level, newTrophies);
    }
  }

  /**
   * Creates a new hero with default level 1 and no skills, weaknesses, or trophies.
   *
   * @param id the unique hero identifier
   * @param name the hero's name
   * @param species the hero's species or role
   * @param age the hero's age
   * @return the created hero
   */
  public Hero createHero(Long id, String name, String species, int age) {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(name, "name cannot be null");

    var hero =
        new Hero(id, name, species, age, new HashSet<>(), new HashSet<>(), 1, new HashSet<>());
    heroes.put(id, hero);
    heroByName.put(name, hero);
    return hero;
  }

  /**
   * Finds a hero by name.
   *
   * @param name the hero's name
   * @return Optional containing the hero if found
   */
  public Optional<Hero> findByName(String name) {
    return Optional.ofNullable(heroByName.get(name));
  }

  /**
   * Finds a hero by ID.
   *
   * @param id the hero's ID
   * @return Optional containing the hero if found
   */
  public Optional<Hero> findById(Long id) {
    return Optional.ofNullable(heroes.get(id));
  }

  /**
   * Grants a skill to a hero.
   *
   * @param heroName the hero's name
   * @param skill the skill to grant
   * @return true if successful, false if hero not found
   */
  public boolean grantSkill(String heroName, String skill) {
    Hero hero = heroByName.get(heroName);
    if (hero == null) {
      return false;
    }

    var updated = hero.withSkill(skill);
    heroes.put(updated.id(), updated);
    heroByName.put(updated.name(), updated);
    return true;
  }

  /**
   * Checks if a hero has a specific skill.
   *
   * @param heroName the hero's name
   * @param skill the skill to check
   * @return true if the hero has the skill
   */
  public boolean hasSkill(String heroName, String skill) {
    Hero hero = heroByName.get(heroName);
    return hero != null && hero.skills().contains(skill);
  }

  /**
   * Grants a weakness to a team member.
   *
   * @param heroName the team member's name
   * @param weakness the weakness to record
   * @return true if successful, false if not found
   */
  public boolean grantWeakness(String heroName, String weakness) {
    Hero hero = heroByName.get(heroName);
    if (hero == null) return false;
    var updated = hero.withWeakness(weakness);
    heroes.put(updated.id(), updated);
    heroByName.put(updated.name(), updated);
    return true;
  }

  /**
   * Checks if a team member has a specific weakness.
   *
   * @param heroName the team member's name
   * @param weakness the weakness to check
   * @return true if the team member has this weakness
   */
  public boolean hasWeakness(String heroName, String weakness) {
    Hero hero = heroByName.get(heroName);
    return hero != null && hero.weaknesses().contains(weakness);
  }

  /**
   * Awards a trophy to a hero.
   *
   * @param heroName the hero's name
   * @param trophy the trophy to award
   * @return true if successful, false if hero not found
   */
  public boolean awardTrophy(String heroName, String trophy) {
    Hero hero = heroByName.get(heroName);
    if (hero == null) {
      return false;
    }

    var updated = hero.withTrophy(trophy);
    heroes.put(updated.id(), updated);
    heroByName.put(updated.name(), updated);
    return true;
  }

  /**
   * Updates a hero's level.
   *
   * @param heroName the hero's name
   * @param newLevel the new level
   * @return true if successful, false if hero not found
   */
  public boolean updateLevel(String heroName, int newLevel) {
    Hero hero = heroByName.get(heroName);
    if (hero == null) {
      return false;
    }

    var updated = hero.withLevel(newLevel);
    heroes.put(updated.id(), updated);
    heroByName.put(updated.name(), updated);
    return true;
  }

  /** Clears all heroes. */
  public void clear() {
    heroes.clear();
    heroByName.clear();
  }

  /**
   * Returns the total number of heroes.
   *
   * @return hero count
   */
  public int count() {
    return heroes.size();
  }

  /**
   * Checks if a hero exists by name.
   *
   * @param name the hero's name
   * @return true if hero exists
   */
  public boolean exists(String name) {
    return heroByName.containsKey(name);
  }
}
