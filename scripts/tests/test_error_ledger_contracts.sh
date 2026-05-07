#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

ledger="${TEST_ROOT}/docs/operations/error-ledger.md"
obsidian_doc="${TEST_ROOT}/docs/operations/obsidian-error-ledger.md"
docs_index="${TEST_ROOT}/docs/index.md"

assert_file_exists "${ledger}"
assert_contains "[Error Ledger](./operations/error-ledger.md)" "${docs_index}"
assert_contains "[Error Ledger](./error-ledger.md)" "${obsidian_doc}"
assert_contains "Repo-tracked source of truth" "${obsidian_doc}"
assert_contains "iCloud read hang" "${obsidian_doc}"

for section in \
  "Date" \
  "Work / feature id" \
  "Symptom" \
  "Cause" \
  "Fix" \
  "Prevent next time" \
  "Next checkpoint"; do
  assert_contains "${section}" "${ledger}"
done

for incident in \
  "Jira API Token Exposed In Chat" \
  "finish-pr.sh\` Blocked By \`BEHIND" \
  "reviewdog ShellCheck \`SC1091" \
  "Installed Hook Symlinks Could Not Source \`common.sh" \
  "PR Creation Blocked By Missing Verification State" \
  "Obsidian/iCloud Error Ledger Read Hung"; do
  assert_contains "${incident}" "${ledger}"
done

if rg -n "ATATT|JIRA_API_TOKEN=.*[A-Za-z0-9_-]{20,}|api-token|password=" "${ledger}"; then
  fail "error ledger must not contain secret-looking token values."
fi
