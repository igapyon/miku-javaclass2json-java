# TODO

## High Priority

- Done: Add `.github/workflows/release-cli-runtime.yml` equivalent to `workplace/refs-20/miku-grep-java-devel/.github/workflows/release-cli-runtime.yml` for publishing the CLI runtime jar.
- Done: Document multi-process operation rules.
- Done: Add focused input tests for single `.jar`, directory-contained `.jar`, single `.class`, and concrete method call output.
- Done: Document existing output directory behavior.

## Medium Priority

- Done: Refactor the Java implementation where it improves maintainability.
- Done: This project is not a straight Node-to-Java conversion, so Java-native refactoring is allowed.
- Done: Exclude `module-info.class` from the shaded runtime jar if release packaging should avoid shade warnings.
- Done: Add Maven plugin execution tests beyond simple instantiation.
- Done: Decide the intended detail level for `invokedynamic`.

## Low Priority

- Done: Expand README usage examples.
- Done: Add self jar smoke script and document it in README.
- Done: Decide JSONL schema metadata policy.
- Done: Outline the separate merge / graph / advanced-index CLI.

## Decided Non-Goals

- Do not convert JVM descriptors into Java-style signatures in this CLI.
- Do not pretty-print class JSON for human readability.
- Do not add graph or advanced index generation to this CLI.
