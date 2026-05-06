# miku-javaclass2json-java

`miku-javaclass2json-java` is a Java-first miku tool that indexes `.class`
files and `.jar` files into JSON / JSONL artifacts for agent workflows.

This project intentionally differs from the usual miku Java application rule.
There is no Node.js / TypeScript upstream repository for this product. The Java
implementation is the canonical implementation because the product semantics
are centered on Java bytecode and Java build outputs.

## Execution Surfaces

This repository has two execution surfaces:

- CLI: `miku-javaclass2json`
- Maven plugin: `miku-javaclass2json-maven-plugin`

The core API is kept inside the CLI runtime module. It is not split into a
third standalone module at this stage.

## CLI

```sh
mvn package
java -jar miku-javaclass2json/target/miku-javaclass2json-0.1.0-SNAPSHOT.jar index \
  --input target/classes \
  --output .java-class-index
```

The generated layout is:

```text
.java-class-index/
  index.json
  symbols.jsonl
  dependencies.jsonl
  classes/
```

## Maven Plugin

The first goal is explicit execution only:

```sh
mvn jp.igapyon:miku-javaclass2json-maven-plugin:0.1.0-SNAPSHOT:index
```

## Repository Operation

- Maven is the build tool.
- Java source and target compatibility are `1.8`.
- `mvn test` is the primary verification command.
- `workplace/` is a local scratch area; only `workplace/.gitkeep` is tracked.
- `.java-class-index/`, Maven `target/`, and `.DS_Store` are ignored.
- The design memo is in `docs/java-class-index-design.md`.

## Sister Reference

Initial layout was checked against local sister repositories:

- `/Users/igapyon/Documents/git/miku-indexgen-java`
- `/Users/igapyon/Documents/git/miku-xlsx2md-java`

Those references informed the parent POM, runtime jar module, Maven plugin
module, CLI delegation style, and `workplace/` convention. The upstream mapping
documents normally used for straight conversion are intentionally not primary
documents here because this product has no Node upstream.
