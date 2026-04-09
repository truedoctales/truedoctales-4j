# How the binding works

## Create a Plot

A Plot binds logical Steps. We later use the Plot name as a prefix inside the stories.
First, create a Java class with the `@Plot` annotation:

```java
@Plot("Greeting")
public class GreetingPlot {
    // step implementations go here
}
```

Register the plot so the framework can resolve its steps:

```java
plotRegistry.register(new GreetingPlot());
```

## The first Step

Define a step using the `@Step` annotation. The value must match what you write
in the story markdown:

```java
@Step("Say Hello")
public void sayHello() {
    System.out.println("Hello, True Doc Tales!");
}
```

In a story file you reference it with a blockquote that names the Plot in bold:

```markdown
> **Greeting** Say Hello
```

The framework finds `GreetingPlot.sayHello()` and executes it.

> **Greeting** Say Hello


## Calls with variable Inputs

Steps can accept inline variable values. Wrap values in `*...*` (italic) in the story:

```java
@Step("Greet ${name}")
public void greet(String name) {
    System.out.println("Hello, " + name + "!");
}
```

```markdown
> **Greeting** Greet *John*
```

> **Greeting** Greet *John*

Or greet multiple people using a data table. The same plot and step handle both cases:

```markdown
> **Greeting** Greet *${name}*
>
> | name  |
> |-------|
> | John  |
> | Jane  |
> | Doe   |
```

> **Greeting** Greet *${name}*
> 
> | name  |
> |-------|
> | John  |
> | Jane  |
> | Doe   |
> 

### Check multiple results as table

A step can also receive a table of *expected* outputs and assert all of them:

```java
@Step("Greet ${name} ${count} times")
public void greetMultipleTimes(String name, Integer count,
        List<Map<String, String>> expected) { ... }
```

```markdown
> **Greeting** Greet *John* *3* times
>
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |
```

> **Greeting** Greet *John* *3* times
> 
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |
