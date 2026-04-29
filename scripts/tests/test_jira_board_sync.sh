#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

repo="${tmp}/repo"
jira_api="${tmp}/fake-jira-api.sh"
jira_status="${tmp}/jira-status"
jira_log="${tmp}/jira.log"
setup_sandbox_repo "${repo}"
write_fake_jira_api "${jira_api}"

run_jira() {
  STRICT_REPO_ROOT="${repo}" \
  STRICT_JIRA_API_STUB="${jira_api}" \
  JIRA_FAKE_STATUS_FILE="${jira_status}" \
  JIRA_FAKE_LOG="${jira_log}" \
  "${TEST_ROOT}/scripts/task/jira-board.sh" "$@"
}

printf '해야 할 일\n' > "${jira_status}"
run_jira start-task sample-task SPT-1 >/dev/null
[[ "$(cat "${jira_status}")" == "진행 중" ]] || fail "expected start-task to move Jira to 진행 중"
assert_file_exists "${repo}/.codex/task-state/sample-task.env"
assert_contains "JIRA_ISSUE_KEY=SPT-1" "${repo}/.codex/task-state/sample-task.env"

run_jira start-task sample-task SPT-1 >/dev/null
[[ "$(cat "${jira_status}")" == "진행 중" ]] || fail "expected in-progress Jira start to be idempotent"

printf '완료\n' > "${jira_status}"
if run_jira start-task done-task SPT-1 >/dev/null 2>&1; then
  fail "expected completed Jira issue to be blocked at start"
fi

printf '해야 할 일\n' > "${jira_status}"
if STRICT_REPO_ROOT="${repo}" \
  STRICT_JIRA_API_STUB="${jira_api}" \
  JIRA_FAKE_STATUS_FILE="${jira_status}" \
  JIRA_FAKE_NO_TRANSITION=1 \
  "${TEST_ROOT}/scripts/task/jira-board.sh" validate-start SPT-1 >/dev/null 2>&1; then
  fail "expected missing transition target to fail"
fi

printf '진행 중\n' > "${jira_status}"
run_jira done-task sample-task SPT-1 >/dev/null
[[ "$(cat "${jira_status}")" == "완료" ]] || fail "expected done-task to move Jira to 완료"
assert_contains "JIRA_DONE_AT=" "${repo}/.codex/task-state/sample-task.env"
