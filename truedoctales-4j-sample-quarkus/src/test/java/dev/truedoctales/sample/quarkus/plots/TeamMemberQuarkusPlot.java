package dev.truedoctales.sample.quarkus.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.HeroService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;

@ApplicationScoped
@Plot("Quarkus Team")
public class TeamMemberQuarkusPlot {

  @Inject HeroService heroService;

  @Step(value = "Create team member", description = "Registers a team member using a Quarkus bean.")
  public void createTeamMember(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "name", description = "Team member name") String name,
      @Variable(value = "role", description = "Team member role") String role) {
    heroService.createHero(id, name, role, 0);
  }

  @Step(value = "Team member exists", description = "Verifies the Quarkus-managed service state.")
  public void teamMemberExists(
      @Variable(value = "name", description = "Team member name") String name) {
    Assertions.assertTrue(heroService.exists(name), "Team member '" + name + "' should exist");
  }
}
