package dev.truedoctales.sample.domain;

import java.util.*;

/**
 * Service for managing Scrum sprints and tracking the gap between reported and verified velocity.
 *
 * <p>The core insight: a sprint with high <em>reported</em> velocity but low <em>verified</em>
 * velocity has not delivered — it has created the illusion of delivery.
 */
public class SprintService {

  /** Sprint lifecycle status. */
  public enum SprintStatus {
    ACTIVE,
    COMPLETED,
    FAILED
  }

  /** A sprint with its planning and delivery metrics. */
  public record Sprint(
      Long id,
      String name,
      int plannedPoints,
      String goal,
      SprintStatus status,
      int reportedPoints,
      int verifiedPoints) {

    public Sprint withStatus(SprintStatus newStatus) {
      return new Sprint(id, name, plannedPoints, goal, newStatus, reportedPoints, verifiedPoints);
    }

    public Sprint withReported(int points) {
      return new Sprint(id, name, plannedPoints, goal, status, points, verifiedPoints);
    }

    public Sprint withVerified(int points) {
      return new Sprint(id, name, plannedPoints, goal, status, reportedPoints, points);
    }
  }

  private final Map<Long, Sprint> sprints = new HashMap<>();
  private final Map<String, Sprint> byName = new LinkedHashMap<>();

  private final Map<String, String> taskToSprint = new HashMap<>();
  private final Map<String, Integer> taskPoints = new HashMap<>();
  private final Set<String> doneTasks = new LinkedHashSet<>();
  private final Set<String> verifiedTasks = new LinkedHashSet<>();

  /**
   * Creates a new sprint in ACTIVE state.
   *
   * @param id unique sprint identifier
   * @param name sprint name
   * @param plannedPoints story points committed for this sprint
   * @param goal the sprint goal
   */
  public Sprint createSprint(Long id, String name, int plannedPoints, String goal) {
    var sprint = new Sprint(id, name, plannedPoints, goal, SprintStatus.ACTIVE, 0, 0);
    sprints.put(id, sprint);
    byName.put(name, sprint);
    return sprint;
  }

  /**
   * Adds a task to a sprint with its story point value.
   *
   * @param sprintName the sprint name
   * @param taskName the task name
   * @param points story points for this task
   */
  public boolean addTask(String sprintName, String taskName, int points) {
    if (!byName.containsKey(sprintName)) return false;
    taskToSprint.put(taskName, sprintName);
    taskPoints.put(taskName, points);
    return true;
  }

  /**
   * Marks a task as done (developer-reported). Updates the sprint's reported velocity.
   *
   * @param taskName the task to mark done
   */
  public boolean markTaskDone(String taskName) {
    if (!taskToSprint.containsKey(taskName)) return false;
    if (doneTasks.add(taskName)) {
      updateSprintReported(taskToSprint.get(taskName));
    }
    return true;
  }

  /**
   * Marks a task as verified (QA-confirmed). Updates the sprint's verified velocity.
   *
   * @param taskName the task to verify
   */
  public boolean verifyTask(String taskName) {
    if (!taskToSprint.containsKey(taskName)) return false;
    if (verifiedTasks.add(taskName)) {
      updateSprintVerified(taskToSprint.get(taskName));
    }
    return true;
  }

  /**
   * Returns true if the task has been marked done by the developer.
   *
   * @param taskName the task name
   */
  public boolean isTaskDone(String taskName) {
    return doneTasks.contains(taskName);
  }

  /**
   * Returns true if the task has been verified by QA.
   *
   * @param taskName the task name
   */
  public boolean isTaskVerified(String taskName) {
    return verifiedTasks.contains(taskName);
  }

  /**
   * Closes the sprint. Status is COMPLETED if all done tasks are verified; FAILED otherwise.
   *
   * @param sprintName the sprint to close
   */
  public Sprint closeSprint(String sprintName) {
    Sprint sprint = byName.get(sprintName);
    if (sprint == null) return null;
    SprintStatus status =
        verifiedTasks.containsAll(doneTasks) ? SprintStatus.COMPLETED : SprintStatus.FAILED;
    var updated = sprint.withStatus(status);
    byName.put(sprintName, updated);
    sprints.put(updated.id(), updated);
    return updated;
  }

  /**
   * Finds a sprint by name.
   *
   * @param name the sprint name
   */
  public Optional<Sprint> findByName(String name) {
    return Optional.ofNullable(byName.get(name));
  }

  /** Clears all sprint data. */
  public void clear() {
    sprints.clear();
    byName.clear();
    taskToSprint.clear();
    taskPoints.clear();
    doneTasks.clear();
    verifiedTasks.clear();
  }

  private void updateSprintReported(String sprintName) {
    Sprint sprint = byName.get(sprintName);
    if (sprint == null) return;
    int total =
        doneTasks.stream()
            .filter(t -> sprintName.equals(taskToSprint.get(t)))
            .mapToInt(t -> taskPoints.getOrDefault(t, 0))
            .sum();
    var updated = sprint.withReported(total);
    byName.put(sprintName, updated);
    sprints.put(updated.id(), updated);
  }

  private void updateSprintVerified(String sprintName) {
    Sprint sprint = byName.get(sprintName);
    if (sprint == null) return;
    int total =
        verifiedTasks.stream()
            .filter(t -> sprintName.equals(taskToSprint.get(t)))
            .mapToInt(t -> taskPoints.getOrDefault(t, 0))
            .sum();
    var updated = sprint.withVerified(total);
    byName.put(sprintName, updated);
    sprints.put(updated.id(), updated);
  }
}
