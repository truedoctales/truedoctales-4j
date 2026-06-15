# The Spring Developer Who Wanted Real Wiring

## True Doc Tales in a Spring application

Spring developers usually describe behavior through application services, repositories, configuration, and test slices that are already managed by the Spring container. True Doc Tales does not replace that setup. It lets the story runner use it.

With the Spring integration, plots can be ordinary Spring beans. A plot class is still marked with `@Plot`, and its executable methods are still marked with `@Step`, but the object itself is created by Spring. That means constructor injection, test configuration, profiles, mocks, and application services remain part of the normal Spring test context.

At test startup, the Spring auto-configuration looks for beans annotated with `@Plot` and registers them in the True Doc Tales `PlotRegistry`. The Markdown story keeps referencing the plot and step names, while the implementation behind those steps can call real Spring-managed collaborators.

The result is the same True Doc Tales flow:

- the Markdown chapter explains the expected behavior
- the story runner parses the readable business story
- the plot registry connects story steps to Java methods
- Spring supplies the plot beans and their dependencies
- the executed story proves whether the documented behavior matches the application

For a Spring team, this keeps the documentation close to the way the application is already tested. The story remains readable for stakeholders, but the implementation runs inside the Spring context developers use every day.
