#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

JAR="miku-javaclass2json/target/miku-javaclass2json-0.5.4.jar"
OUTPUT="workplace/smoke-self-jar-index"

mvn package

if [ ! -f "$JAR" ]; then
  echo "missing runtime jar: $JAR" >&2
  exit 1
fi

rm -rf "$OUTPUT"

java -jar "$JAR" index --input "$JAR" --output "$OUTPUT"

for file in \
  "$OUTPUT/index.json" \
  "$OUTPUT/classes.jsonl" \
  "$OUTPUT/symbols.jsonl" \
  "$OUTPUT/dependencies.jsonl" \
  "$OUTPUT/method-calls.jsonl" \
  "$OUTPUT/method-call-summary.jsonl" \
  "$OUTPUT/method-call-reverse-summary.jsonl" \
  "$OUTPUT/sources.jsonl" \
  "$OUTPUT/warnings.log"
do
  if [ ! -f "$file" ]; then
    echo "missing generated file: $file" >&2
    exit 1
  fi
done

if ! grep -q '"methodCallsIndex": "method-calls.jsonl"' "$OUTPUT/index.json"; then
  echo "index.json does not reference method-calls.jsonl" >&2
  exit 1
fi

if ! grep -q '"methodCallReverseSummaryIndex": "method-call-reverse-summary.jsonl"' "$OUTPUT/index.json"; then
  echo "index.json does not reference method-call-reverse-summary.jsonl" >&2
  exit 1
fi

if [ ! -s "$OUTPUT/method-calls.jsonl" ]; then
  echo "method-calls.jsonl is empty" >&2
  exit 1
fi

if [ ! -s "$OUTPUT/method-call-reverse-summary.jsonl" ]; then
  echo "method-call-reverse-summary.jsonl is empty" >&2
  exit 1
fi

echo "smoke ok: $OUTPUT"
