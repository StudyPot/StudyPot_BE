#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

for script in create-pr.sh verify-pr-ready.sh finish-pr.sh jira-board.sh; do
  path="${TEST_ROOT}/scripts/task/${script}"
  assert_file_exists "${path}"
  bash -n "${path}"
done

assert_contains "verify-pr-ready.sh" "${TEST_ROOT}/scripts/task/finish-pr.sh"
assert_contains "Codex Subagent Review Gate: PASS" "${TEST_ROOT}/scripts/task/finish-pr.sh"
assert_contains "jira_board_mark_done_for_task" "${TEST_ROOT}/scripts/task/finish-pr.sh"
assert_contains "Closes #" "${TEST_ROOT}/scripts/task/create-pr.sh"
assert_contains "Jira: [" "${TEST_ROOT}/scripts/task/create-pr.sh"
assert_contains "validate-start" "${TEST_ROOT}/scripts/task/init-task.sh"
