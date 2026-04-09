# How to Write a Book

This guide walks through every technical building block you need to turn a story file into
living, executable documentation. Each section explains **what** you write, **how** to implement
it in Java, and shows the markdown syntax side-by-side with a live step that actually runs.

---

## Scene: Create a Plot

A **Plot** is a Java class whose methods provide step implementations. The plot name becomes the
prefix you use when calling steps in your stories.

Annotate the class with `@Plot` and give it a name:

```java
@Plot("Greeting")
public class GreetingPlot {
    // step implementations go here
}
```

Register the plot with the execution registry so the framework can resolve its steps at runtime:

```java
plotRegistry.register(new GreetingPlot());
```

Once registered, every `@Step` method in `GreetingPlot` is available to any story under the
`Greeting` prefix.

> **Greeting** Say Hello

## Scene: Write a Simple Step

A **Step** is a Java method annotated with `@Step`. The annotation value is the phrase you write
in the story. The method contains the implementation — an assertion, a service call, or any
executable logic.

Define the step inside your plot class:

```java
@Step(value = "Say Hello", description = "Prints a simple greeting to the console.")
public void greet() {
    System.out.println("Hello, True Doc Tales!");
}
```

Reference it in a story using a blockquote with the plot name in **bold**:

```markdown
> **Greeting** Say Hello
```

The framework resolves the binding `Greeting` → `GreetingPlot` and phrase `Say Hello` →
`greet()`, then executes the method.

> **Greeting** Say Hello

## Scene: Pass a Variable to a Step

Steps can receive inline values from the story. Declare a placeholder with `${name}` in the
`@Step` phrase and mark the matching parameter with `@Variable`:

```java
@Step(value = "Greet ${name}", description = "Greets the person identified by name.")
public void greetSomeone(
        @Variable(value = "name", description = "Name of the person to greet") String name) {
    System.out.println("Hello, " + name + "!");
}
```

In the story file, replace the placeholder with the concrete value wrapped in `*...*` (italic):

```markdown
> **Greeting** Greet *John*
```

The framework extracts `John` from the italic span and passes it as the `name` argument.

> **Greeting** Greet *John*

## Scene: Run a Step over Multiple Rows

When you need to call the same step with many different inputs, add a markdown table directly
below the step call. The same Java method handles a single call and a table-driven call — no
additional code is required.

Use `*${name}*` as the placeholder in the story so the report renders each row's value:

```markdown
> **Greeting** Greet *${name}*
>
> | name  |
> |-------|
> | John  |
> | Jane  |
> | Doe   |
```

The framework iterates over all data rows and calls `greetSomeone` once per row.

> **Greeting** Greet *${name}*
>
> | name  |
> |-------|
> | John  |
> | Jane  |
> | Doe   |

## Scene: Assert Multiple Outputs with a Table

A step can also receive a table of *expected* values and validate that the implementation
produces exactly those outputs. Declare a `List<Map<String, String>>` parameter and annotate it
with `@Table`:

```java
@Step(
    value = "Greet ${name} ${count} times",
    description = "Greets the person the given number of times and verifies the output.")
public void greetSomeoneMultipleTimes(
        @Variable(value = "name", description = "Name of the person to greet") String name,
        @Variable(value = "count", description = "How many times to greet") Integer count,
        @Table(headers = {@Variable(value = "expected", description = "Expected greeting output")})
            List<Map<String, String>> expected) {
    List<String> list = IntStream.range(1, count + 1)
        .mapToObj(i -> "%s. Hello, %s!".formatted(i, name))
        .toList();
    Assertions.assertEquals(expected.stream().map(m -> m.get("expected")).toList(), list);
}
```

The story supplies the expected rows in the table:

```markdown
> **Greeting** Greet *John* *3* times
>
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |
```

The framework maps the table to the `expected` parameter and the step asserts the full list.

> **Greeting** Greet *John* *3* times
>
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |
