package dev.truedoctales.sample.jupiter;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.annotations.Scene;
import dev.truedoctales.api.annotations.Story;
import dev.truedoctales.execution.extension.StoryExtension;
import dev.truedoctales.sample.domain.FightService;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.domain.HeroService.Hero;
import dev.truedoctales.sample.domain.MonsterService;
import dev.truedoctales.sample.domain.QuestService;
import dev.truedoctales.sample.domain.QuestService.Quest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/// Code-based version of "The Developer Who Was Always Done" story.
///
/// This story mirrors the Markdown version in 03_squirrel-dev but is written entirely in Java.
/// It demonstrates that code-based and markdown-based stories can coexist and
/// produce the same documentation output.
///
/// Alex — a fast developer who reads the first acceptance criterion and marks tickets done.
/// Maria — a QA engineer who returns from sick leave to find four missing criteria.
/// The sprint that could not close because velocity had been mistaken for quality.
@Story(
    book = "Book of Stories",
    storyPath = "03_squirrel-dev/01_squirrel-dev.md",
    title = "The Developer Who Was Always Done (Code)",
    markdown =
        """
            # The Developer Who Was Always Done (Code)

            Sprint 14 has the highest velocity in FinTrack history.
            Alex has closed six tickets. Maria comes back on Thursday.
            She opens the first ticket. It has five acceptance criteria.
            One of them is implemented.
            """)
@ExtendWith(StoryExtension.class)
public class SquirrelDevCodeTest {

  private HeroService heroService;
  private MonsterService monsterService;
  private QuestService questService;
  private FightService fightService;

  @BeforeEach
  void setUp() {
    heroService = new HeroService();
    monsterService = new MonsterService();
    questService = new QuestService();
    fightService = new FightService(heroService, monsterService);
  }

  @Scene(
      title = "The user registration story — five criteria, all important",
      description =
          """
          Emma has written the most complete story of her career.
          Five acceptance criteria. Each one necessary.
          The enterprise client InnoConnect's go-live depends on it.

          The story is assigned to Alex on Monday morning.
          """)
  @Test
  void userRegistrationStoryCreatedAndAssigned() {
    Hero alex = heroService.createHero(11L, "Alex", "Human", 29);
    assertNotNull(alex);
    assertTrue(heroService.grantSkill("Alex", "Java"));
    assertTrue(heroService.grantSkill("Alex", "REST API Design"));
    assertTrue(heroService.grantSkill("Alex", "Fast Delivery"));

    Quest quest =
        questService.createQuest(
            11L,
            "Implement User Registration",
            "Register new users: validation, email confirmation, duplicate check, and GDPR logging",
            "IN_PROGRESS");
    assertNotNull(quest);
    assertEquals("IN_PROGRESS", quest.status());

    assertTrue(questService.assignToHero("Alex", "Implement User Registration"));

    Quest assigned = questService.findByName("Implement User Registration").orElseThrow();
    assertEquals("IN_PROGRESS", assigned.status());
    assertEquals("Alex", questService.getHeroQuest("Alex").orElseThrow());
  }

  @Scene(
      title = "Alex reads the first line and marks the ticket done",
      description =
          """
          Alex scans the story. His eyes land on criterion 1.
          He implements it in three hours. He marks the ticket done.
          He picks up the next ticket.

          Criteria 2 through 5 — including the GDPR audit log — are untouched.
          The ticket is green. Four acceptance criteria watch him leave.
          """)
  @Test
  void alexClosesTicketAfterOneCriterion() {
    heroService.createHero(11L, "Alex", "Human", 29);
    questService.createQuest(
        11L,
        "Implement User Registration",
        "Register new users: all 5 criteria",
        "IN_PROGRESS");
    questService.assignToHero("Alex", "Implement User Registration");

    // Alex marks done after implementing only criterion 1
    assertTrue(questService.completeQuest("Implement User Registration"));

    Quest ticket = questService.findByName("Implement User Registration").orElseThrow();
    // The ticket says COMPLETED — but only one fifth of it was built
    assertEquals("COMPLETED", ticket.status());

    // The partial implementation and missing tests are now active threats
    monsterService.createMonster(15L, "Partial Implementation", "HIGH", "Complete Specification Coverage");
    monsterService.createMonster(11L, "Missing Acceptance Test", "HIGH", "Automated Verification");
    assertTrue(monsterService.isAlive("Partial Implementation"));
    assertTrue(monsterService.isAlive("Missing Acceptance Test"));
  }

  @Scene(
      title = "Maria comes back on Thursday",
      description =
          """
          Maria returns from sick leave. She opens the user registration ticket.
          She reads all five criteria. She starts checking.

          By 9:30 she has found four gaps.
          Criterion 5 — the GDPR audit log — is missing.
          That is not a bug. That is a compliance violation.
          """)
  @Test
  void mariaDiscoversTheFourMissingCriteria() {
    heroService.createHero(11L, "Alex", "Human", 29);
    heroService.createHero(12L, "Maria", "Human", 33);
    assertTrue(heroService.grantSkill("Maria", "Test Automation"));
    assertTrue(heroService.grantSkill("Maria", "Defect Analysis"));

    monsterService.createMonster(15L, "Partial Implementation", "HIGH", "Complete Specification Coverage");
    monsterService.createMonster(11L, "Missing Acceptance Test", "HIGH", "Automated Verification");

    // Maria tries to fight the partial implementation with her test report
    // She cannot defeat it — the gap is already built in
    var testReport = fightService.attack("Maria", "Partial Implementation", "Test Report");
    assertFalse(testReport.success(), "A test report cannot fix missing implementation");

    // Both threats remain alive — Maria has found them, but cannot resolve them alone
    assertTrue(monsterService.isAlive("Partial Implementation"));
    assertTrue(monsterService.isAlive("Missing Acceptance Test"));
  }

  @Scene(
      title = "The sprint review — forty-three points, nothing to ship",
      description =
          """
          Friday. Sprint review. The burndown chart looks perfect.
          Thomas has a call with InnoConnect at 4 PM.

          Maria explains the four missing criteria.
          She explains the GDPR audit log — a contractual obligation.

          Alex says: "I built the registration. I thought that was the ticket."
          The sprint cannot close. InnoConnect go-live is postponed two weeks.
          Thomas cancels the 4 PM call and sends an apology email instead.
          """)
  @Test
  void sprintReviewCannotClose() {
    heroService.createHero(11L, "Alex", "Human", 29);
    heroService.createHero(12L, "Maria", "Human", 33);
    heroService.createHero(13L, "Thomas", "Human", 52);
    heroService.createHero(14L, "Stefan", "Human", 41);

    monsterService.createMonster(15L, "Partial Implementation", "HIGH", "Complete Specification Coverage");
    monsterService.createMonster(12L, "Blame Culture", "EXTREME", "Shared Accountability");

    // Stefan's code review comes too late — the damage is already merged
    var codeReview = fightService.attack("Stefan", "Partial Implementation", "Code Review");
    assertFalse(codeReview.success(), "A code review cannot retroactively add missing criteria");

    // Thomas raises the issue in the sprint review — but feedback alone cannot fix it
    var sprintFeedback = fightService.attack("Thomas", "Partial Implementation", "Sprint Feedback");
    assertFalse(sprintFeedback.success(), "Sprint feedback cannot fix missing implementation");

    // Blame culture emerges when there is no shared evidence of what was agreed
    assertTrue(monsterService.isAlive("Blame Culture"));
    assertTrue(monsterService.isAlive("Partial Implementation"));

    // Both heroes end the sprint at the same level they started — no growth from this failure
    Hero verifiedAlex = heroService.findByName("Alex").orElseThrow();
    Hero verifiedStefan = heroService.findByName("Stefan").orElseThrow();
    assertEquals(1, verifiedAlex.level());
    assertEquals(1, verifiedStefan.level());
  }
}
