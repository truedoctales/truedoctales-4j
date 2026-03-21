package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.MonsterService;
import org.junit.jupiter.api.Assertions;

/**
 * Business risk operations for delivery stories.
 *
 * <p>Models the silent risks that grow in the gap between what is written
 * and what is built. Each risk has a severity and a structural mitigation.
 */
@Plot("Risk")
public class RiskPlot {

  private final MonsterService monsterService;

  public RiskPlot(MonsterService monsterService) {
    this.monsterService = monsterService;
  }

  @Step(value = "Create risk",
      description = "Registers a new risk with its severity and known mitigation.")
  public void createRisk(
      @Variable(value = "id", description = "Unique identifier") Long id,
      @Variable(value = "name", description = "Risk name") String name,
      @Variable(value = "severity", description = "Risk severity: HIGH or EXTREME") String severity,
      @Variable(value = "mitigation", description = "The structural solution that defeats this risk") String mitigation) {
    monsterService.createMonster(id, name, severity, mitigation);
  }

  @Step(value = "Risk is active",
      description = "Asserts that the risk is currently active in the project.")
  public void riskIsActive(
      @Variable(value = "name", description = "Risk name") String name) {
    Assertions.assertTrue(monsterService.isAlive(name),
        "Risk '" + name + "' should be active");
  }

  @Step(value = "Risk is mitigated",
      description = "Asserts that the risk has been resolved and is no longer active.")
  public void riskIsMitigated(
      @Variable(value = "name", description = "Risk name") String name) {
    Assertions.assertFalse(monsterService.isAlive(name),
        "Risk '" + name + "' should be mitigated");
  }
}
