#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"
# shellcheck source=scripts/task/jira-board.sh
JIRA_BOARD_SOURCE_ONLY=1 source "${SCRIPT_DIR}/jira-board.sh"

mode="notify-ready"
if [[ "${1:-}" == "cleanup-merged" ]]; then
  mode="cleanup-merged"
  shift
fi

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

[[ -n "${pr}" ]] || fail "usage: finish-pr.sh [cleanup-merged] <PR_NUMBER> [--resolve-threads]"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."
[[ "${resolve_threads}" == "0" ]] || fail "--resolve-threads is intentionally not automatic yet. Resolve addressed threads in GitHub, then rerun."

repo="$(repo_root)"
primary_repo="$(primary_worktree_path)"
base_ref="$(gh pr view "${pr}" --json baseRefName --jq .baseRefName)"
head_ref="$(gh pr view "${pr}" --json headRefName --jq .headRefName)"
head_before="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
pr_title="$(gh pr view "${pr}" --json title --jq .title)"
pr_url="$(gh pr view "${pr}" --json url --jq .url)"

[[ "${base_ref}" == "develop" ]] || fail "finish-pr.sh only handles PRs targeting develop. Found: ${base_ref}"
validate_commit_subject "${pr_title}" "PR title"
[[ "${head_ref}" =~ ^codex/([a-z0-9][a-z0-9-]*)$ ]] || fail "PR head must be codex/<slug>. Found: ${head_ref}"
slug="${BASH_REMATCH[1]}"
load_task_env "${slug}"
[[ -n "${JIRA_ISSUE_KEY:-}" ]] || fail "Jira issue is required in task state before finishing PR."

require_latest_head_review_gates() {
  local head_sha="$1"
  local pass_count

  if [[ "${STRICT_REQUIRE_GITHUB_ACTIONS_REVIEW_PASS:-1}" != "0" ]]; then
    pass_count="$(gh pr view "${pr}" --json comments --jq "
      [.comments[]? | select((.body | contains(\"GitHub Actions Review Gate: PASS\")) and (.body | contains(\"Head: ${head_sha}\")))] | length
    ")"
    [[ "${pass_count}" -gt 0 ]] || fail "latest-head GitHub Actions review gate pass marker is missing."
  fi

  "${SCRIPT_DIR}/verify-role-review-gates.sh" "${pr}" "${head_sha}"
}

find_feature_worktree() {
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
}

require_clean_feature_worktree() {
  local local_head behind ahead

  find_feature_worktree
  [[ -n "${feature_worktree}" && -d "${feature_worktree}" ]] || fail "feature worktree not found for ${head_ref}"
  [[ "${locked}" == "0" ]] || fail "feature worktree is locked: ${feature_worktree}"
  [[ -z "$(git -C "${feature_worktree}" status --short)" ]] || fail "feature worktree is dirty: ${feature_worktree}"
  local_head="$(git -C "${feature_worktree}" rev-parse HEAD)"
  [[ "${local_head}" == "${head_before}" ]] || fail "feature worktree HEAD does not match PR head."

  git -C "${feature_worktree}" fetch origin "${head_ref}" >/dev/null 2>&1 || true
  if git -C "${feature_worktree}" rev-parse --verify "origin/${head_ref}" >/dev/null 2>&1; then
    read -r behind ahead < <(git -C "${feature_worktree}" rev-list --left-right --count "origin/${head_ref}...HEAD")
    [[ "${ahead}" == "0" && "${behind}" == "0" ]] || fail "feature branch is ahead/diverged from origin/${head_ref}."
  fi
}

sync_develop() {
  local develop_worktree

  git -C "${primary_repo}" fetch origin develop >/dev/null
  if git -C "${primary_repo}" show-ref --verify --quiet refs/heads/develop; then
    develop_worktree="$(git -C "${primary_repo}" worktree list --porcelain | awk '
      /^worktree / { sub(/^worktree /, ""); path=$0 }
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
}

notify_ready_for_manual_merge() {
  local head_after found_bot_activity

  if [[ -n "${STRICT_REVIEW_BOT_LOGIN:-}" ]]; then
    found_bot_activity="$(gh pr view "${pr}" --json comments,reviews --jq "
      ((.comments // []) + (.reviews // []))
      | map(select(.author.login == \"${STRICT_REVIEW_BOT_LOGIN}\"))
      | length
    ")"
    [[ "${found_bot_activity}" -gt 0 ]] || fail "required review bot activity not found: ${STRICT_REVIEW_BOT_LOGIN}"
  fi

  STRICT_ALLOW_BLOCKED_FOR_MANUAL_MERGE=1 "${SCRIPT_DIR}/verify-pr-ready.sh" "${pr}"
  head_after="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
  [[ "${head_before}" == "${head_after}" ]] || fail "PR head changed during verification."

  require_latest_head_review_gates "${head_after}"
  require_clean_feature_worktree

  "${SCRIPT_DIR}/notify-pr-ready.sh" "${pr}" "${pr_url}" "${head_after}" "ready"
  printf 'PR 수동 merge 준비 완료: %s\n' "${pr_url}"
  printf '사용자가 GitHub에서 merge한 뒤 실행: scripts/task/finish-pr.sh cleanup-merged %s\n' "${pr}"
}

cleanup_merged_pr() {
  local state

  state="$(gh pr view "${pr}" --json state --jq .state)"
  [[ "${state}" == "MERGED" ]] || fail "cleanup-merged requires PR to be MERGED. Found: ${state}"

  require_clean_feature_worktree
  git -C "${repo}" push --no-verify origin --delete "${head_ref}" || true
  sync_develop

  git -C "${primary_repo}" worktree remove "${feature_worktree}"
  git -C "${primary_repo}" branch -d "${head_ref}" || true

  STRICT_REPO_ROOT="${primary_repo}" jira_board_mark_done_for_task "${slug}" "${JIRA_ISSUE_KEY}"

  printf 'merge된 PR %s와 local branch %s 정리를 완료했습니다.\n' "${pr}" "${head_ref}"
}

case "${mode}" in
  notify-ready)
    notify_ready_for_manual_merge
    ;;
  cleanup-merged)
    cleanup_merged_pr
    ;;
  *)
    fail "unknown finish mode: ${mode}"
    ;;
esac
