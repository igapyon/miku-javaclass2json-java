# Java-First Decision

This repository intentionally creates a miku Java application without a
Node.js / TypeScript upstream.

## Decision

`miku-javaclass2json-java` treats the Java implementation as the canonical
implementation.

The usual miku Java application rule prefers straight conversion from a
suffixless Node.js / TypeScript main application. This project differs because
its core input and semantics are `.class`, `.jar`, classpath, descriptors, and
Java build outputs.

## Boundary

The product has two execution surfaces: CLI and Maven plugin.

The product core lives inside the CLI runtime module,
`miku-javaclass2json`. It is not a third module at this stage.

Entrypoints call the core:

- CLI: `jp.igapyon.mikujavaclass2json.cli.MikuJavaclass2jsonCli`
- Maven plugin: `jp.igapyon.mikujavaclass2json.mavenplugin.MikuJavaclass2jsonMojo`

Future Agent Skills or MCP surfaces should consume generated artifacts or call
the core API. They should not become the semantic center.

## Initial MVP

The first implementation reads `.class`, classes directories, and `.jar` files
with a small JDK-only class file parser. It emits:

- `index.json`
- `classes.jsonl`
- `symbols.jsonl`
- `dependencies.jsonl`
- `method-calls.jsonl`
- `sources.jsonl`
- `warnings.log`
- per-class JSON files under `classes/`

When duplicate binary class names appear, the later class JSON write overwrites
the earlier one. The tool only records an English warning line with a JST
timestamp because large inputs may be split across multiple processes and global
duplicate aggregation belongs to a later indexing or merge step.

Jandex or ASM can be added later when annotation values, generic signatures, or
bytecode instruction-level calls become required.
