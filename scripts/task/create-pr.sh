#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

command -v gh >/dev/null 2>&1 || fail "gh CLI is required."

repo="$(repo_root)"
branch="$(git -C "${repo}" branch --show-current)"
[[ "${branch}" =~ ^codex/([a-z0-9][a-z0-9-]*)$ ]] || fail "create-pr.sh must run from codex/<slug> branch."
slug="${BASH_REMATCH[1]}"

load_task_env "${slug}"
[[ -n "${EXEC_PLAN}" && -f "${EXEC_PLAN}" ]] || fail "EXEC_PLAN이 없습니다."
[[ "${LAST_VERIFY_STATUS:-}" == "passed" ]] || fail "last verification status is not passed. Commit once after passing hooks or set task state intentionally."
[[ -n "${JIRA_ISSUE_KEY:-}" && -n "${JIRA_ISSUE_URL:-}" ]] || fail "Jira issue is required in task state. Start work with init-task.sh <slug> [title] --jira <ISSUE_KEY>."

base="${STRICT_PR_BASE:-develop}"
title="${STRICT_PR_TITLE:-${TASK_TITLE}}"
repo_name="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
issue_number="${STRICT_ISSUE_NUMBER:-}"

if [[ -z "${issue_number}" ]]; then
  issue_text="$(printf "## Work\n\n%s\n\n## Jira\n\n- Issue: \`%s\`\n- URL: %s\n\n## Evidence\n\n- EXEC_PLAN: \`%s\`\n- Verification: \`%s\` / \`%s\` / \`%s\`\n" \
    "${title}" \
    "${JIRA_ISSUE_KEY:-not-linked}" \
    "${JIRA_ISSUE_URL:-not-linked}" \
    "${EXEC_PLAN}" \
    "${LAST_VERIFY_COMMAND:-unknown}" \
    "${LAST_VERIFY_STATUS:-unknown}" \
    "${LAST_VERIFY_AT:-unknown}")"
  issue_number="$(gh api "repos/${repo_name}/issues" \
    -f title="${title}" \
    -f body="${issue_text}" \
    --jq .number)"
fi

git -C "${repo}" push -u origin "${branch}"

pr_body="$(mktemp)"
trap 'rm -f "${pr_body:-}"' EXIT
{
  printf 'Closes #%s\n\n' "${issue_number}"
  if [[ -n "${JIRA_ISSUE_KEY:-}" ]]; then
    printf 'Jira: [%s](%s)\n\n' "${JIRA_ISSUE_KEY}" "${JIRA_ISSUE_URL}"
  fi
  printf '## EXEC_PLAN\n\n'
  printf -- "- Path: \`%s\`\n\n" "${EXEC_PLAN}"
  sed -n '1,220p' "${EXEC_PLAN}"
  printf '\n\n## Verification\n\n'
  printf -- "- Command: \`%s\`\n" "${LAST_VERIFY_COMMAND:-unknown}"
  printf -- "- Status: \`%s\`\n" "${LAST_VERIFY_STATUS:-unknown}"
  printf -- "- Time: \`%s\`\n" "${LAST_VERIFY_AT:-unknown}"
  printf '\n## Review Gate Checklist\n\n'
  printf -- '- [ ] GitHub Actions Review Gate pass marker posted for latest head\n'
  printf -- '- [ ] Required GitHub Actions checks passing\n'
  printf -- '- [ ] reviewdog/actionlint feedback addressed\n'
  printf -- '- [ ] Feature continuity maintained until completion or documented blocker\n'
  printf -- '- [ ] User decision recorded for any unplanned scope or opinion-sensitive tradeoff\n'
  printf -- '- [ ] CTO Architecture Gate evidence-backed marker posted for latest head\n'
  printf -- '- [ ] QA Verification Gate evidence-backed marker posted for latest head\n'
  printf -- '- [ ] Product Value Gate evidence-backed marker posted for latest head\n'
  printf -- '- [ ] Final CTO Merge Gate evidence-backed marker posted for latest head\n'
  printf -- '- [ ] Review threads resolved\n'
} > "${pr_body}"

pr_url="$(gh pr create --base "${base}" --head "${branch}" --title "${title}" --body-file "${pr_body}")"
printf '%s\n' "${pr_url}"

pr_number="${pr_url##*/}"
if [[ "${STRICT_AUTO_FINISH_PR:-0}" != "0" ]]; then
  "${SCRIPT_DIR}/finish-pr.sh" "${pr_number}"
fi
