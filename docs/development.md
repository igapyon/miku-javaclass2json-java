# Development Guide

## Product Shape

This repository has two execution surfaces:

- CLI: `miku-javaclass2json`
- Maven plugin: `miku-javaclass2json-maven-plugin`

The core API is kept inside the CLI runtime module. It is not split into a
third standalone module at this stage.

This project intentionally differs from the usual miku Java application rule.
There is no Node.js / TypeScript upstream repository for this product. The Java
implementation is the canonical implementation because the product semantics
are centered on Java bytecode and Java build outputs.

See [Java-first decision](java-first-decision.md) for the architectural
decision record.

## Repository Operation

- Maven is the build tool.
- Java source and target compatibility are `1.8`.
- `mvn test` is the primary verification command.
- `workplace/` is a local scratch area; only `workplace/.gitkeep` is tracked.
- `.java-class-index/`, Maven `target/`, and `.DS_Store` are ignored.
- The design memo is in [Java class index design memo](java-class-index-design.md).

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

## Sister Reference

Initial layout was checked against local sister repositories:

- `/Users/igapyon/Documents/git/miku-indexgen-java`
- `/Users/igapyon/Documents/git/miku-xlsx2md-java`

Those references informed the parent POM, runtime jar module, Maven plugin
module, CLI delegation style, and `workplace/` convention. The upstream mapping
documents normally used for straight conversion are intentionally not primary
documents here because this product has no Node upstream.
