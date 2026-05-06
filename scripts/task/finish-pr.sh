#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"
# shellcheck source=scripts/task/jira-board.sh
JIRA_BOARD_SOURCE_ONLY=1 source "${SCRIPT_DIR}/jira-board.sh"

resolve_threads=0
pr=""
for arg in "$@"; do
  case "${arg}" in
    --resolve-threads)
      resolve_threads=1
      ;;
    *)
      pr="${arg}"
      ;;
  esac
done

[[ -n "${pr}" ]] || fail "usage: finish-pr.sh <PR_NUMBER> [--resolve-threads]"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."
[[ "${resolve_threads}" == "0" ]] || fail "--resolve-threads is intentionally not automatic yet. Resolve addressed threads in GitHub, then rerun."

repo="$(repo_root)"
primary_repo="$(primary_worktree_path)"
base_ref="$(gh pr view "${pr}" --json baseRefName --jq .baseRefName)"
head_ref="$(gh pr view "${pr}" --json headRefName --jq .headRefName)"
head_before="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"

[[ "${base_ref}" == "develop" ]] || fail "finish-pr.sh only merges PRs targeting develop. Found: ${base_ref}"
[[ "${head_ref}" =~ ^codex/([a-z0-9][a-z0-9-]*)$ ]] || fail "PR head must be codex/<slug>. Found: ${head_ref}"
slug="${BASH_REMATCH[1]}"
load_task_env "${slug}"
[[ -n "${JIRA_ISSUE_KEY:-}" ]] || fail "Jira issue is required in task state before finishing PR."

if [[ -n "${STRICT_REVIEW_BOT_LOGIN:-}" ]]; then
  found_bot_activity="$(gh pr view "${pr}" --json comments,reviews --jq "
    ((.comments // []) + (.reviews // []))
    | map(select(.author.login == \"${STRICT_REVIEW_BOT_LOGIN}\"))
    | length
  ")"
  [[ "${found_bot_activity}" -gt 0 ]] || fail "required review bot activity not found: ${STRICT_REVIEW_BOT_LOGIN}"
fi

"${SCRIPT_DIR}/verify-pr-ready.sh" "${pr}"
head_after="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
[[ "${head_before}" == "${head_after}" ]] || fail "PR head changed during verification."

if [[ "${STRICT_REQUIRE_GITHUB_ACTIONS_REVIEW_PASS:-1}" != "0" ]]; then
  pass_count="$(gh pr view "${pr}" --json comments --jq "
    [.comments[]? | select((.body | contains(\"GitHub Actions Review Gate: PASS\")) and (.body | contains(\"Head: ${head_after}\")))] | length
  ")"
  [[ "${pass_count}" -gt 0 ]] || fail "latest-head GitHub Actions review gate pass marker is missing."
fi

subagent_review_marker() {
  case "$1" in
    1)
      printf 'Codex Subagent Review Round 1: PASS'
      ;;
    2)
      printf 'Codex Subagent Review Round 2: PASS'
      ;;
    3)
      printf 'Codex Subagent Review Round 3: PASS'
      ;;
    *)
      fail "unknown Codex subagent review round: $1"
      ;;
  esac
}

required_subagent_rounds="${STRICT_REQUIRE_CODEX_SUBAGENT_ROUNDS:-3}"
[[ "${required_subagent_rounds}" =~ ^[0-3]$ ]] || fail "STRICT_REQUIRE_CODEX_SUBAGENT_ROUNDS must be 0, 1, 2, or 3."

if [[ "${STRICT_REQUIRE_CODEX_SUBAGENT_PASS:-0}" == "1" && "${required_subagent_rounds}" == "0" ]]; then
  required_subagent_rounds="3"
fi

for ((round = 1; round <= required_subagent_rounds; round++)); do
  marker="$(subagent_review_marker "${round}")"
  pass_count="$(gh pr view "${pr}" --json comments --jq "
    [.comments[]? | select((.body | contains(\"${marker}\")) and (.body | contains(\"Head: ${head_after}\")))] | length
  ")"
  [[ "${pass_count}" -gt 0 ]] || fail "latest-head Codex subagent review round ${round} pass marker is missing."
done

feature_worktree=""
locked="0"
while IFS= read -r line; do
  case "${line}" in
    worktree\ *)
      current_path="${line#worktree }"
      current_branch=""
      current_locked="0"
      ;;
    branch\ refs/heads/*)
      current_branch="${line#branch refs/heads/}"
      ;;
    locked*)
      current_locked="1"
      ;;
    "")
      if [[ "${current_branch:-}" == "${head_ref}" ]]; then
        feature_worktree="${current_path:-}"
        locked="${current_locked:-0}"
      fi
      ;;
  esac
done < <(git -C "${repo}" worktree list --porcelain; printf '\n')

[[ -n "${feature_worktree}" && -d "${feature_worktree}" ]] || fail "feature worktree not found for ${head_ref}"
[[ "${locked}" == "0" ]] || fail "feature worktree is locked: ${feature_worktree}"
[[ -z "$(git -C "${feature_worktree}" status --short)" ]] || fail "feature worktree is dirty: ${feature_worktree}"
local_head="$(git -C "${feature_worktree}" rev-parse HEAD)"
[[ "${local_head}" == "${head_after}" ]] || fail "feature worktree HEAD does not match verified PR head."

git -C "${feature_worktree}" fetch origin "${head_ref}" >/dev/null 2>&1 || true
if git -C "${feature_worktree}" rev-parse --verify "origin/${head_ref}" >/dev/null 2>&1; then
  read -r behind ahead < <(git -C "${feature_worktree}" rev-list --left-right --count "origin/${head_ref}...HEAD")
  [[ "${ahead}" == "0" && "${behind}" == "0" ]] || fail "feature branch is ahead/diverged from origin/${head_ref}."
fi

gh pr merge "${pr}" --merge --match-head-commit "${head_after}"
git -C "${repo}" push --no-verify origin --delete "${head_ref}" || true

git -C "${primary_repo}" fetch origin develop >/dev/null
if git -C "${primary_repo}" show-ref --verify --quiet refs/heads/develop; then
  develop_worktree="$(git -C "${primary_repo}" worktree list --porcelain | awk '
    /^worktree / { path=$2 }
    /^branch refs\/heads\/develop$/ { print path }
  ')"
  if [[ -n "${develop_worktree}" ]]; then
    [[ -z "$(git -C "${develop_worktree}" status --short)" ]] || fail "local develop worktree is dirty: ${develop_worktree}"
    git -C "${develop_worktree}" merge --ff-only origin/develop
  else
    git -C "${primary_repo}" branch -f develop origin/develop
  fi
else
  git -C "${primary_repo}" branch develop origin/develop
fi

git -C "${primary_repo}" worktree remove "${feature_worktree}"
git -C "${primary_repo}" branch -d "${head_ref}" || true

STRICT_REPO_ROOT="${primary_repo}" jira_board_mark_done_for_task "${slug}" "${JIRA_ISSUE_KEY}"

printf 'Finished PR %s and cleaned %s.\n' "${pr}" "${head_ref}"
