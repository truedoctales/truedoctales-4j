# Chapter 1: Framework Basics - Technical Reference

This chapter serves as a technical reference guide for the True Doc Tales framework. It demonstrates all available operations in a step-by-step, progressive manner without narrative embellishment.

## Purpose

Unlike later chapters that use storytelling to demonstrate features, this chapter is deliberately dry and technical. It's designed to:

- Show **what** the framework can do
- Demonstrate **how** to use each feature
- Provide reference examples for test writers
- Progress from simple to complex operations
- **Demonstrate the binding mechanism** that connects markdown to Java code

## Technical Foundation: The Binding Mechanism

The True Doc Tales framework transforms markdown stories into living documentation through a binding mechanism that connects narrative steps to executable Java code.

### How Bindings Work

The framework uses two key annotations to create bindings:

1. **`@Plot("Plot Name")`** - Marks a Java class as providing method bindings
   - The plot name identifies the source of step implementations
   - Plots are registered to make their bindings available
   - Example: `@Plot("Greeting")`

2. **`@Step("step description")`** - Marks a Java method as executable from markdown
   - The description matches what appears in story files
   - Method parameters map to table columns in markdown
   - Example: `@Step("Say Hello")`

### Connecting Stories to Code

In markdown stories, you reference plot bindings using the `@Step` directive:

```markdown
@Step(Greeting) Greet ${name}

| name  |
|-------|
| John  |
| Jane  |
```

This connects to a Java method:

```java
@Plot("Greeting")
public class GreetingPlot {
    @Step("Greet ${name}")
    void greetSomeone(String name) {
        System.out.println("Hello, " + name + "!");
    }
}
```

### The Binding Flow

1. Plot classes are registered: `registry.register(new GreetingPlot())`
2. Framework scans for `@Step` methods and creates bindings
3. Markdown `@Step(Plot Name)` references find the corresponding Java method
4. Table data is mapped to method parameters
5. The method executes, making the story come alive

This mechanism is what transforms static markdown into executable, verifiable documentation.

## Coverage

This chapter covers three main entity types (TimeEra, Lifeform, Task) and their operations:

1. **Basic CRUD Operations**: Create, validate, count, and delete entities
2. **Simple Queries**: Find by ID, check existence, count records
3. **Advanced Queries**: Skill-based searches, filtering, team assembly

## Structure

Each story builds on previous ones:
- Start with simple create and validate operations
- Progress to queries and filters
- End with complex multi-criteria searches

## Usage

When writing your own tests, refer to this chapter to:
- See the exact step syntax for each operation
- Understand table data formats
- Learn parameter patterns
- Find examples of progressive feature usage

*This is a technical reference. For storytelling examples, see Chapters 2 and 3.*
