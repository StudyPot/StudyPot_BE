#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

assert_contains "test_quality_gate_contracts.sh" "${TEST_ROOT}/scripts/tests/run.sh"
assert_contains "db-schema-coverage" "${TEST_ROOT}/.github/workflows/pr-quality.yml"
assert_contains "db-schema-coverage" "${TEST_ROOT}/scripts/task/verify-pr-ready.sh"
assert_contains "db-schema-coverage" "${TEST_ROOT}/docs/operations/pr-review-gate.md"
assert_contains "db-schema-coverage" "${TEST_ROOT}/docs/testing/codex-harness.md"

ruby -e '
  require "yaml"
  doc = YAML.load_file("docs/specs/openapi.yaml")
  abort("openapi must be 3.1.x") unless doc.fetch("openapi").to_s.start_with?("3.1.")
  abort("info.title is required") unless doc.dig("info", "title")
  abort("paths is required") unless doc["paths"].is_a?(Hash)
'

python3 - <<'PY'
from pathlib import Path
import re
import sys

root = Path(".")
contract = (root / "docs/specs/db-contract-v1.md").read_text()
schema = (root / "docs/specs/db-schema-v1.sql").read_text()
migration_path = root / "src/main/resources/db/migration/V1__erd_v0_8_mysql8_schema.sql"

if not migration_path.is_file():
    print(f"missing Flyway ERD baseline migration: {migration_path}", file=sys.stderr)
    sys.exit(1)

migration = migration_path.read_text()

def normalize_sql(value):
    value = value.replace("\r\n", "\n")
    value = re.sub(r"[ \t]+\n", "\n", value)
    return value.strip()

if normalize_sql(migration) != normalize_sql(schema):
    print("Flyway migration must match docs/specs/db-schema-v1.sql", file=sys.stderr)
    sys.exit(1)

expected = re.findall(r"^\|\s*\d+\s*\|\s*`([^`]+)`\s*\|", contract, flags=re.MULTILINE)
actual = re.findall(r"(?im)^create\s+table\s+`?([a-z0-9_]+)`?\s*\(", schema)
migration_actual = re.findall(r"(?im)^create\s+table\s+`?([a-z0-9_]+)`?\s*\(", migration)

if len(expected) != 19:
    print(f"expected DB contract to list 19 ERD tables, found {len(expected)}", file=sys.stderr)
    sys.exit(1)

missing = sorted(set(expected) - set(actual))
extra = sorted(set(actual) - set(expected))
if missing or extra:
    print(f"DB schema coverage mismatch: missing={missing}, extra={extra}", file=sys.stderr)
    sys.exit(1)

if set(migration_actual) != set(expected):
    print(
        f"Flyway migration coverage mismatch: missing={sorted(set(expected) - set(migration_actual))}, extra={sorted(set(migration_actual) - set(expected))}",
        file=sys.stderr,
    )
    sys.exit(1)

for table in expected:
    match = re.search(
        rf"(?is)^create\s+table\s+`?{re.escape(table)}`?\s*\((.*?)\)\s*engine=",
        migration,
        flags=re.MULTILINE,
    )
    if not match:
        print(f"missing create table block for {table}", file=sys.stderr)
        sys.exit(1)
    if not re.search(r"(?im)^\s*id\s+binary\(16\)\s+not\s+null\b", match.group(1)):
        print(f"{table}.id must be binary(16) not null", file=sys.stderr)
        sys.exit(1)

for forbidden in ("discord_integration", "study_session", "study_group_invitation"):
    if re.search(rf"(?im)^create\s+table\s+`?{forbidden}`?\b", schema):
        print(f"deferred table must not be in MVP schema: {forbidden}", file=sys.stderr)
        sys.exit(1)

lower_schema = schema.lower()
for required_token in ("binary(16)", "timestamp(6)", " json "):
    if required_token not in lower_schema:
        print(f"schema must contain MySQL8 token: {required_token.strip()}", file=sys.stderr)
        sys.exit(1)
PY
