package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import java.util.*;

/**
 * Achievement tracking for Greek mythology heroes.
 *
 * <p>Demonstrates achievement/trophy system.
 */
@Plot("Achievement")
public class AchievementPlot {

  private final Set<String> achievements = new HashSet<>();

  @Step(value = "Unlocked", description = "Records that the hero has unlocked an achievement.")
  public void unlocked(
      @Variable(value = "hero", description = "Name of the hero") String hero,
      @Variable(value = "achievement", description = "Achievement name") String achievement) {
    achievements.add(hero + ":" + achievement);
  }
}
