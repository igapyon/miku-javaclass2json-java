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
  method-call-reverse-summary.jsonl
  sources.jsonl
  warnings.log
  cls/
```

Use the JSONL files for broad searches, then open the matching per-class JSON
under `cls/` when detailed class information is needed. Nested and anonymous
classes with `$` in their binary names are stored inside the top-level class
JSON as `nestedClasses[]`.

Common searches:

```sh
rg '"binaryName":"jp.example.Foo"' .java-class-index/classes.jsonl
rg '"fromClass":"jp.example.Foo"' .java-class-index/method-calls.jsonl
rg '"toClass":"jp.example.Foo"' .java-class-index/method-call-reverse-summary.jsonl
rg '"toMethod":"println"' .java-class-index/method-calls.jsonl
rg '"name":"run"' .java-class-index/cls/jp/example/Foo.json
```

## Build

```sh
mvn package
```

The runtime CLI jar is created at:

```text
miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar
```

The CLI source jar is also created at:

```text
miku-javaclass2json/target/miku-javaclass2json-0.5.4-sources.jar
```

## CLI Usage

Print the CLI version:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar --version
```

Run the full index pipeline for compiled classes:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar index \
  --verbose \
  --input target/classes \
  --output .java-class-index
```

Index a jar:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar index \
  --input target/example.jar \
  --output .java-class-index
```

If `--output` is omitted, the CLI writes to `.java-class-index`.

### CLI Arguments

```text
miku-javaclass2json index [options]
```

| Option | Required | Description |
| --- | --- | --- |
| `--input <path>` | Required except `--phase step4` | Input class directory, single `.class` file, or `.jar` file. When a directory is given, nested `.class` files and `.jar` files are read. |
| `--output <dir>` | No | Output directory. Default: `.java-class-index`. |
| `--phase <all|step1|step2|step3|step4>` | No | Selects the pipeline phase. Default: `all`. |
| `--step1-output <dir|binary-names.jsonl>` | No | Reads the binary-name set created by `step1`. If omitted for `step2` or `step3`, the CLI reads `binary-names.jsonl` from `--output`. May be specified multiple times for split inputs. |
| `--exclude-package <binary.package.*>` | No | Removes matching classes from the index. May be specified multiple times. |
| `--exclude-call-package <binary.package.*>` | No | Keeps matching classes indexed, but removes method-call edges where `fromClass` or `toClass` matches. May be specified multiple times. |
| `--verbose` | No | Prints progress messages to stderr, including the active step. May be placed before or after `index`. |

Wildcard package patterns should be quoted in shells:

```sh
--exclude-package 'org.objectweb.*'
```

Repeat options to specify multiple packages:

```sh
--exclude-package 'org.objectweb.*' \
--exclude-package 'com.fasterxml.*' \
--exclude-call-package 'java.*'
```

### Phases

Without `--phase`, the CLI runs the full single-process pipeline:

```text
all = collect class names, write per-class JSON, write JSONL indexes,
      then write method-call-reverse-summary.jsonl
```

Use split phases when the output is too large, or when you only need part of
the generated index:

| Phase | Reads | Writes |
| --- | --- | --- |
| `step1` | `--input` | `binary-names.jsonl` |
| `step2` | `--input`, `--step1-output` | `cls/<topLevelBinaryName>.json` with `$` classes embedded as `nestedClasses[]` |
| `step3` | `--input`, `--step1-output` | `classes.jsonl`, `symbols.jsonl`, `dependencies.jsonl`, `method-calls.jsonl`, `method-call-summary.jsonl`, `sources.jsonl`, `warnings.log`, `index.json` |
| `step4` | `method-call-summary.jsonl` under `--output` | `method-call-reverse-summary.jsonl`, updated `index.json` |

For `step2` and `step3`, `--step1-output` is optional when `step1` wrote
`binary-names.jsonl` into the same `--output` directory. Specify
`--step1-output` when step1 output lives somewhere else or when multiple step1
outputs should be combined.

If you only want per-class JSON files under `cls/`, run `step1` and
`step2` only:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar index \
  --phase step1 \
  --input target/classes \
  --output .java-class-index-step1

java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar index \
  --phase step2 \
  --input target/classes \
  --output .java-class-index-step1
```

Exclude packages that should not appear in line-oriented indexes:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar index \
  --input target/example.jar \
  --output .java-class-index \
  --exclude-package 'org.objectweb.*'
```

`--exclude-package` removes matching classes from `classes.jsonl`,
`sources.jsonl`, `symbols.jsonl`, `dependencies.jsonl`, `method-calls.jsonl`,
method-call summaries, and per-class JSON dependencies/calls. This option is
intended for packages that should not participate in the index at all, such as
large shaded libraries.

For large systems, pass the same `--exclude-package` values to every phase that
reads class files: `step1`, `step2`, and `step3`. Use
`--exclude-call-package` only when the classes and symbols should remain indexed
but matching method-call edges should be filtered.

For split execution, reverse summary generation is separated as `step4`:

```sh
java -jar miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar index \
  --phase step4 \
  --output .java-class-index
```

## Maven Plugin Usage

The Maven plugin is currently intended for explicit execution:

```sh
mvn jp.igapyon:miku-javaclass2json-maven-plugin:0.5.4:index
```

The only plugin goal is `index`. The goal should be specified explicitly when
running the plugin from the command line.

Plugin parameters:

| Property | Default | Description |
| --- | --- | --- |
| `miku-javaclass2json.classesDirectory` | `${project.build.outputDirectory}` | Directory of compiled classes to index. |
| `miku-javaclass2json.outputDirectory` | `${project.basedir}/.java-class-index` | Output index directory. |
| `miku-javaclass2json.excludePackages` | empty | Packages removed from the index. |
| `miku-javaclass2json.excludeCallPackages` | empty | Packages kept in the index but removed from method-call edges. |
| `miku-javaclass2json.verbose` | `false` | Prints progress messages through the Maven log. |
| `miku-javaclass2json.skip` | `false` | Skips plugin execution. |

By default, it indexes `${project.build.outputDirectory}` and writes
`${project.basedir}/.java-class-index`.

Enable plugin progress output:

```sh
mvn jp.igapyon:miku-javaclass2json-maven-plugin:0.5.4:index \
  -Dmiku-javaclass2json.verbose=true
```

## Output Notes

- JVM descriptors are emitted as JVM descriptors.
- Line-oriented indexes grow with classes, members, dependencies, or bytecode
  calls. For large systems, define package exclusions before indexing; shaded
  libraries and generated implementation packages are common candidates.
- Per-class JSON files include method-level `calls[]`.
- Nested and anonymous `$` classes are embedded in their top-level class JSON
  as `nestedClasses[]` to keep generated paths shorter.
- `method-calls.jsonl` is useful for streaming call searches.
- `method-call-summary.jsonl` aggregates repeated call edges and suppresses
  platform/API calls by default.
- `method-call-reverse-summary.jsonl` has the same aggregation level as
  `method-call-summary.jsonl`, but starts each line with the called method so
  "who calls this method/class" searches have a target-first entry point.
- Raw reverse class reference edges are not generated by default. If added,
  they should remain an explicit debug/detail option because they can grow
  quickly on large systems.
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
