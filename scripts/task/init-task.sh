#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./common.sh
source "${SCRIPT_DIR}/common.sh"

slug="${1:-}"
shift || true
title=""
jira_issue=""

[[ -n "${slug}" ]] || {
  echo "Error: usage: init-task.sh <slug> [title] --jira <ISSUE_KEY>" >&2
  exit 1
}

if [[ "${1:-}" != "--jira" && -n "${1:-}" ]]; then
  title="$1"
  shift
fi

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --jira)
      jira_issue="${2:-}"
      [[ -n "${jira_issue}" ]] || fail "--jira requires an issue key."
      shift 2
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

[[ -n "${jira_issue}" ]] || fail "Jira issue is required. Usage: init-task.sh <slug> [title] --jira <ISSUE_KEY>"
"${SCRIPT_DIR}/jira-board.sh" validate-start "${jira_issue}"

worktree="$("${SCRIPT_DIR}/new-worktree.sh" "${slug}")"
STRICT_REPO_ROOT="${worktree}" "${SCRIPT_DIR}/jira-board.sh" start-task "${slug}" "${jira_issue}"
STRICT_REPO_ROOT="${worktree}" "${SCRIPT_DIR}/allocate-port.sh" "${slug}"
STRICT_REPO_ROOT="${worktree}" "${SCRIPT_DIR}/create-log-dir.sh" "${slug}"
STRICT_REPO_ROOT="${worktree}" "${SCRIPT_DIR}/new-exec-plan.sh" "${slug}" "${title}"
