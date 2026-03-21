package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Variable;
import dev.truedoctales.sample.domain.SpecificationService;
import org.junit.jupiter.api.Assertions;

/**
 * Executable specification operations — the heart of TrueDocTales.
 *
 * <p>A feature without a concrete, verifiable example is not a specification.
 * It is a wish. This plot makes the difference visible and executable.
 *
 * <p>Use {@code Has no examples} in failure stories to show missing specifications.
 * Use {@code Add example} + {@code Has examples} in success stories to show the fix.
 */
@Plot("Specification")
public class SpecificationPlot {

  private final SpecificationService specificationService;

  public SpecificationPlot(SpecificationService specificationService) {
    this.specificationService = specificationService;
  }

  @Step(value = "Add example",
      description = "Adds a concrete, verifiable example to a feature specification. "
          + "Given a precondition, when something happens, the expected outcome is defined.")
  public void addExample(
      @Variable(value = "feature", description = "The feature being specified") String feature,
      @Variable(value = "given", description = "The precondition or input") String given,
      @Variable(value = "expected", description = "The expected outcome") String expected) {
    specificationService.addExample(feature, given, expected);
  }

  @Step(value = "Has examples",
      description = "Asserts that the feature has at least one concrete example. "
          + "A feature with examples can be proven to exist.")
  public void hasExamples(
      @Variable(value = "feature", description = "The feature to check") String feature) {
    Assertions.assertTrue(specificationService.hasExamples(feature),
        "Feature '" + feature + "' has no examples — it cannot be proven to exist");
  }

  @Step(value = "Has no examples",
      description = "Asserts that the feature has no concrete examples. "
          + "This is the failure mode: a requirement written without a single verifiable case.")
  public void hasNoExamples(
      @Variable(value = "feature", description = "The feature to check") String feature) {
    Assertions.assertFalse(specificationService.hasExamples(feature),
        "Feature '" + feature + "' has examples — but it should not at this point in the story");
  }

  @Step(value = "Example count is",
      description = "Asserts the exact number of examples registered for a feature.")
  public void exampleCountIs(
      @Variable(value = "feature", description = "The feature to check") String feature,
      @Variable(value = "count", description = "Expected number of examples") Integer count) {
    Assertions.assertEquals(count, specificationService.exampleCount(feature),
        "Feature '" + feature + "' should have " + count + " example(s)");
  }
}
