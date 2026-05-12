#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

assert_contains "test_swagger_docs_contracts.sh" "${TEST_ROOT}/scripts/tests/run.sh"
assert_contains "Swagger Docs" "${TEST_ROOT}/docs/testing/codex-harness.md"
assert_contains "신규 API" "${TEST_ROOT}/docs/testing/codex-harness.md"

export TEST_ROOT
python3 - <<'PY'
import os
import re
import sys
from pathlib import Path

root = Path(os.environ["TEST_ROOT"])
controller_paths = sorted(root.glob("src/main/java/com/studypot/aistudyleader/**/controller/*Controller.java"))
hangul = re.compile(r"[가-힣]")
mapping = re.compile(r"@(Get|Post|Put|Patch|Delete)Mapping")
record_decl = re.compile(r"\brecord\s+([A-Za-z][A-Za-z0-9_]*)\s*\(")
failures = []

for path in controller_paths:
    text = path.read_text(encoding="utf-8")
    if "@RestController" not in text:
        continue

    rest_index = text.index("@RestController")
    class_doc = text[max(0, rest_index - 500):rest_index]
    if "@Tag(" not in class_doc or "description =" not in class_doc or not hangul.search(class_doc):
        failures.append(f"{path.relative_to(root)}: @RestController must have Korean @Tag name and description")

    lines = text.splitlines()
    for index, line in enumerate(lines):
        if not mapping.search(line):
            continue

        context = "\n".join(lines[max(0, index - 80):index + 1])
        if "@Operation(" not in context or "summary =" not in context or "description =" not in context:
            failures.append(f"{path.relative_to(root)}:{index + 1}: mapping must have @Operation summary and description")
        elif not hangul.search(context):
            failures.append(f"{path.relative_to(root)}:{index + 1}: @Operation text must be Korean")

        if "@ApiResponse" not in context:
            failures.append(f"{path.relative_to(root)}:{index + 1}: mapping must document responses with @ApiResponse")

        signature = "\n".join(lines[index:min(len(lines), index + 30)])
        mapping_line = line
        if "{" in mapping_line and "@Parameter(" not in signature:
            failures.append(f"{path.relative_to(root)}:{index + 1}: path variables must have @Parameter descriptions")

    for index, line in enumerate(lines):
        found = record_decl.search(line)
        if not found:
            continue
        record_name = found.group(1)
        context = "\n".join(lines[max(0, index - 6):index + 1])
        if "@Schema(" not in context or "description =" not in context or not hangul.search(context):
            failures.append(f"{path.relative_to(root)}:{index + 1}: record {record_name} must have Korean @Schema description")

if not controller_paths:
    failures.append("No controller files found under src/main/java")

if failures:
    print("Swagger Docs contract violations:", file=sys.stderr)
    for failure in failures:
        print(f"- {failure}", file=sys.stderr)
    sys.exit(1)
PY
