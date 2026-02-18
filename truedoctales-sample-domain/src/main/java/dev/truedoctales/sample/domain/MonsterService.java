package dev.truedoctales.sample.domain;

import java.util.*;

/**
 * Service for managing monsters in Greek mythology stories.
 *
 * <p>Demonstrates domain service pattern with in-memory storage.
 */
public class MonsterService {

  private final Map<Long, Monster> monsters = new HashMap<>();
  private final Map<String, Monster> monsterByName = new HashMap<>();

  /** Monster record representing a mythological monster with threat level and weakness. */
  public record Monster(Long id, String name, String threat, String weakness, boolean alive) {

    public Monster withAlive(boolean alive) {
      return new Monster(id, name, threat, weakness, alive);
    }
  }

  /**
   * Creates a new monster.
   *
   * @param id the unique monster identifier
   * @param name the monster's name
   * @param threat the threat level
   * @param weakness the monster's weakness
   * @return the created monster
   */
  public Monster createMonster(Long id, String name, String threat, String weakness) {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(name, "name cannot be null");

    var monster = new Monster(id, name, threat, weakness, true);
    monsters.put(id, monster);
    monsterByName.put(name, monster);
    return monster;
  }

  /**
   * Finds a monster by name.
   *
   * @param name the monster's name
   * @return Optional containing the monster if found
   */
  public Optional<Monster> findByName(String name) {
    return Optional.ofNullable(monsterByName.get(name));
  }

  /**
   * Finds a monster by ID.
   *
   * @param id the monster's ID
   * @return Optional containing the monster if found
   */
  public Optional<Monster> findById(Long id) {
    return Optional.ofNullable(monsters.get(id));
  }

  /**
   * Marks a monster as dead.
   *
   * @param monsterName the monster's name
   * @return true if successful, false if monster not found
   */
  public boolean killMonster(String monsterName) {
    Monster monster = monsterByName.get(monsterName);
    if (monster == null) {
      return false;
    }

    var updated = monster.withAlive(false);
    monsters.put(updated.id(), updated);
    monsterByName.put(updated.name(), updated);
    return true;
  }

  /**
   * Checks if a monster is alive.
   *
   * @param monsterName the monster's name
   * @return true if the monster is alive
   */
  public boolean isAlive(String monsterName) {
    Monster monster = monsterByName.get(monsterName);
    return monster != null && monster.alive();
  }

  /** Clears all monsters. */
  public void clear() {
    monsters.clear();
    monsterByName.clear();
  }

  /**
   * Returns the total number of monsters.
   *
   * @return monster count
   */
  public int count() {
    return monsters.size();
  }

  /**
   * Checks if a monster exists by name.
   *
   * @param name the monster's name
   * @return true if monster exists
   */
  public boolean exists(String name) {
    return monsterByName.containsKey(name);
  }
}
