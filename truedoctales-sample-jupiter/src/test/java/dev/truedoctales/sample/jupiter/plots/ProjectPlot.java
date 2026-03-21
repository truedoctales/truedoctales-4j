package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.ProjectService;
import dev.truedoctales.sample.domain.SpecificationService;
import org.junit.jupiter.api.Assertions;

/**
 * Project-level operations linking business features to their specification evidence.
 *
 * <p>The key insight: a feature is only "live" if it has at least one verified
 * specification example. Documentation alone is not proof of existence.
 *
 * <p>Use {@code Feature not live} to expose the gap between the product catalogue
 * and the actual running system.
 */
@Plot("Project")
public class ProjectPlot {

  private final ProjectService projectService;
  private final SpecificationService specificationService;

  public ProjectPlot(ProjectService projectService, SpecificationService specificationService) {
    this.projectService = projectService;
    this.specificationService = specificationService;
  }

  @Step(value = "Create project",
      description = "Registers a software project with its delivery goal.")
  public void createProject(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "name", description = "Project name") String name,
      @Variable(value = "goal", description = "Delivery goal") String goal) {
    projectService.createProject(id, name, goal);
  }

  @Step(value = "Project exists",
      description = "Asserts that the project has been registered.")
  public void projectExists(
      @Variable(value = "name", description = "Project name") String name) {
    Assertions.assertTrue(projectService.exists(name),
        "Project '" + name + "' should exist");
  }

  @Step(value = "Feature is live",
      description = "Asserts that a feature has at least one verified specification example, "
          + "proving it exists in the running system.")
  public void featureIsLive(
      @Variable(value = "project", description = "Project name") String project,
      @Variable(value = "feature", description = "Feature name") String feature) {
    Assertions.assertTrue(specificationService.hasExamples(feature),
        "Feature '" + feature + "' in project '" + project + "' has no verified examples. "
            + "It is in the catalogue — but it is not live.");
  }

  @Step(value = "Feature not live",
      description = "Asserts that a feature has NO verified specification examples. "
          + "It may appear in the product catalogue, but it cannot be proven to exist.")
  public void featureNotLive(
      @Variable(value = "project", description = "Project name") String project,
      @Variable(value = "feature", description = "Feature name") String feature) {
    Assertions.assertFalse(specificationService.hasExamples(feature),
        "Feature '" + feature + "' in project '" + project + "' has verified examples. "
            + "It IS live — check the story logic.");
  }
}
