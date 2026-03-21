package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.FightService;
import dev.truedoctales.sample.domain.FightService.AttackResult;
import org.junit.jupiter.api.Assertions;

/**
 * Attempt operations for business problem stories.
 *
 * <p>Models the futile attempts teams make to resolve business risks
 * without the right tools — and the successful resolutions that become
 * possible once they have the right skills.
 */
@Plot("Attempt")
public class AttemptPlot {

  private final FightService fightService;

  public AttemptPlot(FightService fightService) {
    this.fightService = fightService;
  }

  @Step(value = "Fails",
      description = "Records an attempt to resolve a business risk that fails. "
          + "The approach is insufficient without the right structural solution.")
  public void fails(
      @Variable(value = "teamMember", description = "Who is attempting the resolution") String teamMember,
      @Variable(value = "risk", description = "The business risk being addressed") String risk,
      @Variable(value = "approach", description = "The approach taken") String approach,
      @Variable(value = "result", description = "Expected result — must be FAILED") String result) {
    AttackResult attackResult = fightService.attack(teamMember, risk, approach);
    Assertions.assertFalse(attackResult.success(), "Attempt should fail without the right skill");
    Assertions.assertEquals("FAILED", result, "Expected FAILED result");
  }

  @Step(value = "Succeeds with skill",
      description = "A team member resolves a business risk by applying the correct skill. "
          + "The team member must have the skill and the risk must still be active.")
  public void succeedsWithSkill(
      @Variable(value = "teamMember", description = "Who is resolving the risk") String teamMember,
      @Variable(value = "risk", description = "The business risk to resolve") String risk,
      @Variable(value = "skill", description = "The skill applied") String skill,
      @Variable(value = "outcome", description = "Expected outcome — must be RESOLVED") String outcome) {
    AttackResult attackResult = fightService.defeatWithSkill(teamMember, risk, skill);
    Assertions.assertTrue(attackResult.success(),
        "Team member '" + teamMember + "' should resolve '" + risk + "' with skill '" + skill + "'");
    Assertions.assertEquals("RESOLVED", outcome, "Expected RESOLVED outcome");
  }
}
