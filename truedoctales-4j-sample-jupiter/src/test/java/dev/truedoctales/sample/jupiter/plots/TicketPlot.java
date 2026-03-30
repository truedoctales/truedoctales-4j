package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.domain.QuestService;
import dev.truedoctales.sample.domain.QuestService.Quest;
import org.junit.jupiter.api.Assertions;

/**
 * Ticket (task) lifecycle operations for sprint stories.
 *
 * <p>Tracks a ticket from creation through assignment to completion, verifying that the developer
 * who marks it done actually owns it.
 */
@Plot("Ticket")
public class TicketPlot {

  private final QuestService questService;
  private final HeroService heroService;

  public TicketPlot(QuestService questService, HeroService heroService) {
    this.questService = questService;
    this.heroService = heroService;
  }

  @Step(
      value = "Create ticket",
      description = "Creates a new sprint ticket with a title, description and initial status.")
  public void createTicket(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "title", description = "Ticket title") String title,
      @Variable(value = "description", description = "What needs to be done") String description,
      @Variable(value = "status", description = "Initial status") String status) {
    questService.createQuest(id, title, description, status);
  }

  @Step(
      value = "Assign to developer",
      description = "Assigns the ticket to a developer. Both must already exist.")
  public void assignToDeveloper(
      @Variable(value = "developer", description = "Name of the developer") String developer,
      @Variable(value = "ticket", description = "Ticket title") String ticket) {
    Assertions.assertTrue(
        heroService.exists(developer), "Team member '" + developer + "' must exist");
    Assertions.assertTrue(questService.exists(ticket), "Ticket '" + ticket + "' must exist");
    questService.assignToHero(developer, ticket);
  }

  @Step(
      value = "Ticket status is",
      description = "Asserts that the ticket has the expected status.")
  public void ticketStatusIs(
      @Variable(value = "ticket", description = "Ticket title") String ticket,
      @Variable(value = "expectedStatus", description = "Expected status value")
          String expectedStatus) {
    Quest q =
        questService
            .findByName(ticket)
            .orElseThrow(() -> new AssertionError("Ticket '" + ticket + "' must exist"));
    Assertions.assertEquals(
        expectedStatus, q.status(), "Ticket '" + ticket + "' should have status " + expectedStatus);
  }

  @Step(
      value = "Close ticket",
      description =
          "Closes the ticket assigned to the developer. The ticket must be assigned to them.")
  public void closeTicket(
      @Variable(value = "developer", description = "Name of the developer") String developer,
      @Variable(value = "ticket", description = "Ticket title") String ticket) {
    String devTicket =
        questService
            .getHeroQuest(developer)
            .orElseThrow(
                () -> new AssertionError("Developer '" + developer + "' has no assigned ticket"));
    Assertions.assertEquals(
        ticket,
        devTicket,
        "Developer '" + developer + "' must have ticket '" + ticket + "' assigned");
    questService.completeQuest(ticket);
  }
}
