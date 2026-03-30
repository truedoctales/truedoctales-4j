package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Table;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.SprintService;
import dev.truedoctales.sample.domain.SprintService.Sprint;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

/**
 * Sprint lifecycle operations for Scrum-based business stories.
 *
 * <p>Tracks the critical gap between <em>reported</em> velocity (what the developer said was done)
 * and <em>verified</em> velocity (what QA confirmed actually works).
 *
 * <p>A sprint where reported velocity is high but verified velocity is low has not delivered — it
 * has generated the illusion of delivery.
 */
@Plot("Sprint")
public class SprintPlot {

  private final SprintService sprintService;

  public SprintPlot(SprintService sprintService) {
    this.sprintService = sprintService;
  }

  @Step(
      value = "Plan sprint",
      description = "Creates a new sprint with its name, committed story points, and goal.")
  public void planSprint(
      @Variable(value = "id", description = "Unique sprint identifier") Long id,
      @Variable(value = "name", description = "Sprint name") String name,
      @Variable(value = "plannedPoints", description = "Story points committed for this sprint")
          Integer plannedPoints,
      @Variable(value = "goal", description = "What this sprint should deliver") String goal) {
    sprintService.createSprint(id, name, plannedPoints, goal);
  }

  @Step(
      value = "Add task ${sprint}",
      description =
          "Adds one or more tasks to the sprint. Each task has a title and story point value.")
  public void addTask(
      @Variable(value = "sprint", description = "Sprint name (inline)") String sprint,
      @Table(
              headers = {
                @Variable(value = "task", description = "Task title"),
                @Variable(value = "points", description = "Story point value")
              })
          List<Map<String, String>> rows) {
    for (var row : rows) {
      sprintService.addTask(sprint, row.get("task"), Integer.parseInt(row.get("points")));
    }
  }

  @Step(
      value = "Mark task done",
      description =
          "Developer marks a task as done. Updates reported velocity. "
              + "Does not guarantee the task is complete — that is what verification is for.")
  public void markTaskDone(
      @Variable(value = "task", description = "Task title to mark done") String task) {
    Assertions.assertTrue(
        sprintService.markTaskDone(task), "Task '" + task + "' must be registered in a sprint");
  }

  @Step(
      value = "Verify task",
      description = "QA confirms a task is fully complete. Updates verified velocity.")
  public void verifyTask(
      @Variable(value = "task", description = "Task title to verify") String task) {
    Assertions.assertTrue(
        sprintService.verifyTask(task),
        "Task '" + task + "' must be marked done before it can be verified");
  }

  @Step(
      value = "Task is not verified",
      description =
          "Asserts that the task has NOT been verified by QA. "
              + "A done-but-unverified task is a gap in the sprint.")
  public void taskIsNotVerified(@Variable(value = "task", description = "Task title") String task) {
    Assertions.assertFalse(
        sprintService.isTaskVerified(task), "Task '" + task + "' should not be verified yet");
  }

  @Step(
      value = "Close sprint",
      description =
          "Closes the sprint. Status is COMPLETED if all done tasks are verified, FAILED otherwise.")
  public void closeSprint(@Variable(value = "sprint", description = "Sprint name") String sprint) {
    Sprint closed = sprintService.closeSprint(sprint);
    Assertions.assertNotNull(closed, "Sprint '" + sprint + "' must exist");
  }

  @Step(
      value = "Sprint status is",
      description = "Asserts the sprint's final status: COMPLETED or FAILED.")
  public void sprintStatusIs(
      @Variable(value = "sprint", description = "Sprint name") String sprint,
      @Variable(value = "expected", description = "Expected status: COMPLETED or FAILED")
          String expected) {
    Sprint s =
        sprintService
            .findByName(sprint)
            .orElseThrow(() -> new AssertionError("Sprint '" + sprint + "' must exist"));
    Assertions.assertEquals(
        expected, s.status().name(), "Sprint '" + sprint + "' should have status " + expected);
  }

  @Step(
      value = "Reported velocity is",
      description =
          "Asserts the developer-reported story points for the sprint. "
              + "This is what the burndown chart shows.")
  public void reportedVelocityIs(
      @Variable(value = "sprint", description = "Sprint name") String sprint,
      @Variable(value = "expected", description = "Expected reported points") Integer expected) {
    Sprint s =
        sprintService
            .findByName(sprint)
            .orElseThrow(() -> new AssertionError("Sprint '" + sprint + "' must exist"));
    Assertions.assertEquals(
        expected,
        s.reportedPoints(),
        "Sprint '" + sprint + "' reported velocity should be " + expected);
  }

  @Step(
      value = "Verified velocity is",
      description =
          "Asserts the QA-verified story points for the sprint. "
              + "This is the real velocity — what was actually proven to work.")
  public void verifiedVelocityIs(
      @Variable(value = "sprint", description = "Sprint name") String sprint,
      @Variable(value = "expected", description = "Expected verified points") Integer expected) {
    Sprint s =
        sprintService
            .findByName(sprint)
            .orElseThrow(() -> new AssertionError("Sprint '" + sprint + "' must exist"));
    Assertions.assertEquals(
        expected,
        s.verifiedPoints(),
        "Sprint '" + sprint + "' verified velocity should be " + expected);
  }
}
