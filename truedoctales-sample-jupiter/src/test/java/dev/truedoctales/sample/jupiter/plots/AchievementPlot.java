package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import java.util.*;

/**
 * Achievement tracking for Greek mythology heroes.
 *
 * <p>Demonstrates achievement/trophy system.
 */
@Plot("Achievement")
public class AchievementPlot {

  private final Set<String> achievements = new HashSet<>();

  @Step("Unlocked")
  public void unlocked(String hero, String achievement) {
    achievements.add(hero + ":" + achievement);
  }
}
