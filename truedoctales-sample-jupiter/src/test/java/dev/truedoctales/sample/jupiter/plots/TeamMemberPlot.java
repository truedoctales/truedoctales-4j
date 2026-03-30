package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.HeroService;
import org.junit.jupiter.api.Assertions;

/**
 * Team member operations for business stories.
 *
 * <p>Models the people on a software delivery team — their roles, skills, and the habits that
 * quietly undermine every sprint.
 */
@Plot("Team Member")
public class TeamMemberPlot {

  private final HeroService heroService;

  public TeamMemberPlot(HeroService heroService) {
    this.heroService = heroService;
  }

  @Step(
      value = "Create team member",
      description = "Registers a new team member with their name and role.")
  public void createTeamMember(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "name", description = "Full display name") String name,
      @Variable(value = "role", description = "Job role") String role) {
    heroService.createHero(id, name, role, 0);
  }

  @Step(
      value = "Team member exists",
      description = "Asserts that a team member with this name has been registered.")
  public void teamMemberExists(
      @Variable(value = "name", description = "Team member name") String name) {
    Assertions.assertTrue(heroService.exists(name), "Team member '" + name + "' should exist");
  }

  @Step(value = "Grant skill", description = "Adds a skill to a team member's profile.")
  public void grantSkill(
      @Variable(value = "teamMember", description = "Name of the team member") String teamMember,
      @Variable(value = "skill", description = "Skill to add") String skill) {
    Assertions.assertTrue(
        heroService.grantSkill(teamMember, skill), "Team member '" + teamMember + "' must exist");
  }

  @Step(value = "Has skill", description = "Asserts that the team member has the specified skill.")
  public void hasSkill(
      @Variable(value = "teamMember", description = "Name of the team member") String teamMember,
      @Variable(value = "skill", description = "Expected skill") String skill) {
    Assertions.assertTrue(
        heroService.hasSkill(teamMember, skill),
        "Team member '" + teamMember + "' should have skill '" + skill + "'");
  }

  @Step(
      value = "Grant weakness",
      description = "Records a known weakness in a team member's working habits.")
  public void grantWeakness(
      @Variable(value = "teamMember", description = "Name of the team member") String teamMember,
      @Variable(value = "weakness", description = "Weakness to record") String weakness) {
    Assertions.assertTrue(
        heroService.grantWeakness(teamMember, weakness),
        "Team member '" + teamMember + "' must exist");
  }

  @Step(
      value = "Has weakness",
      description = "Asserts that the team member has the specified weakness on record.")
  public void hasWeakness(
      @Variable(value = "teamMember", description = "Name of the team member") String teamMember,
      @Variable(value = "weakness", description = "Expected weakness") String weakness) {
    Assertions.assertTrue(
        heroService.hasWeakness(teamMember, weakness),
        "Team member '" + teamMember + "' should have weakness '" + weakness + "'");
  }

  @Step(value = "Trophy earned", description = "Awards a recognition trophy to the team member.")
  public void trophyEarned(
      @Variable(value = "teamMember", description = "Name of the team member") String teamMember,
      @Variable(value = "trophy", description = "Trophy to award") String trophy) {
    Assertions.assertTrue(
        heroService.awardTrophy(teamMember, trophy), "Team member '" + teamMember + "' must exist");
  }
}
