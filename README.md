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
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.0.jar index \
  --input target/classes \
  --output .java-class-index
```

The generated layout is:

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

If the same binary class name appears more than once, the later class JSON write
overwrites the earlier one. This tool does not try to resolve that situation as
a ClassLoader would. It only appends an English warning line with a JST
timestamp to `warnings.log`.

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

For split execution, run the phases explicitly. `step1` collects only class
names. `step2` writes per-class JSON using one or more `step1` outputs as the
global internal-class name set. `step3` writes the surrounding JSONL indexes and
`index.json`:

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

Graph generation, artifact merging, duplicate aggregation, and advanced search
indexes are intentionally outside this CLI. They should be built by another CLI
from the generated JSON / JSONL artifacts.

Per-class JSON files include method-level `calls[]`, while `method-calls.jsonl`
keeps the same call surface available for cross-class streaming search.
`method-call-summary.jsonl` is the agent-friendly call index: it aggregates
repeated call edges, adds `targetKind`, and suppresses `external-platform` and
`external-api` calls by default.

The generated artifacts are primarily for generative AI / agent consumption.
JVM descriptors are intentionally emitted as-is, and class JSON favors compact
output over human-oriented pretty formatting.

Common searches:

```sh
rg '"binaryName":"jp.example.Foo"' .java-class-index/classes.jsonl
rg '"fromClass":"jp.example.Foo"' .java-class-index/method-calls.jsonl
rg '"toMethod":"println"' .java-class-index/method-calls.jsonl
rg '"name":"run"' .java-class-index/classes/jp/example/Foo.json
```

`invokedynamic` is recorded as a bytecode-level call surface. This CLI does not
expand lambda bodies, string concatenation recipes, or bootstrap method
semantics into higher-level Java concepts.

## Maven Plugin

The first goal is explicit execution only:

```sh
mvn jp.igapyon:miku-javaclass2json-maven-plugin:0.5.0:index
```

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
