#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
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

"${SCRIPT_DIR}/verify-coderabbit-review.sh" "${pr}"

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

required_checks="${STRICT_REQUIRED_CHECKS:-harness-tests shellcheck-reviewdog workflow-lint openapi-parse db-schema-coverage backend-check codeql-scan review-gate-pass}"
for required_check in ${required_checks}; do
  check_state="$(gh pr view "${pr}" --json statusCheckRollup --jq "
    [.statusCheckRollup[]? | select(
      (.name? == \"${required_check}\")
      or (.context? == \"${required_check}\")
      or ((.name? // \"\") | endswith(\" / ${required_check}\"))
      or ((.context? // \"\") | endswith(\" / ${required_check}\"))
    )]
    | if length == 0 then
        \"missing\"
      else
        .[0]
        | if ((.conclusion? // \"\") != \"\") then
            .conclusion
          elif ((.state? // \"\") != \"\") then
            .state
          elif ((.status? // \"\") != \"\") then
            .status
          else
            \"UNKNOWN\"
          end
      end
  ")"
  case "${check_state}" in
    SUCCESS|SKIPPED|NEUTRAL)
      ;;
    missing)
      fail "required PR check is missing: ${required_check}"
      ;;
    *)
      fail "required PR check is not passing: ${required_check} (${check_state})"
      ;;
  esac
done

merge_state="$(gh pr view "${pr}" --json mergeStateStatus --jq '.mergeStateStatus // ""')"
case "${merge_state}" in
  DIRTY|BEHIND)
    fail "PR merge state is blocked: ${merge_state}"
    ;;
  BLOCKED)
    [[ "${STRICT_ALLOW_BLOCKED_FOR_MANUAL_MERGE:-0}" == "1" ]] || fail "PR merge state is blocked: ${merge_state}"
    ;;
esac

repo_name="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
owner="${repo_name%%/*}"
name="${repo_name#*/}"
# shellcheck disable=SC2016
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
