package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import dev.truedoctales.api.annotations.Table;
import dev.truedoctales.api.annotations.Variable;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;

@Plot("Greeting")
public class GreetingPlot {

  @Step(value = "Say Hello", description = "Prints a simple greeting to the console.")
  public void greet() {
    System.out.println("Hello, True Doc Tales!");
  }

  @Step(value = "Greet ${name}", description = "Greets the person identified by name.")
  public void greetSomeone(
      @Variable(value = "name", description = "Name of the person to greet") String name) {
    System.out.println("Hello, " + name + "!");
  }

  @Step(
      value = "Greet ${name} ${count} times",
      description = "Greets the person the given number of times and verifies the output.")
  public void greetSomeoneMultipleTimes(
      @Variable(value = "name", description = "Name of the person to greet") String name,
      @Variable(value = "count", description = "How many times to greet") Integer count,
      @Table(headers = {@Variable(value = "expected", description = "Expected greeting output")})
          List<Map<String, String>> expected) {
    List<String> list =
        IntStream.range(1, count + 1)
            .boxed()
            .map(i -> "%s. Hello, %s!".formatted(i, name))
            .toList();

    System.out.println(list);
    List<String> expectedValues = expected.stream().map(m -> m.get("expected")).toList();
    Assertions.assertEquals(expectedValues, list);
  }
}
