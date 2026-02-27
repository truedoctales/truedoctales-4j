# truedoctales-maven-plugin

Maven plugin that generates enriched Markdown reports from True Doc Tales test execution results.

## How it works

1. **`mvn verify`** — runs tests; `JsonStoryListener` writes per-story execution JSON to `target/truedoctales-report/`
2. **`mvn site`** — triggers the `report` goal in the `pre-site` phase; `BookReportGenerator` merges the JSON with the original book Markdown and writes enriched copies to `target/truedoctales-markdown/`

## Usage

Add the plugin to the module that runs your True Doc Tales tests:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>dev.truedoctales</groupId>
      <artifactId>truedoctales-maven-plugin</artifactId>
      <version>${project.version}</version>
      <executions>
        <execution>
          <goals>
            <goal>report</goal>
          </goals>
          <phase>pre-site</phase>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

## Running

```bash
# Step 1: run tests to generate execution JSON
mvn verify

# Step 2: generate the enriched Markdown report
mvn site -pl truedoctales-sample-jupiter -am
```

After `mvn site`, the enriched Markdown will be in:

```
truedoctales-sample-jupiter/target/truedoctales-markdown/
```

## Configuration

| Parameter              | Default                                        | Description                                |
|------------------------|------------------------------------------------|--------------------------------------------|
| `truedoctales.bookDirectory`      | `${project.basedir}/../fairy-doc-tales`        | Path to the original book Markdown directory |
| `truedoctales.executionDirectory` | `${project.build.directory}/truedoctales-report` | Directory with JSON execution results       |
| `truedoctales.outputDirectory`    | `${project.build.directory}/truedoctales-markdown` | Output directory for enriched Markdown     |

## Example output

Original Markdown:
```markdown
> **Quest** Create quest
```

Enriched Markdown:
```markdown
> **Quest** Create quest ✅
```

Status badges: ✅ SUCCESS, ❌ FAILURE, ⚠️ ERROR, ⏭️ SKIPPED
