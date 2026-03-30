package dev.truedoctales.sample.domain;

import java.util.*;

/**
 * Service for managing quests in Greek mythology stories.
 *
 * <p>Demonstrates domain service pattern with in-memory storage.
 */
public class QuestService {

  private final Map<Long, Quest> quests = new HashMap<>();
  private final Map<String, Quest> questByName = new HashMap<>();
  private final Map<String, String> heroQuests = new HashMap<>(); // hero -> quest name

  /** Quest record representing a quest with status. */
  public record Quest(Long id, String name, String description, String status) {

    public Quest withStatus(String newStatus) {
      return new Quest(id, name, description, newStatus);
    }
  }

  /**
   * Creates a new quest.
   *
   * @param id the unique quest identifier
   * @param name the quest's name
   * @param description the quest description
   * @param status the quest status
   * @return the created quest
   */
  public Quest createQuest(Long id, String name, String description, String status) {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(name, "name cannot be null");

    var quest = new Quest(id, name, description, status);
    quests.put(id, quest);
    questByName.put(name, quest);
    return quest;
  }

  /**
   * Finds a quest by name.
   *
   * @param name the quest's name
   * @return Optional containing the quest if found
   */
  public Optional<Quest> findByName(String name) {
    return Optional.ofNullable(questByName.get(name));
  }

  /**
   * Finds a quest by ID.
   *
   * @param id the quest's ID
   * @return Optional containing the quest if found
   */
  public Optional<Quest> findById(Long id) {
    return Optional.ofNullable(quests.get(id));
  }

  /**
   * Assigns a quest to a hero.
   *
   * @param heroName the hero's name
   * @param questName the quest's name
   * @return true if successful, false if quest not found
   */
  public boolean assignToHero(String heroName, String questName) {
    if (!questByName.containsKey(questName)) {
      return false;
    }
    heroQuests.put(heroName, questName);
    return true;
  }

  /**
   * Gets the quest assigned to a hero.
   *
   * @param heroName the hero's name
   * @return Optional containing the quest name if found
   */
  public Optional<String> getHeroQuest(String heroName) {
    return Optional.ofNullable(heroQuests.get(heroName));
  }

  /**
   * Completes a quest.
   *
   * @param questName the quest's name
   * @return true if successful, false if quest not found
   */
  public boolean completeQuest(String questName) {
    Quest quest = questByName.get(questName);
    if (quest == null) {
      return false;
    }

    var updated = quest.withStatus("COMPLETED");
    quests.put(updated.id(), updated);
    questByName.put(updated.name(), updated);
    return true;
  }

  /**
   * Updates a quest's status.
   *
   * @param questName the quest's name
   * @param newStatus the new status
   * @return true if successful, false if quest not found
   */
  public boolean updateStatus(String questName, String newStatus) {
    Quest quest = questByName.get(questName);
    if (quest == null) {
      return false;
    }

    var updated = quest.withStatus(newStatus);
    quests.put(updated.id(), updated);
    questByName.put(updated.name(), updated);
    return true;
  }

  /** Clears all quests. */
  public void clear() {
    quests.clear();
    questByName.clear();
    heroQuests.clear();
  }

  /**
   * Returns the total number of quests.
   *
   * @return quest count
   */
  public int count() {
    return quests.size();
  }

  /**
   * Checks if a quest exists by name.
   *
   * @param name the quest's name
   * @return true if quest exists
   */
  public boolean exists(String name) {
    return questByName.containsKey(name);
  }
}
