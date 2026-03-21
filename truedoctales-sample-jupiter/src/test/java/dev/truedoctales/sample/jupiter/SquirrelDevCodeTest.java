package dev.truedoctales.sample.jupiter;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.annotations.Scene;
import dev.truedoctales.api.annotations.Story;
import dev.truedoctales.execution.extension.StoryExtension;
import dev.truedoctales.sample.domain.FightService;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.domain.MonsterService;
import dev.truedoctales.sample.domain.SpecificationService;
import dev.truedoctales.sample.domain.SprintService;
import dev.truedoctales.sample.domain.SprintService.Sprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/// Code-based story: "The Developer Who Was Always Done"
///
/// This story mirrors the Markdown version in 03_squirrel-dev but is written entirely in Java.
/// It demonstrates that code-based and markdown-based stories can coexist.
///
/// The Scrum anti-pattern on display:
/// - Checklist Charlie marks 6 sprint tasks done (43 reported points) after reading criterion 1
/// - Bugfinder Betty verifies none of them — they are all incomplete
/// - Sprint 14 closes as FAILED: reported velocity 43, verified velocity 0
/// - The GDPR audit log was never built — a compliance violation hiding inside a green ticket
@Story(
    book = "Book of Stories",
    storyPath = "03_squirrel-dev/01_squirrel-dev.md",
    title = "The Developer Who Was Always Done (Code)",
    markdown =
        """
            # The Developer Who Was Always Done (Code)

            Sprint 14 at FinTrack Solutions.
            Reported velocity: 43 points. Verified velocity: 0.
            The sprint closes as FAILED.
            Checklist Charlie has moved on to Sprint 15.
            """)
@ExtendWith(StoryExtension.class)
public class SquirrelDevCodeTest {

  private HeroService heroService;
  private MonsterService monsterService;
  private SprintService sprintService;
  private SpecificationService specificationService;
  private FightService fightService;

  @BeforeEach
  void setUp() {
    heroService = new HeroService();
    monsterService = new MonsterService();
    sprintService = new SprintService();
    specificationService = new SpecificationService();
    fightService = new FightService(heroService, monsterService);
  }

  @Scene(
      title = "Sprint 14 is planned — five tasks, 43 points, one go-live",
      description =
          """
          The sprint plan looks solid on paper.
          Five tasks. 43 story points. InnoConnect's go-live depends on User Registration.

          The critical detail: not a single task has a specification example.
          No concrete examples means no shared understanding of what "done" looks like.
          """)
  @Test
  void sprintPlannedWithNoExamples() {
    heroService.createHero(11L, "Checklist Charlie", "Developer", 0);
    heroService.createHero(12L, "Bugfinder Betty", "QA Engineer", 0);

    sprintService.createSprint(
        1L, "Sprint 14", 43, "User Registration ready for InnoConnect go-live");
    sprintService.addTask("Sprint 14", "User Registration", 13);
    sprintService.addTask("Sprint 14", "Email Validation", 5);
    sprintService.addTask("Sprint 14", "Confirmation Email", 5);
    sprintService.addTask("Sprint 14", "Duplicate Registration Check", 5);
    sprintService.addTask("Sprint 14", "GDPR Audit Log", 8);
    sprintService.addTask("Sprint 14", "Password Reset Flow", 7);

    // No specification examples for any task — the root cause of everything that follows
    assertFalse(
        specificationService.hasExamples("User Registration"),
        "User Registration has no specification examples");
    assertFalse(
        specificationService.hasExamples("GDPR Audit Log"),
        "GDPR Audit Log has no specification examples — and it is a legal requirement");

    Sprint sprint = sprintService.findByName("Sprint 14").orElseThrow();
    assertEquals("ACTIVE", sprint.status().name());
    assertEquals(43, sprint.plannedPoints());
  }

  @Scene(
      title = "Checklist Charlie reads the first criterion and marks everything done",
      description =
          """
          Monday morning. Checklist Charlie picks up User Registration.
          He reads criterion 1: "A new user can register with email and password."
          He builds it. He marks it done. He picks up the next ticket.

          By Wednesday: six tickets closed, 43 points reported.
          The burndown chart looks like a masterclass in agile delivery.
          """)
  @Test
  void charlieReportsFullVelocityWithPartialWork() {
    heroService.createHero(11L, "Checklist Charlie", "Developer", 0);

    sprintService.createSprint(1L, "Sprint 14", 43, "User Registration go-live");
    sprintService.addTask("Sprint 14", "User Registration", 13);
    sprintService.addTask("Sprint 14", "Email Validation", 5);
    sprintService.addTask("Sprint 14", "Confirmation Email", 5);
    sprintService.addTask("Sprint 14", "Duplicate Registration Check", 5);
    sprintService.addTask("Sprint 14", "GDPR Audit Log", 8);
    sprintService.addTask("Sprint 14", "Password Reset Flow", 7);

    // Charlie marks everything done — criterion 1 per task
    sprintService.markTaskDone("User Registration");
    sprintService.markTaskDone("Email Validation");
    sprintService.markTaskDone("Confirmation Email");
    sprintService.markTaskDone("Duplicate Registration Check");
    sprintService.markTaskDone("GDPR Audit Log");
    sprintService.markTaskDone("Password Reset Flow");

    Sprint sprint = sprintService.findByName("Sprint 14").orElseThrow();
    assertEquals(
        43,
        sprint.reportedPoints(),
        "Reported velocity is 43 — exactly as planned. The chart looks great.");
    assertEquals(
        0, sprint.verifiedPoints(), "Verified velocity is 0 — nothing has been QA-confirmed yet.");
  }

  @Scene(
      title = "Bugfinder Betty comes back — and cannot verify a single task",
      description =
          """
          Thursday. Bugfinder Betty returns from sick leave.
          She opens the sprint board. Six green tickets. 43 points.
          She opens User Registration. She reads all five acceptance criteria.
          She starts checking.

          Email validation: broken. Confirmation email: missing.
          Duplicate check: not implemented. GDPR audit log: zero entries.

          She cannot verify any task. She reopens all six tickets.
          """)
  @Test
  void bettyCannotVerifyAnything() {
    heroService.createHero(11L, "Checklist Charlie", "Developer", 0);
    heroService.createHero(12L, "Bugfinder Betty", "QA Engineer", 0);

    sprintService.createSprint(1L, "Sprint 14", 43, "User Registration go-live");
    sprintService.addTask("Sprint 14", "User Registration", 13);
    sprintService.addTask("Sprint 14", "Email Validation", 5);
    sprintService.addTask("Sprint 14", "Confirmation Email", 5);
    sprintService.addTask("Sprint 14", "Duplicate Registration Check", 5);
    sprintService.addTask("Sprint 14", "GDPR Audit Log", 8);
    sprintService.addTask("Sprint 14", "Password Reset Flow", 7);

    sprintService.markTaskDone("User Registration");
    sprintService.markTaskDone("Email Validation");
    sprintService.markTaskDone("Confirmation Email");
    sprintService.markTaskDone("Duplicate Registration Check");
    sprintService.markTaskDone("GDPR Audit Log");
    sprintService.markTaskDone("Password Reset Flow");

    // Betty cannot verify any task — they are all incomplete
    assertFalse(sprintService.isTaskVerified("User Registration"));
    assertFalse(sprintService.isTaskVerified("Email Validation"));
    assertFalse(sprintService.isTaskVerified("Confirmation Email"));
    assertFalse(sprintService.isTaskVerified("Duplicate Registration Check"));
    assertFalse(sprintService.isTaskVerified("GDPR Audit Log"));
    assertFalse(sprintService.isTaskVerified("Password Reset Flow"));

    Sprint sprint = sprintService.findByName("Sprint 14").orElseThrow();
    assertEquals(43, sprint.reportedPoints(), "Reported: 43");
    assertEquals(0, sprint.verifiedPoints(), "Verified: 0");
  }

  @Scene(
      title = "Sprint 14 closes as FAILED — the burndown chart was fiction",
      description =
          """
          Friday sprint review. The burndown chart shows a perfect sprint.
          Thomas has a call with InnoConnect at 4 PM.

          Bugfinder Betty explains: six tasks marked done. Zero verified.
          GDPR audit log missing — a contractual and legal requirement.
          InnoConnect's go-live is postponed two weeks.

          Thomas cancels the 4 PM call.
          The sprint closes as FAILED.
          The velocity was fiction.
          """)
  @Test
  void sprintClosesAsFailed() {
    heroService.createHero(11L, "Checklist Charlie", "Developer", 0);
    heroService.createHero(12L, "Bugfinder Betty", "QA Engineer", 0);
    heroService.createHero(13L, "Mirror Mike", "CFO", 0);

    sprintService.createSprint(1L, "Sprint 14", 43, "User Registration go-live");
    sprintService.addTask("Sprint 14", "User Registration", 13);
    sprintService.addTask("Sprint 14", "Email Validation", 5);
    sprintService.addTask("Sprint 14", "Confirmation Email", 5);
    sprintService.addTask("Sprint 14", "Duplicate Registration Check", 5);
    sprintService.addTask("Sprint 14", "GDPR Audit Log", 8);
    sprintService.addTask("Sprint 14", "Password Reset Flow", 7);

    // Charlie marks all done
    sprintService.markTaskDone("User Registration");
    sprintService.markTaskDone("Email Validation");
    sprintService.markTaskDone("Confirmation Email");
    sprintService.markTaskDone("Duplicate Registration Check");
    sprintService.markTaskDone("GDPR Audit Log");
    sprintService.markTaskDone("Password Reset Flow");

    // Betty verifies nothing
    Sprint beforeClose = sprintService.findByName("Sprint 14").orElseThrow();
    assertEquals(43, beforeClose.reportedPoints());
    assertEquals(0, beforeClose.verifiedPoints());

    // Sprint closes
    Sprint closed = sprintService.closeSprint("Sprint 14");
    assertNotNull(closed);

    // The verdict
    assertEquals(
        SprintService.SprintStatus.FAILED,
        closed.status(),
        "Sprint FAILED: 43 points reported, 0 verified");
    assertEquals(43, closed.reportedPoints(), "The burndown chart showed 43 delivered points");
    assertEquals(0, closed.verifiedPoints(), "Zero points were actually verified by QA");
  }
}
