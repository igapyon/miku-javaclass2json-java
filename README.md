# miku-javaclass2json-java

`miku-javaclass2json-java` indexes Java `.class` files, classes
directories, and `.jar` files into JSON / JSONL artifacts.

The generated index is intended for agent workflows: it lets an agent inspect
compiled Java structure, symbols, dependencies, and method-call surfaces
without reading every source file first.

## What It Generates

Running the indexer creates a directory like this:

```text
.java-class-index/
  index.json
  classes.jsonl
  symbols.jsonl
  dependencies.jsonl
  method-calls.jsonl
  method-call-summary.jsonl
  sources.jsonl
  warnings.log
  classes/
```

Use the JSONL files for broad searches, then open the matching per-class JSON
under `classes/` when detailed class information is needed.

Common searches:

```sh
rg '"binaryName":"jp.example.Foo"' .java-class-index/classes.jsonl
rg '"fromClass":"jp.example.Foo"' .java-class-index/method-calls.jsonl
rg '"toMethod":"println"' .java-class-index/method-calls.jsonl
rg '"name":"run"' .java-class-index/classes/jp/example/Foo.json
```

## Build

```sh
mvn package
```

The runtime CLI jar is created at:

```text
miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar
```

## CLI Usage

Index compiled classes:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --input target/classes \
  --output .java-class-index
```

Index a jar:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --input target/example.jar \
  --output .java-class-index
```

If `--output` is omitted, the CLI writes to `.java-class-index`.

## Maven Plugin Usage

The Maven plugin is currently intended for explicit execution:

```sh
mvn jp.igapyon:miku-javaclass2json-maven-plugin:0.5.0:index
```

Plugin parameters:

```text
miku-javaclass2json.classesDirectory
miku-javaclass2json.outputDirectory
miku-javaclass2json.skip
```

By default, it indexes `${project.build.outputDirectory}` and writes
`${project.basedir}/.java-class-index`.

## Output Notes

- JVM descriptors are emitted as JVM descriptors.
- Per-class JSON files include method-level `calls[]`.
- `method-calls.jsonl` is useful for streaming call searches.
- `method-call-summary.jsonl` aggregates repeated call edges and suppresses
  platform/API calls by default.
- `invokedynamic` is recorded as a bytecode-level call surface. Lambda bodies,
  string concatenation recipes, and bootstrap method semantics are not expanded
  into higher-level Java concepts.

## Verification

Run unit tests:

```sh
mvn test
```

Build the runtime jar and index that jar with itself:

```sh
sh scripts/smoke-self-jar.sh
```

The smoke script writes generated artifacts under
`workplace/smoke-self-jar-index/`.

## Documentation

- [Advanced indexing](docs/advanced-indexing.md)
- [Development guide](docs/development.md)
- [Java-first decision](docs/java-first-decision.md)
- [Java class index design memo](docs/java-class-index-design.md)
