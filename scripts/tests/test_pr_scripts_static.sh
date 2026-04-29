#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

for script in create-pr.sh verify-pr-ready.sh finish-pr.sh jira-board.sh; do
  path="${TEST_ROOT}/scripts/task/${script}"
  assert_file_exists "${path}"
  bash -n "${path}"
done

assert_file_exists "${TEST_ROOT}/.github/workflows/pr-quality.yml"
assert_file_exists "${TEST_ROOT}/.github/workflows/codeql.yml"
assert_file_exists "${TEST_ROOT}/.github/dependabot.yml"
assert_contains "verify-pr-ready.sh" "${TEST_ROOT}/scripts/task/finish-pr.sh"
assert_contains "GitHub Actions Review Gate: PASS" "${TEST_ROOT}/scripts/task/finish-pr.sh"
assert_contains "STRICT_REQUIRE_GITHUB_ACTIONS_REVIEW_PASS" "${TEST_ROOT}/scripts/task/finish-pr.sh"
assert_contains "harness-tests shellcheck-reviewdog workflow-lint openapi-parse backend-check codeql-scan review-gate-pass" "${TEST_ROOT}/scripts/task/verify-pr-ready.sh"
assert_contains "jira_board_mark_done_for_task" "${TEST_ROOT}/scripts/task/finish-pr.sh"
assert_contains "Closes #" "${TEST_ROOT}/scripts/task/create-pr.sh"
assert_contains "Jira: [" "${TEST_ROOT}/scripts/task/create-pr.sh"
assert_contains "GitHub Actions Review Gate pass marker" "${TEST_ROOT}/scripts/task/create-pr.sh"
assert_contains "reviewdog/action-shellcheck@v1" "${TEST_ROOT}/.github/workflows/pr-quality.yml"
assert_contains "reviewdog/action-actionlint@v1" "${TEST_ROOT}/.github/workflows/pr-quality.yml"
assert_contains "actions/github-script@v7" "${TEST_ROOT}/.github/workflows/pr-quality.yml"
assert_contains "github/codeql-action/init@v3" "${TEST_ROOT}/.github/workflows/codeql.yml"
assert_contains "package-ecosystem: \"github-actions\"" "${TEST_ROOT}/.github/dependabot.yml"
assert_contains "validate-start" "${TEST_ROOT}/scripts/task/init-task.sh"
