# How the binding works

## Create a Plot

A Plot bind logical Steps we later use the Plot name as prefix inside the stories.
At first, you need to create a java class with annotation @Plot annotation.

```java
@Plot("Greeting")
public class SamplePlot {
    // plot implementation
}
```



## The first Step

We can define a step using the @Step annotation.

```java
@Step("Say Hello") 
public void sayHello() {
    System.out.println("Hello, True Doc Tales!");
}
```

> **Greeting** Say Hello


## Calls with variable Inputs

```java
@Step("Greet ${name}") 
public void greet(String name) {
    System.out.println("Hello, " + name + "!");
}
```
> **Greeting** Greet *John*

Or greet multiple people.
You can still use the same plot.

> **Greeting** Greet *${name}*
> 
> | name  |
> |-------|
> | John  |
> | Jane  |
> | Doe   |
> 

### Check multiple results as table

> **Greeting** Greet *John* *3* times
> 
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |

> **Greeting** Greet *John* *3* times
> 
> | expected        |
> |-----------------|
> | 1. Hello, John! |
> | 2. Hello, John! |
> | 3. Hello, John! |