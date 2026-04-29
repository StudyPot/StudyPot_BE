#!/usr/bin/env bash

set -euo pipefail

JIRA_BOARD_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./common.sh
source "${JIRA_BOARD_DIR}/common.sh"

jira_board_base_url() {
  local base="${STRICT_JIRA_BASE_URL:-https://studypot.atlassian.net}"
  printf '%s\n' "${base%/}"
}

jira_board_project_key() {
  printf '%s\n' "${STRICT_JIRA_PROJECT_KEY:-SPT}"
}

jira_board_status_todo() {
  printf '%s\n' "${STRICT_JIRA_STATUS_TODO:-해야 할 일}"
}

jira_board_status_in_progress() {
  printf '%s\n' "${STRICT_JIRA_STATUS_IN_PROGRESS:-진행 중}"
}

jira_board_status_done() {
  printf '%s\n' "${STRICT_JIRA_STATUS_DONE:-완료}"
}

jira_board_issue_url() {
  local issue_key="$1"
  printf '%s/browse/%s\n' "$(jira_board_base_url)" "${issue_key}"
}

jira_board_require_runtime() {
  command -v python3 >/dev/null 2>&1 || fail "python3 is required for Jira JSON parsing."
  if [[ -z "${STRICT_JIRA_API_STUB:-}" ]]; then
    command -v curl >/dev/null 2>&1 || fail "curl is required for Jira REST calls."
    [[ -n "${JIRA_EMAIL:-}" ]] || fail "JIRA_EMAIL is required."
    [[ -n "${JIRA_API_TOKEN:-}" ]] || fail "JIRA_API_TOKEN is required."
  fi
}

jira_board_request() {
  local method="$1"
  local path="$2"
  local body_file="${3:-}"
  local url

  jira_board_require_runtime
  if [[ -n "${STRICT_JIRA_API_STUB:-}" ]]; then
    "${STRICT_JIRA_API_STUB}" "${method}" "${path}" "${body_file}"
    return
  fi

  url="$(jira_board_base_url)${path}"
  if [[ -n "${body_file}" ]]; then
    curl -fsS \
      -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
      -X "${method}" \
      -H 'Accept: application/json' \
      -H 'Content-Type: application/json' \
      --data @"${body_file}" \
      "${url}"
  else
    curl -fsS \
      -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
      -X "${method}" \
      -H 'Accept: application/json' \
      "${url}"
  fi
}

jira_board_issue_details() {
  local issue_key="$1"
  local json

  json="$(jira_board_request GET "/rest/api/3/issue/${issue_key}?fields=summary,issuetype,status,project")"
  python3 -c '
import json, sys

def clean(value):
    if value is None:
        return ""
    return str(value).replace("\t", " ").replace("\n", " ")

doc = json.load(sys.stdin)
fields = doc.get("fields") or {}
project = fields.get("project") or {}
issue_type = fields.get("issuetype") or {}
status = fields.get("status") or {}
print("\t".join([
    clean(doc.get("key")),
    clean(project.get("key")),
    clean(fields.get("summary")),
    clean(issue_type.get("name")),
    clean(status.get("name")),
]))
' <<<"${json}"
}

jira_board_transition_id_for_status() {
  local issue_key="$1"
  local target_status="$2"
  local json

  json="$(jira_board_request GET "/rest/api/3/issue/${issue_key}/transitions")"
  JIRA_TARGET_STATUS="${target_status}" python3 -c '
import json, os, sys

target = os.environ["JIRA_TARGET_STATUS"]
doc = json.load(sys.stdin)
for transition in doc.get("transitions", []):
    to_status = (transition.get("to") or {}).get("name")
    if to_status == target:
        print(transition.get("id", ""))
        raise SystemExit(0)
raise SystemExit(1)
' <<<"${json}"
}

jira_board_transition_to_status() {
  local issue_key="$1"
  local target_status="$2"
  local transition_id tmp_body

  transition_id="$(jira_board_transition_id_for_status "${issue_key}" "${target_status}")" \
    || fail "Jira transition target not available for ${issue_key}: ${target_status}"

  tmp_body="$(mktemp)"
  printf '{"transition":{"id":"%s"}}\n' "${transition_id}" > "${tmp_body}"
  jira_board_request POST "/rest/api/3/issue/${issue_key}/transitions" "${tmp_body}" >/dev/null
  rm -f "${tmp_body}"
}

jira_board_validate_issue_for_task() {
  local issue_key="$1"
  local mode="${2:-start}"
  local expected_project allowed_types todo_status in_progress_status done_status
  local key project summary issue_type status

  expected_project="$(jira_board_project_key)"
  [[ "${issue_key}" == "${expected_project}-"* ]] || fail "Jira issue must be in ${expected_project}: ${issue_key}"

  IFS=$'\t' read -r key project summary issue_type status < <(jira_board_issue_details "${issue_key}")
  [[ "${key}" == "${issue_key}" ]] || fail "Jira issue key mismatch: expected ${issue_key}, got ${key}"
  [[ "${project}" == "${expected_project}" ]] || fail "Jira issue project mismatch: expected ${expected_project}, got ${project}"

  allowed_types=",${STRICT_JIRA_ALLOWED_ISSUE_TYPES:-작업,Task},"
  [[ "${allowed_types}" == *",${issue_type},"* ]] || fail "Jira issue type must be one of ${allowed_types#,}: ${issue_type}"

  todo_status="$(jira_board_status_todo)"
  in_progress_status="$(jira_board_status_in_progress)"
  done_status="$(jira_board_status_done)"

  case "${mode}" in
    start)
      [[ "${status}" != "${done_status}" ]] || fail "Jira issue is already done: ${issue_key}"
      case "${status}" in
        "${todo_status}")
          jira_board_transition_id_for_status "${issue_key}" "${in_progress_status}" >/dev/null \
            || fail "Jira issue cannot transition to ${in_progress_status}: ${issue_key}"
          ;;
        "${in_progress_status}")
          ;;
        *)
          fail "Jira issue must be ${todo_status} or ${in_progress_status} to start: ${issue_key} is ${status}"
          ;;
      esac
      ;;
    done)
      if [[ "${status}" != "${done_status}" ]]; then
        jira_board_transition_id_for_status "${issue_key}" "${done_status}" >/dev/null \
          || fail "Jira issue cannot transition to ${done_status}: ${issue_key}"
      fi
      ;;
    *)
      fail "unknown Jira validation mode: ${mode}"
      ;;
  esac

  printf '%s\t%s\t%s\t%s\t%s\n' "${key}" "${project}" "${summary}" "${issue_type}" "${status}"
}

jira_board_validate_start() {
  local issue_key="$1"
  jira_board_validate_issue_for_task "${issue_key}" start >/dev/null
}

jira_board_mark_started_for_task() {
  local slug="$1"
  local issue_key="$2"
  local key project summary issue_type status in_progress_status

  IFS=$'\t' read -r key project summary issue_type status < <(jira_board_validate_issue_for_task "${issue_key}" start)
  in_progress_status="$(jira_board_status_in_progress)"
  if [[ "${status}" != "${in_progress_status}" ]]; then
    jira_board_transition_to_status "${issue_key}" "${in_progress_status}"
  fi

  load_task_env "${slug}"
  JIRA_ISSUE_KEY="${issue_key}"
  JIRA_ISSUE_URL="$(jira_board_issue_url "${issue_key}")"
  JIRA_ISSUE_SUMMARY="${summary}"
  JIRA_STARTED_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  write_task_env "${slug}"

  printf 'Jira %s is in progress: %s\n' "${JIRA_ISSUE_KEY}" "${JIRA_ISSUE_URL}"
}

jira_board_mark_done_for_task() {
  local slug="$1"
  local issue_key="${2:-}"
  local key project summary issue_type status done_status

  load_task_env "${slug}"
  [[ -n "${issue_key}" ]] || issue_key="${JIRA_ISSUE_KEY:-}"
  [[ -n "${issue_key}" ]] || fail "JIRA_ISSUE_KEY is required to mark Jira done."

  IFS=$'\t' read -r key project summary issue_type status < <(jira_board_validate_issue_for_task "${issue_key}" done)
  done_status="$(jira_board_status_done)"
  if [[ "${status}" != "${done_status}" ]]; then
    jira_board_transition_to_status "${issue_key}" "${done_status}"
  fi

  JIRA_ISSUE_KEY="${issue_key}"
  JIRA_ISSUE_URL="$(jira_board_issue_url "${issue_key}")"
  JIRA_ISSUE_SUMMARY="${summary}"
  JIRA_DONE_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  write_task_env "${slug}"

  printf 'Jira %s is done: %s\n' "${JIRA_ISSUE_KEY}" "${JIRA_ISSUE_URL}"
}

if [[ "${JIRA_BOARD_SOURCE_ONLY:-0}" == "1" ]]; then
  return 0 2>/dev/null || exit 0
fi

cmd="${1:-}"
case "${cmd}" in
  issue-url)
    [[ -n "${2:-}" ]] || fail "usage: jira-board.sh issue-url <ISSUE_KEY>"
    jira_board_issue_url "$2"
    ;;
  validate-start)
    [[ -n "${2:-}" ]] || fail "usage: jira-board.sh validate-start <ISSUE_KEY>"
    jira_board_validate_start "$2"
    ;;
  start-task)
    [[ -n "${2:-}" && -n "${3:-}" ]] || fail "usage: jira-board.sh start-task <slug> <ISSUE_KEY>"
    jira_board_mark_started_for_task "$2" "$3"
    ;;
  done-task)
    [[ -n "${2:-}" ]] || fail "usage: jira-board.sh done-task <slug> [ISSUE_KEY]"
    jira_board_mark_done_for_task "$2" "${3:-}"
    ;;
  *)
    fail "usage: jira-board.sh {issue-url|validate-start|start-task|done-task} ..."
    ;;
esac
