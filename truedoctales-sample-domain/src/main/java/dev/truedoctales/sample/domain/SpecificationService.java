package dev.truedoctales.sample.domain;

import java.util.*;

/**
 * Service for tracking feature specifications and their concrete examples.
 *
 * <p>A feature without examples cannot be proven to exist. This service makes that
 * gap visible — and executable.
 */
public class SpecificationService {

  private final Map<String, List<Example>> examples = new LinkedHashMap<>();

  /** A single concrete example for a feature specification. */
  public record Example(String feature, String given, String expected) {}

  /**
   * Adds a concrete example for a feature.
   *
   * @param feature the feature being specified
   * @param given the input or precondition
   * @param expected the expected outcome
   */
  public void addExample(String feature, String given, String expected) {
    examples.computeIfAbsent(feature, _ -> new ArrayList<>())
        .add(new Example(feature, given, expected));
  }

  /**
   * Returns true if the feature has at least one concrete example.
   *
   * @param feature the feature name
   */
  public boolean hasExamples(String feature) {
    List<Example> list = examples.get(feature);
    return list != null && !list.isEmpty();
  }

  /**
   * Returns the number of examples for a feature.
   *
   * @param feature the feature name
   */
  public int exampleCount(String feature) {
    return examples.getOrDefault(feature, List.of()).size();
  }

  /**
   * Returns all examples for a feature.
   *
   * @param feature the feature name
   */
  public List<Example> getExamples(String feature) {
    return Collections.unmodifiableList(examples.getOrDefault(feature, List.of()));
  }

  /** Clears all specifications. */
  public void clear() {
    examples.clear();
  }
}
