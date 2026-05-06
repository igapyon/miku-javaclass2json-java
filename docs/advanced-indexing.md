# Advanced Indexing

This document covers operational details that are useful after the basic CLI or
Maven plugin flow is working.

## Split Execution

For large-scale or multi-process runs, each process must write to a separate
output directory:

```text
.java-class-index-parts/
  part-001/
  part-002/
  part-003/
```

Do not write from multiple processes to the same output directory. Existing
class JSON files are overwritten and warnings are appended, so fresh output
directories are recommended for large runs.

For split execution, run the phases explicitly:

- `step1` collects binary class names.
- `step2` writes per-class JSON using one or more `step1` outputs as the
  global internal-class name set.
- `step3` writes the surrounding JSONL indexes and `index.json`.
- `step4` writes derived reverse indexes from the step3 JSONL output.

For large inputs, define package exclusions before indexing and pass them to
every phase that reads class files. Line-oriented artifacts such as
`classes.jsonl`, `sources.jsonl`, `symbols.jsonl`, `dependencies.jsonl`,
`method-calls.jsonl`, and method-call summaries otherwise grow with the input
classes, members, dependencies, and bytecode call surface.

Use `--exclude-package` for packages that should not participate in the index at
all, such as large shaded libraries. Use `--exclude-call-package` only when
classes and symbols should remain indexed but matching method-call edges should
be filtered.

Example:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step1 \
  --input target/classes-part-001 \
  --exclude-package 'org.objectweb.*' \
  --output .java-class-index-step1/part-001

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step1 \
  --input target/classes-part-002 \
  --exclude-package 'org.objectweb.*' \
  --output .java-class-index-step1/part-002

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step2 \
  --input target/classes-part-001 \
  --exclude-package 'org.objectweb.*' \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-001

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step2 \
  --input target/classes-part-002 \
  --exclude-package 'org.objectweb.*' \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-002

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step3 \
  --input target/classes-part-001 \
  --exclude-package 'org.objectweb.*' \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-001

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step3 \
  --input target/classes-part-002 \
  --exclude-package 'org.objectweb.*' \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-002

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step4 \
  --output .java-class-index-parts/part-001

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.1.jar index \
  --phase step4 \
  --output .java-class-index-parts/part-002
```

The default `index` command is equivalent to `--phase all` for small or
single-process runs.

`step4` reads `method-call-summary.jsonl` and writes
`method-call-reverse-summary.jsonl`. It does not read `.class` files again.

Package exclusion patterns use simple binary-name prefixes. A pattern ending in
`.*` matches that package prefix, so `'org.objectweb.*'` matches
`org.objectweb.asm.ClassReader`. Quote wildcard patterns in the shell.

## Duplicate Classes

If the same binary class name appears more than once, the later class JSON
write overwrites the earlier one.

This tool does not try to resolve that situation as a ClassLoader would. It
only appends an English warning line with a JST timestamp to `warnings.log`.
Duplicate aggregation belongs to a later merge or reporting step.

## Responsibility Boundary

`miku-javaclass2json` generates broad bytecode-derived artifacts:

```text
bytecode -> class JSON / basic JSONL / method-calls.jsonl
```

Graph generation, artifact merging, duplicate aggregation, and advanced search
indexes are intentionally outside this CLI. They should be built by another
CLI or tool from the generated JSON / JSONL artifacts.
