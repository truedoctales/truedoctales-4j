# How to Write a Book

This guide walks through every technical building block you need to turn a story file into
living, executable documentation. Each section explains **what** a concept is, **why** it
exists, **how** to implement it in Java, and **how** to reference it in markdown. At the end
of each section a live step runs the exact code that was just described, so you can see the
result directly in the rendered output.

---

## Scene: Create a Plot

The first thing you need before writing any story is a **Plot**. A Plot is the bridge between
your markdown narrative and your Java implementation. It is a plain Java class annotated with
`@Plot`, and the string you pass to that annotation — the *plot name* — is the identifier
your stories will use to call its steps. Think of it as a named container: all step methods
inside the class are grouped under that name, so the framework knows exactly where to look
when it encounters a step call in a story file.

The Java code below shows the minimal declaration. The `@Plot("Greeting")` annotation
registers the class under the name `Greeting`. The class body can stay empty for now — steps
are added in the next section.

```java
@Plot("Greeting")
public class GreetingPlot {
    // step implementations go here
}
```

Declaring the class is not enough on its own. The framework discovers plots only if they are
explicitly registered with the `SimplePlotRegistry` before a story runs. The call below hands
an instance of `GreetingPlot` to the registry. From that point on, any step call in any story
that starts with `**Greeting**` will be dispatched to a method in this class.

```java
plotRegistry.register(new GreetingPlot());
```

Once registered, every `@Step` method in `GreetingPlot` is available to any story under the
`Greeting` prefix. The following step call already works because `GreetingPlot` is registered
and its `greet()` method (defined in the next section) is ready to run.

> **Greeting** Say Hello

---

## Scene: Write a Simple Step

A **Step** is a Java method that the framework executes when it encounters a matching phrase
in a story file. You declare the step by adding the `@Step` annotation to the method. The
`value` attribute is the phrase the framework matches against — it must match the text that
follows the plot name in the markdown story, character for character. The optional
`description` attribute documents what the step does and appears in the generated report.

The method body contains whatever logic is needed: printing output, calling a service,
running an assertion, or any other executable code. There are no constraints on what the
method may do, as long as it is public and void (or returns a value the framework can use
for assertions in later patterns).

```java
@Step(value = "Say Hello", description = "Prints a simple greeting to the console.")
public void greet() {
    System.out.println("Hello, True Doc Tales!");
}
```

To call this step from a story, write a markdown blockquote (`>`) that starts with the plot
name wrapped in `**bold**`, followed by a space and then the exact step phrase. The bold
plot name tells the framework which registered plot to look in, and the phrase after it
selects the matching `@Step` method.

```markdown
> **Greeting** Say Hello
```

When the framework processes this line it resolves the binding: `Greeting` maps to
`GreetingPlot`, and `Say Hello` maps to the `greet()` method. The method is invoked, its
output is captured, and the result is embedded in the report. The live call below
demonstrates this — the text you see in the rendered output comes directly from the
`System.out.println` inside the method.

> **Greeting** Say Hello

---

## Scene: Pass a Variable to a Step

Hard-coded phrases are useful for fixed actions, but most steps need to work with different
input values. The framework supports **inline variables**: you declare a `${placeholder}` in
the `@Step` phrase and annotate the corresponding Java parameter with `@Variable`. At
runtime the framework extracts the value from the story, converts it to the declared Java
type, and injects it into the method call.

The Java side declares `${name}` in the step phrase and maps it to a `String name` parameter.
The `@Variable` annotation connects the two: its `value` must match the placeholder name
exactly. You can have as many variables as you need — each one adds a placeholder in the
phrase and a corresponding annotated parameter.

```java
@Step(value = "Greet ${name}", description = "Greets the person identified by name.")
public void greetSomeone(
        @Variable(value = "name", description = "Name of the person to greet") String name) {
    System.out.println("Hello, " + name + "!");
}
```

In the story you supply the concrete value by writing it in `*italic*` at the position where
the placeholder appears. The framework identifies each italic span as a variable value, strips
the formatting, and passes the plain text to the matching parameter. The order of the italic
spans in the story line maps to the order of the `@Variable` parameters in the method
signature.

```markdown
> **Greeting** Greet *John*
```

Below the framework extracts `John` from the italic span, converts it to a `String`, and
passes it to `name`. The method then prints `Hello, John!`.

> **Greeting** Greet *John*

---

## Scene: Run a Step over Multiple Rows

Writing one story line per value quickly becomes repetitive. Instead you can attach a
**data table** directly below a step call. The framework reads every data row from the table
and calls the same step method once per row, substituting the row values into the
placeholders on each invocation. Crucially, you do not need to change the Java method at all
— the same method that handles a single inline value also handles a table-driven call
automatically.

In the markdown story, write the step call with `*${name}*` as the placeholder — using the
variable name rather than a hard-coded value signals to the report generator that each row
should be rendered with its own substituted value. The table that follows must have a header
column whose name matches the `@Variable` placeholder exactly. Each subsequent row provides
one concrete value.

```markdown
> **Greeting** Greet *${name}*
>
> | name  |
> |-------|
> | John  |
> | Jane  |
> | Doe   |
```

The framework iterates over all three rows and calls `greetSomeone` once per row, passing
`John`, `Jane`, and `Doe` in turn. Each call is recorded as a separate execution in the
report, so the output below shows three independent results.

> **Greeting** Greet *${name}*
>
> | name  |
> |-------|
> | John  |
> | Jane  |
> | Doe   |

---

## Scene: Assert Multiple Outputs with a Table

So far the tables provided *input* to the step. Tables can also carry *expected output*: the
step reads both the inputs and the expected values, runs the business logic, and then
asserts that the actual output matches the expected rows. This pattern turns a story file
into a verifiable specification — the markdown table is both human-readable documentation
and the source of truth for the assertion.

On the Java side you add a `List<Map<String, String>>` parameter to the method and annotate
it with `@Table`. Each map in the list represents one expected row, and its keys correspond
to the column headers declared in the annotation. The method then computes the actual output
and uses `Assertions.assertEquals` to verify it against the expected values. The `@Variable`
parameters for `name` and `count` work exactly as before — they carry the inline values from
the story line.

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

In the story the inline values `*John*` and `*3*` are passed as `name` and `count`. The
table below the step call carries the expected output in its `expected` column. Each row
describes what the step should produce for one iteration. The column header name must match
the `value` of the `@Variable` declared inside the `@Table` annotation.

```markdown
> **Greeting** Greet *John* *3* times
>
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |
```

The framework passes the three-row table to `expected`, the method generates the greetings,
and `Assertions.assertEquals` confirms they match. If any row differs, the test fails with
a clear diff showing which expected value was not produced. The live call below runs the
assertion — if it renders successfully, the output matched.

> **Greeting** Greet *John* *3* times
>
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |
