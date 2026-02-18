package dev.truedoctales.sample.jupiter.plots;

import dev.truedoctales.api.annotations.Plot;
import dev.truedoctales.api.annotations.Step;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;

@Plot("Greeting")
public class GreetingPlot {

  @Step("Say Hello")
  public void greet() {
    System.out.println("Hello, True Doc Tales!");
  }

  @Step("Greet ${name}")
  public void greetSomeone(String name) {
    System.out.println("Hello, " + name + "!");
  }

  @Step("Greet ${name} ${count} times")
  public void greetSomeoneMultipleTimes(
      String name, Integer count, List<Map<String, String>> expected) {
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
