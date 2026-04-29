#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

repo="${tmp}/repo"
worktrees="${tmp}/worktrees"
logs="${tmp}/logs"
jira_status="${tmp}/jira-status"
jira_log="${tmp}/jira.log"
jira_api="${tmp}/fake-jira-api.sh"
setup_sandbox_repo "${repo}"
printf '해야 할 일\n' > "${jira_status}"
write_fake_jira_api "${jira_api}"

STRICT_REPO_ROOT="${repo}" \
STRICT_WORKTREE_BASE_DIR="${worktrees}" \
STRICT_LOG_BASE_DIR="${logs}" \
STRICT_JIRA_API_STUB="${jira_api}" \
JIRA_FAKE_STATUS_FILE="${jira_status}" \
JIRA_FAKE_LOG="${jira_log}" \
"${TEST_ROOT}/scripts/task/init-task.sh" sample-task "Sample Task" --jira SPT-1 >/dev/null

[[ -d "${worktrees}/sample-task" ]] || fail "expected worktree to exist"
assert_file_exists "${worktrees}/sample-task/.codex/task-state/sample-task.env"
assert_file_exists "${worktrees}/sample-task/docs/exec-plans/active/$(date '+%Y%m%d')-sample-task.md"
assert_contains "## Related Feature IDs" "${worktrees}/sample-task/docs/exec-plans/active/$(date '+%Y%m%d')-sample-task.md"
assert_contains "JIRA_ISSUE_KEY=SPT-1" "${worktrees}/sample-task/.codex/task-state/sample-task.env"
assert_contains "Jira issue: \`SPT-1\`" "${worktrees}/sample-task/docs/exec-plans/active/$(date '+%Y%m%d')-sample-task.md"
[[ "$(cat "${jira_status}")" == "진행 중" ]] || fail "expected Jira status to be 진행 중"
