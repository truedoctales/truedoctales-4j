package dev.truedoctales.sample.domain;

import java.util.*;

/**
 * Service for managing software projects at the business level.
 *
 * <p>A project tracks its team and the features it claims to support. Features without verified
 * examples are not live — they are aspirations.
 */
public class ProjectService {

  /** A software project with a name and delivery goal. */
  public record Project(Long id, String name, String goal) {}

  private final Map<Long, Project> projects = new HashMap<>();
  private final Map<String, Project> byName = new LinkedHashMap<>();

  /**
   * Creates a new project.
   *
   * @param id unique identifier
   * @param name project name
   * @param goal the project's delivery goal
   */
  public Project createProject(Long id, String name, String goal) {
    var project = new Project(id, name, goal);
    projects.put(id, project);
    byName.put(name, project);
    return project;
  }

  /**
   * Returns true if a project with the given name exists.
   *
   * @param name the project name
   */
  public boolean exists(String name) {
    return byName.containsKey(name);
  }

  /**
   * Finds a project by name.
   *
   * @param name the project name
   */
  public Optional<Project> findByName(String name) {
    return Optional.ofNullable(byName.get(name));
  }

  /** Clears all projects. */
  public void clear() {
    projects.clear();
    byName.clear();
  }
}
