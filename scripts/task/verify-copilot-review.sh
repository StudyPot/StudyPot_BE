#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
head_sha="${2:-}"
[[ -n "${pr}" ]] || fail "usage: verify-copilot-review.sh <PR_NUMBER> [HEAD_SHA]"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."

if [[ "${STRICT_REQUIRE_COPILOT_REVIEW:-1}" == "0" ]]; then
  printf 'Copilot review gate skipped for PR %s.\n' "${pr}"
  exit 0
fi

reviewer="${STRICT_COPILOT_REVIEW_LOGIN:-copilot-pull-request-reviewer}"
if [[ -z "${head_sha}" ]]; then
  head_sha="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
fi

if [[ "${STRICT_REQUIRE_LATEST_HEAD_COPILOT_REVIEW:-0}" == "1" ]]; then
  review_filter=".author.login == \"${reviewer}\" and ((.commit.oid // \"\") == \"${head_sha}\")"
  review_error="latest-head Copilot review activity is required from ${reviewer} for head ${head_sha}."
else
  review_filter=".author.login == \"${reviewer}\""
  review_error="Copilot review activity is required from ${reviewer} before manual merge notification."
fi

review_count="$(gh pr view "${pr}" --json reviews --jq "
  [.reviews[]? | select(${review_filter})]
  | length
")"
[[ "${review_count}" -gt 0 ]] || fail "${review_error}"

repo_name="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
owner="${repo_name%%/*}"
name="${repo_name#*/}"

# shellcheck disable=SC2016
copilot_thread_summary="$(gh api graphql \
  -f query='query($owner:String!, $name:String!, $number:Int!) {
    repository(owner:$owner, name:$name) {
      pullRequest(number:$number) {
        reviewThreads(first:100) {
          pageInfo { hasNextPage }
          nodes {
            isResolved
            comments(first:100) {
              pageInfo { hasNextPage }
              nodes {
                author { login }
              }
            }
          }
        }
      }
    }
  }' \
  -F owner="${owner}" \
  -F name="${name}" \
  -F number="${pr}" \
  --jq "
    .data.repository.pullRequest.reviewThreads as \$threads
    | [
        ([
          \$threads.nodes[]?
          | select(.isResolved == false)
          | select([.comments.nodes[]?.author.login] | index(\"${reviewer}\"))
        ] | length),
        (\$threads.pageInfo.hasNextPage // false),
        ([\$threads.nodes[]? | select((.comments.pageInfo.hasNextPage // false) == true)] | length)
      ]
    | @tsv
  ")"
read -r unresolved_count has_more_threads threads_with_more_comments <<<"${copilot_thread_summary}"

[[ "${has_more_threads}" != "true" ]] || fail "Copilot review gate cannot evaluate every review thread because reviewThreads has additional pages."
[[ "${threads_with_more_comments}" == "0" ]] || fail "Copilot review gate cannot evaluate every thread comment because ${threads_with_more_comments} review thread(s) have additional comment pages."
[[ "${unresolved_count}" == "0" ]] || fail "PR has unresolved Copilot review threads from ${reviewer}: ${unresolved_count}"

printf 'Copilot review gate passed for PR %s at head %s.\n' "${pr}" "${head_sha}"
