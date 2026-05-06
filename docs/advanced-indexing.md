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

Example:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --phase step1 \
  --input target/classes-part-001 \
  --output .java-class-index-step1/part-001

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --phase step1 \
  --input target/classes-part-002 \
  --output .java-class-index-step1/part-002

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --phase step2 \
  --input target/classes-part-001 \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-001

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --phase step2 \
  --input target/classes-part-002 \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-002

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --phase step3 \
  --input target/classes-part-001 \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-001

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --phase step3 \
  --input target/classes-part-002 \
  --step1-output .java-class-index-step1/part-001 \
  --step1-output .java-class-index-step1/part-002 \
  --output .java-class-index-parts/part-002
```

The default `index` command is equivalent to `--phase all` for small or
single-process runs.

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
