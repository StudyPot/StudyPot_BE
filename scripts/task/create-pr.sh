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
validate_commit_subject "${title}" "PR title"
repo_name="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
issue_number="${STRICT_ISSUE_NUMBER:-}"

if [[ -z "${issue_number}" ]]; then
  issue_text="$(printf "## 작업\n\n%s\n\n## Jira\n\n- 이슈: \`%s\`\n- URL: %s\n\n## 증거\n\n- EXEC_PLAN: \`%s\`\n- 검증: \`%s\` / \`%s\` / \`%s\`\n" \
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
  printf '## 실행 계획\n\n'
  printf -- "- 경로: \`%s\`\n\n" "${EXEC_PLAN}"
  sed -n '1,220p' "${EXEC_PLAN}"
  printf '\n\n## 검증\n\n'
  printf -- "- 명령: \`%s\`\n" "${LAST_VERIFY_COMMAND:-unknown}"
  printf -- "- 상태: \`%s\`\n" "${LAST_VERIFY_STATUS:-unknown}"
  printf -- "- 시각: \`%s\`\n" "${LAST_VERIFY_AT:-unknown}"
  printf '\n## 리뷰 게이트 체크리스트\n\n'
  printf -- '- [ ] 최신 head에 GitHub Actions Review Gate PASS marker 게시\n'
  printf -- '- [ ] 필수 GitHub Actions checks 통과\n'
  printf -- '- [ ] reviewdog/actionlint 피드백 반영\n'
  printf -- '- [ ] 완료 또는 문서화된 blocker까지 작업 연속성 유지\n'
  printf -- '- [ ] 예상 밖 범위 또는 의견 의존 tradeoff의 사용자 결정 기록\n'
  printf -- '- [ ] 최신 head에 CTO Architecture Gate 증거 포함 marker 게시\n'
  printf -- '- [ ] 최신 head에 QA Verification Gate 증거 포함 marker 게시\n'
  printf -- '- [ ] 최신 head에 Product Value Gate 증거 포함 marker 게시\n'
  printf -- '- [ ] 최신 head에 Final CTO Merge Gate 증거 포함 marker 게시\n'
  printf -- '- [ ] 리뷰 thread 해결\n'
} > "${pr_body}"

pr_url="$(gh pr create --base "${base}" --head "${branch}" --title "${title}" --body-file "${pr_body}")"
printf '%s\n' "${pr_url}"

pr_number="${pr_url##*/}"
if [[ "${STRICT_AUTO_FINISH_PR:-0}" != "0" ]]; then
  "${SCRIPT_DIR}/finish-pr.sh" "${pr_number}"
fi
