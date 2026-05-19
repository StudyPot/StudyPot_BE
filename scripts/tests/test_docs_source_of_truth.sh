#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

assert_contains "test_docs_source_of_truth.sh" "${TEST_ROOT}/scripts/tests/run.sh"
assert_contains "Published source tasks" "${TEST_ROOT}/docs/index.md"
assert_contains "SPT-4" "${TEST_ROOT}/docs/index.md"
assert_contains "SPT-6" "${TEST_ROOT}/docs/index.md"
assert_contains "SPT-7" "${TEST_ROOT}/docs/index.md"
assert_contains "SPT-56" "${TEST_ROOT}/docs/index.md"
assert_contains "SPT-4\`: Requirements source, completed" "${TEST_ROOT}/docs/confluence/10-jira-mapping.md"
assert_contains "SPT-6\`: ERD/data-model source, completed" "${TEST_ROOT}/docs/confluence/10-jira-mapping.md"
assert_contains "SPT-7\`: API source, completed" "${TEST_ROOT}/docs/confluence/10-jira-mapping.md"
assert_contains "SPT-56\`: remaining documentation source-of-truth cleanup Task" "${TEST_ROOT}/docs/confluence/10-jira-mapping.md"
assert_contains "작업\`/\`Task" "${TEST_ROOT}/docs/confluence/10-jira-mapping.md"
assert_contains "CR-20260512-retrospective-rag-boundary" "${TEST_ROOT}/docs/specs/change-control-v1.md"
assert_contains "ADR-20260512-retrospective-rag-boundary" "${TEST_ROOT}/docs/specs/change-control-v1.md"
assert_contains "CR-20260519-redis-rabbitmq-realtime-infra" "${TEST_ROOT}/docs/specs/change-control-v1.md"
assert_contains "ADR-20260519-redis-rabbitmq-realtime-infra" "${TEST_ROOT}/docs/specs/change-control-v1.md"
assert_contains "RabbitMQ is the async dispatch layer" "${TEST_ROOT}/docs/specs/change-control-v1.md"
assert_contains "RabbitMQ must not own final notification status" "${TEST_ROOT}/docs/specs/adr/ADR-20260519-redis-rabbitmq-realtime-infra.md"
assert_contains "Redis/RabbitMQ runtime boundaries" "${TEST_ROOT}/docs/specs/ai-contract-v1.md"
assert_contains "Runtime Infrastructure Boundary" "${TEST_ROOT}/docs/specs/notification-contract-v1.md"
assert_contains "not in current prod compose" "${TEST_ROOT}/docs/architecture/backend-map.md"
assert_contains "DB-First Context Builder" "${TEST_ROOT}/docs/specs/ai-contract-v1.md"
assert_contains "Vector store, GraphRAG, MCP, FastAPI service split" "${TEST_ROOT}/docs/confluence/06-ai-team-leader.md"
assert_contains "CR-20260519-redis-rabbitmq-realtime-infra" "${TEST_ROOT}/docs/confluence/00-doc-hub.md"

if rg -n "/Users/hyunwoo/Documents/New project 3" \
  "${TEST_ROOT}/ARCHITECTURE.md" \
  "${TEST_ROOT}/docs" \
  --glob '!docs/exec-plans/**' >/tmp/studypot-stale-doc-paths.txt; then
  cat /tmp/studypot-stale-doc-paths.txt >&2
  fail "current documentation must not point at the old repository path."
fi
rm -f /tmp/studypot-stale-doc-paths.txt

python3 - <<'PY'
from pathlib import Path
from urllib.parse import unquote
import re
import sys

root = Path(".").resolve()
broken = []
link_pattern = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")

for path in sorted((root / "docs").rglob("*.md")):
    text = path.read_text(encoding="utf-8")
    for raw in link_pattern.findall(text):
        target = raw.strip()
        if not target or target.startswith(("#", "http://", "https://", "mailto:")):
            continue
        target = target.split("#", 1)[0].split("?", 1)[0].strip()
        if not target or target.startswith("/"):
            continue
        resolved = (path.parent / unquote(target)).resolve()
        try:
            resolved.relative_to(root)
        except ValueError:
            continue
        if not resolved.exists():
            broken.append(f"{path.relative_to(root)} -> {target}")

if broken:
    print("Broken local markdown links:", file=sys.stderr)
    for item in broken:
        print(f"- {item}", file=sys.stderr)
    sys.exit(1)
PY
