package dev.truedoctales.execution.execute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Extracts variable values from scenario names using pattern matching.
///
/// Supports ${variable} placeholders in patterns.
public class VariableExtractor {

  private static final Logger LOGGER = Logger.getLogger(VariableExtractor.class.getName());
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");
  private static final int FIRST_GROUP_INDEX = 1;

  /// Checks if a scenario pattern matches a scenario value.
  ///
  /// @param scenarioPattern the pattern with optional ${variable} placeholders
  /// @param scenarioValue the actual scenario value
  /// @return true if the pattern matches the value
  public boolean matches(String scenarioPattern, String scenarioValue) {
    List<String> variableNames = findVariableNames(scenarioPattern);

    if (variableNames.isEmpty()) {
      return scenarioPattern.equals(scenarioValue);
    }

    String regexPattern = buildRegexPattern(scenarioPattern);
    try {
      Matcher scenarioMatcher = buildMatcher(regexPattern, scenarioValue);
      return scenarioMatcher.matches();
    } catch (java.util.regex.PatternSyntaxException e) {
      LOGGER.warning(
          "Invalid regex pattern '"
              + regexPattern
              + "' for scenario '"
              + scenarioValue
              + "': "
              + e.getMessage());
      return false;
    }
  }

  /// Extracts variables from a scenario title using a pattern.
  ///
  /// @param pattern the pattern with ${variable} placeholders
  /// @param scenarioName the actual scenario title
  /// @return map of variable names to values, or null if no match
  Map<String, String> extractVariables(String pattern, String scenarioName) {
    List<String> variableNames = findVariableNames(pattern);

    if (variableNames.isEmpty()) {
      return null;
    }

    String regexPattern = buildRegexPattern(pattern);
    return matchAndExtract(regexPattern, scenarioName, variableNames);
  }

  /// Finds all variable names in a pattern.
  ///
  /// @param pattern the pattern to search
  /// @return list of variable names
  public List<String> findVariableNames(String pattern) {
    List<String> variableNames = new ArrayList<>();
    Matcher matcher = VARIABLE_PATTERN.matcher(pattern);

    while (matcher.find()) {
      variableNames.add(matcher.group(1));
    }

    return variableNames;
  }

  private String buildRegexPattern(String pattern) {
    StringBuilder regexBuilder = new StringBuilder();
    Matcher matcher = VARIABLE_PATTERN.matcher(pattern);
    int lastEnd = 0;

    while (matcher.find()) {
      appendLiteralPart(regexBuilder, pattern, lastEnd, matcher.start());
      regexBuilder.append("(.+?)");
      lastEnd = matcher.end();
    }

    appendRemainingPart(regexBuilder, pattern, lastEnd);
    return regexBuilder.toString();
  }

  private void appendLiteralPart(StringBuilder builder, String pattern, int start, int end) {
    String literalPart = pattern.substring(start, end);
    builder.append(Pattern.quote(literalPart));
  }

  private void appendRemainingPart(StringBuilder builder, String pattern, int lastEnd) {
    if (lastEnd < pattern.length()) {
      builder.append(Pattern.quote(pattern.substring(lastEnd)));
    }
  }

  private Map<String, String> matchAndExtract(
      String regexPattern, String scenarioName, List<String> variableNames) {
    try {
      Matcher scenarioMatcher = buildMatcher(regexPattern, scenarioName);

      if (scenarioMatcher.matches()) {
        return extractMatchedValues(scenarioMatcher, variableNames);
      }
    } catch (java.util.regex.PatternSyntaxException e) {
      LOGGER.warning(
          "Invalid regex pattern '"
              + regexPattern
              + "' for scenario '"
              + scenarioName
              + "': "
              + e.getMessage());
    }

    return null;
  }

  private static Matcher buildMatcher(String regexPattern, String scenarioName) {
    Pattern compiledPattern = Pattern.compile(regexPattern);
    return compiledPattern.matcher(scenarioName);
  }

  private Map<String, String> extractMatchedValues(Matcher matcher, List<String> variableNames) {
    Map<String, String> variables = new HashMap<>();

    for (int i = 0; i < variableNames.size(); i++) {
      String value = matcher.group(i + FIRST_GROUP_INDEX).trim();
      variables.put(variableNames.get(i), value);
      LOGGER.fine("Extracted variable: " + variableNames.get(i) + " = " + value);
    }

    return variables;
  }
}
