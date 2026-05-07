#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

repo="${tmp}/repo"
jira_api="${tmp}/fake-jira-api.sh"
jira_status="${tmp}/jira-status"
jira_log="${tmp}/jira.log"
jira_search="${tmp}/jira-search.json"
setup_sandbox_repo "${repo}"
write_fake_jira_api "${jira_api}"
printf '해야 할 일\n' > "${jira_status}"

cat > "${jira_search}" <<'JSON'
{
  "isLast": true,
  "issues": [
    {
      "key": "SPT-19",
      "fields": {
        "summary": "Spring Boot scaffold",
        "issuetype": { "name": "Task" },
        "status": { "name": "완료", "statusCategory": { "key": "done" } },
        "priority": { "name": "Medium" },
        "updated": "2026-05-01T10:00:00.000+0900",
        "labels": ["foundation"]
      }
    },
    {
      "key": "SPT-20",
      "fields": {
        "summary": "Auth API implementation",
        "issuetype": { "name": "Task" },
        "status": { "name": "진행 중", "statusCategory": { "key": "indeterminate" } },
        "priority": { "name": "Medium" },
        "updated": "2026-05-02T10:00:00.000+0900",
        "labels": ["auth"]
      }
    },
    {
      "key": "SPT-21",
      "fields": {
        "summary": "Group creation flow",
        "issuetype": { "name": "Task" },
        "status": { "name": "해야 할 일", "statusCategory": { "key": "new" } },
        "priority": { "name": "High" },
        "updated": "2026-05-03T10:00:00.000+0900",
        "labels": ["group"]
      }
    },
    {
      "key": "SPT-22",
      "fields": {
        "summary": "Invitation edge cases",
        "issuetype": { "name": "작업" },
        "status": { "name": "해야 할 일", "statusCategory": { "key": "new" } },
        "priority": { "name": "Low" },
        "updated": "2026-05-04T10:00:00.000+0900",
        "labels": ["invite"]
      }
    },
    {
      "key": "SPT-23",
      "fields": {
        "summary": "Epic tracking only",
        "issuetype": { "name": "Epic" },
        "status": { "name": "해야 할 일", "statusCategory": { "key": "new" } },
        "priority": { "name": "Highest" },
        "updated": "2026-05-05T10:00:00.000+0900",
        "labels": ["epic"]
      }
    }
  ]
}
JSON

output="$(
  STRICT_REPO_ROOT="${repo}" \
  STRICT_JIRA_API_STUB="${jira_api}" \
  JIRA_FAKE_STATUS_FILE="${jira_status}" \
  JIRA_FAKE_LOG="${jira_log}" \
  JIRA_FAKE_SEARCH_FILE="${jira_search}" \
  "${TEST_ROOT}/scripts/task/jira-board.sh" recommend-next --limit 3
)"

printf '%s\n' "${output}" > "${tmp}/output.md"
assert_contains "Jira Next Work Recommendations" "${tmp}/output.md"
assert_contains "Read: \`5\` Jira issues" "${tmp}/output.md"
assert_contains "Done: \`1\`" "${tmp}/output.md"
assert_contains "In Progress: \`1\`" "${tmp}/output.md"
assert_contains "To Do: \`3\`" "${tmp}/output.md"
assert_contains "Recent Done Context" "${tmp}/output.md"
assert_contains $'POST\t/rest/api/3/search/jql' "${jira_log}"

first="$(awk '/^1\. / { print; exit }' "${tmp}/output.md")"
second="$(awk '/^2\. / { print; exit }' "${tmp}/output.md")"
third="$(awk '/^3\. / { print; exit }' "${tmp}/output.md")"

[[ "${first}" == *"SPT-20"* ]] || fail "expected in-progress task first, got: ${first}"
[[ "${second}" == *"SPT-21"* ]] || fail "expected high-priority todo task second, got: ${second}"
[[ "${third}" == *"SPT-22"* ]] || fail "expected remaining todo task third, got: ${third}"

recommended_section="$(awk '/^### Recommended Next / { in_section=1; next } /^### Recent Done Context/ { in_section=0 } in_section { print }' "${tmp}/output.md")"
[[ "${recommended_section}" != *"SPT-19"* ]] || fail "expected done issue to be excluded from recommendations"
[[ "${recommended_section}" != *"SPT-23"* ]] || fail "expected non-task issue to be excluded from recommendations"
