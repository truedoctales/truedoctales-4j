package dev.truedoctales.sample.spring.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.HeroService;
import org.junit.jupiter.api.Assertions;

@Plot("Spring Team")
public class TeamMemberSpringPlot {

  private final HeroService heroService;

  public TeamMemberSpringPlot(HeroService heroService) {
    this.heroService = heroService;
  }

  @Step(value = "Create team member", description = "Registers a team member using a Spring bean.")
  public void createTeamMember(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "name", description = "Team member name") String name,
      @Variable(value = "role", description = "Team member role") String role) {
    heroService.createHero(id, name, role, 0);
  }

  @Step(value = "Team member exists", description = "Verifies the Spring-managed service state.")
  public void teamMemberExists(
      @Variable(value = "name", description = "Team member name") String name) {
    Assertions.assertTrue(heroService.exists(name), "Team member '" + name + "' should exist");
  }
}
