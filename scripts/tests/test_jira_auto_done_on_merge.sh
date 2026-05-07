#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

auto_done_script="${TEST_ROOT}/scripts/task/mark-jira-done-from-pr.sh"
assert_file_exists "${auto_done_script}"
bash -n "${auto_done_script}"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

repo="${tmp}/repo"
jira_api="${tmp}/fake-jira-api.sh"
jira_status="${tmp}/jira-status"
jira_log="${tmp}/jira.log"
event_file="${tmp}/event.json"
setup_sandbox_repo "${repo}"
write_fake_jira_api "${jira_api}"

write_event() {
  local merged="$1"
  local base_ref="$2"
  local head_ref="$3"
  local title="$4"
  local body="$5"

  EVENT_MERGED="${merged}" \
  EVENT_BASE_REF="${base_ref}" \
  EVENT_HEAD_REF="${head_ref}" \
  EVENT_TITLE="${title}" \
  EVENT_BODY="${body}" \
  python3 - <<'PY' > "${event_file}"
import json
import os

print(json.dumps({
    "pull_request": {
        "number": 77,
        "merged": os.environ["EVENT_MERGED"] == "true",
        "base": {"ref": os.environ["EVENT_BASE_REF"]},
        "head": {"ref": os.environ["EVENT_HEAD_REF"]},
        "title": os.environ["EVENT_TITLE"],
        "body": os.environ["EVENT_BODY"],
    }
}))
PY
}

run_auto_done() {
  STRICT_REPO_ROOT="${repo}" \
  STRICT_JIRA_API_STUB="${jira_api}" \
  JIRA_FAKE_STATUS_FILE="${jira_status}" \
  JIRA_FAKE_LOG="${jira_log}" \
  GITHUB_EVENT_PATH="${event_file}" \
  "${auto_done_script}"
}

printf '진행 중\n' > "${jira_status}"
write_event true develop codex/spt-77-auto-done "[feat] 자동 완료" $'Jira-Key: SPT-77\nJira: [SPT-77](https://studypot.atlassian.net/browse/SPT-77)'
run_auto_done >/dev/null
[[ "$(cat "${jira_status}")" == "완료" ]] || fail "expected merged codex PR to move Jira to 완료"
assert_contains $'POST\t/rest/api/3/issue/SPT-77/transitions' "${jira_log}"
assert_file_exists "${repo}/.codex/task-state/github-pr-77.env"
assert_contains "JIRA_ISSUE_KEY=SPT-77" "${repo}/.codex/task-state/github-pr-77.env"

printf '진행 중\n' > "${jira_status}"
: > "${jira_log}"
write_event false develop codex/spt-77-auto-done "[feat] 자동 완료" "Jira-Key: SPT-77"
run_auto_done >/dev/null
[[ "$(cat "${jira_status}")" == "진행 중" ]] || fail "expected non-merged PR close to skip Jira transition"
assert_not_contains "/transitions" "${jira_log}"

printf '진행 중\n' > "${jira_status}"
: > "${jira_log}"
write_event true main codex/spt-77-auto-done "[feat] 자동 완료" "Jira-Key: SPT-77"
run_auto_done >/dev/null
[[ "$(cat "${jira_status}")" == "진행 중" ]] || fail "expected non-develop PR merge to skip Jira transition"
assert_not_contains "/transitions" "${jira_log}"

printf '진행 중\n' > "${jira_status}"
: > "${jira_log}"
write_event true develop feature/no-harness-key "[feat] 외부 작업" ""
run_auto_done >/dev/null
[[ "$(cat "${jira_status}")" == "진행 중" ]] || fail "expected non-codex PR merge to skip Jira transition"
assert_not_contains "/transitions" "${jira_log}"

printf '진행 중\n' > "${jira_status}"
: > "${jira_log}"
write_event true develop codex/no-jira-key "[feat] 자동 완료" ""
if run_auto_done >/dev/null 2>&1; then
  fail "expected merged codex PR without Jira key to fail"
fi
[[ "$(cat "${jira_status}")" == "진행 중" ]] || fail "missing Jira key must not transition Jira"
assert_not_contains "/transitions" "${jira_log}"
