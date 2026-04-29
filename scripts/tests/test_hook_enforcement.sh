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
jira_api="${tmp}/fake-jira-api.sh"
setup_sandbox_repo "${repo}"
printf '해야 할 일\n' > "${jira_status}"
write_fake_jira_api "${jira_api}"

STRICT_REPO_ROOT="${repo}" \
STRICT_WORKTREE_BASE_DIR="${worktrees}" \
STRICT_LOG_BASE_DIR="${logs}" \
STRICT_JIRA_API_STUB="${jira_api}" \
JIRA_FAKE_STATUS_FILE="${jira_status}" \
"${TEST_ROOT}/scripts/task/init-task.sh" hook-task "Hook Task" --jira SPT-1 >/dev/null

worktree="${worktrees}/hook-task"
mkdir -p "${worktree}/docs/testing" "${worktree}/src/main" "${worktree}/src/test"
plan="${worktree}/docs/exec-plans/active/$(date '+%Y%m%d')-hook-task.md"
printf '# docs\n' > "${worktree}/docs/testing/codex-harness.md"
cat > "${plan}" <<'PLAN'
# EXEC_PLAN: Hook Task

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md

## Related Feature IDs
- [x] harness-smoke

## Doc Notes
- Smoke notes

## Goal
- Smoke goal

## Approach
- Smoke approach

## Step Plan
- Smoke step

## Done Criteria
- Smoke done
PLAN

printf 'x\n' > "${worktree}/src/main/example.txt"
printf 'x\n' > "${worktree}/src/test/example-test.txt"
git -C "${worktree}" add docs src

STRICT_REPO_ROOT="${worktree}" STRICT_VERIFY_COMMAND="true" "${TEST_ROOT}/scripts/hooks/pre-commit.sh"
STRICT_REPO_ROOT="${worktree}" STRICT_VERIFY_COMMAND="true" "${TEST_ROOT}/scripts/hooks/pre-push.sh"
