#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
[[ -n "${pr}" ]] || fail "usage: verify-pr-ready.sh <PR_NUMBER>"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."

state="$(gh pr view "${pr}" --json state --jq .state)"
[[ "${state}" == "OPEN" ]] || fail "PR is not open: ${state}"

is_draft="$(gh pr view "${pr}" --json isDraft --jq .isDraft)"
[[ "${is_draft}" == "false" ]] || fail "PR is draft."

review_decision="$(gh pr view "${pr}" --json reviewDecision --jq '.reviewDecision // ""')"
[[ "${review_decision}" != "CHANGES_REQUESTED" ]] || fail "PR has CHANGES_REQUESTED."

activity_count="$(gh pr view "${pr}" --json comments,reviews --jq '(.comments | length) + (.reviews | length)')"
[[ "${activity_count}" -gt 0 ]] || fail "PR has no review/comment activity."

rollup_count="$(gh pr view "${pr}" --json statusCheckRollup --jq '(.statusCheckRollup // []) | length')"
if [[ "${rollup_count}" -gt 0 && "${STRICT_SKIP_CHECK_WATCH:-0}" != "1" ]]; then
  gh pr checks "${pr}" --watch --fail-fast >/dev/null || fail "PR checks are pending, failing, or cancelled."
fi

bad_checks="$(gh pr view "${pr}" --json statusCheckRollup --jq '
  (.statusCheckRollup // [])
  | map(select(
      ((.conclusion? // "") != "" and (.conclusion != "SUCCESS" and .conclusion != "SKIPPED" and .conclusion != "NEUTRAL"))
      or ((.status? // "") != "" and .status != "COMPLETED")
      or ((.state? // "") != "" and (.state != "SUCCESS" and .state != "SKIPPED" and .state != "NEUTRAL"))
    ))
  | length
')"
[[ "${bad_checks}" == "0" ]] || fail "PR has non-passing checks."

merge_state="$(gh pr view "${pr}" --json mergeStateStatus --jq '.mergeStateStatus // ""')"
case "${merge_state}" in
  DIRTY|BLOCKED|BEHIND)
    fail "PR merge state is blocked: ${merge_state}"
    ;;
esac

repo_name="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
owner="${repo_name%%/*}"
name="${repo_name#*/}"
unresolved_count="$(gh api graphql \
  -f query='query($owner:String!, $name:String!, $number:Int!) {
    repository(owner:$owner, name:$name) {
      pullRequest(number:$number) {
        reviewThreads(first:100) {
          nodes { isResolved }
        }
      }
    }
  }' \
  -F owner="${owner}" \
  -F name="${name}" \
  -F number="${pr}" \
  --jq '[.data.repository.pullRequest.reviewThreads.nodes[]? | select(.isResolved == false)] | length')"
[[ "${unresolved_count}" == "0" ]] || fail "PR has unresolved review threads: ${unresolved_count}"

printf 'PR %s is ready for finish gate checks.\n' "${pr}"
