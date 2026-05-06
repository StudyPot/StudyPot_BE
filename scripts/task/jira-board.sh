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

jira_board_search_project_json() {
  local project_key="$1"
  local tmp_dir next_token page_index body_file page_file is_last

  tmp_dir="$(mktemp -d)"
  next_token=""
  page_index=0

  while :; do
    body_file="${tmp_dir}/body-${page_index}.json"
    JIRA_SEARCH_PROJECT="${project_key}" \
    JIRA_NEXT_PAGE_TOKEN="${next_token}" \
    JIRA_SEARCH_PAGE_SIZE="${STRICT_JIRA_SEARCH_PAGE_SIZE:-100}" \
    python3 - <<'PY' > "${body_file}"
import json
import os

body = {
    "jql": f"project = {os.environ['JIRA_SEARCH_PROJECT']} ORDER BY key ASC",
    "maxResults": int(os.environ["JIRA_SEARCH_PAGE_SIZE"]),
    "fields": [
        "summary",
        "status",
        "issuetype",
        "priority",
        "labels",
        "assignee",
        "created",
        "updated",
    ],
}
next_page_token = os.environ.get("JIRA_NEXT_PAGE_TOKEN", "")
if next_page_token:
    body["nextPageToken"] = next_page_token
print(json.dumps(body, ensure_ascii=False))
PY

    page_file="${tmp_dir}/page-${page_index}.json"
    jira_board_request POST "/rest/api/3/search/jql" "${body_file}" > "${page_file}"

    next_token="$(python3 -c 'import json, sys; print((json.load(sys.stdin).get("nextPageToken") or ""))' < "${page_file}")"
    is_last="$(python3 -c 'import json, sys; doc=json.load(sys.stdin); print("1" if doc.get("isLast", True) else "0")' < "${page_file}")"
    page_index=$((page_index + 1))

    [[ "${is_last}" == "1" || -z "${next_token}" ]] && break
  done

  python3 - "${tmp_dir}"/page-*.json <<'PY'
import json
import sys

issues = []
for path in sys.argv[1:]:
    with open(path, encoding="utf-8") as handle:
        page = json.load(handle)
    issues.extend(page.get("issues") or [])
print(json.dumps({"issues": issues}, ensure_ascii=False))
PY
  rm -rf "${tmp_dir}"
}

jira_board_recommend_next() {
  local limit="3"
  local arg project_key issues_json issues_file

  while [[ "$#" -gt 0 ]]; do
    arg="$1"
    case "${arg}" in
      --limit)
        limit="${2:-}"
        [[ -n "${limit}" ]] || fail "--limit requires a positive number."
        shift 2
        ;;
      *)
        fail "usage: jira-board.sh recommend-next [--limit 3]"
        ;;
    esac
  done

  [[ "${limit}" =~ ^[0-9]+$ && "${limit}" -gt 0 ]] || fail "--limit must be a positive number."

  project_key="$(jira_board_project_key)"
  issues_json="$(jira_board_search_project_json "${project_key}")"
  issues_file="$(mktemp)"
  printf '%s\n' "${issues_json}" > "${issues_file}"

  JIRA_ISSUES_JSON="${issues_file}" \
  JIRA_RECOMMEND_LIMIT="${limit}" \
  JIRA_PROJECT_KEY="${project_key}" \
  JIRA_BASE_URL="$(jira_board_base_url)" \
  JIRA_STATUS_TODO="$(jira_board_status_todo)" \
  JIRA_STATUS_IN_PROGRESS="$(jira_board_status_in_progress)" \
  JIRA_STATUS_DONE="$(jira_board_status_done)" \
  JIRA_ALLOWED_ISSUE_TYPES="${STRICT_JIRA_ALLOWED_ISSUE_TYPES:-작업,Task}" \
  python3 - <<'PY'
import json
import os
import re
from datetime import datetime

with open(os.environ["JIRA_ISSUES_JSON"], encoding="utf-8") as handle:
    doc = json.load(handle)

issues = doc.get("issues") or []
limit = int(os.environ["JIRA_RECOMMEND_LIMIT"])
project_key = os.environ["JIRA_PROJECT_KEY"]
base_url = os.environ["JIRA_BASE_URL"].rstrip("/")
todo_status = os.environ["JIRA_STATUS_TODO"]
in_progress_status = os.environ["JIRA_STATUS_IN_PROGRESS"]
done_status = os.environ["JIRA_STATUS_DONE"]
allowed_types = {
    value.strip()
    for value in os.environ["JIRA_ALLOWED_ISSUE_TYPES"].split(",")
    if value.strip()
}


def clean(value):
    return str(value or "").replace("\n", " ").strip()


def fields(issue):
    return issue.get("fields") or {}


def issue_key(issue):
    return clean(issue.get("key"))


def key_number(issue):
    match = re.search(r"-(\d+)$", issue_key(issue))
    return int(match.group(1)) if match else 10**9


def issue_summary(issue):
    return clean(fields(issue).get("summary"))


def issue_type(issue):
    return clean((fields(issue).get("issuetype") or {}).get("name"))


def status_name(issue):
    return clean((fields(issue).get("status") or {}).get("name"))


def status_category(issue):
    status = fields(issue).get("status") or {}
    category = status.get("statusCategory") or {}
    return clean(category.get("key") or category.get("name")).lower()


def bucket(issue):
    status = status_name(issue)
    category = status_category(issue)
    if status == done_status or category in {"done", "complete", "completed"}:
        return "done"
    if status == in_progress_status or category in {"indeterminate", "in progress"}:
        return "in_progress"
    if status == todo_status or category in {"new", "to do", "todo"}:
        return "todo"
    return "other_open"


def priority_rank(issue):
    priority = clean((fields(issue).get("priority") or {}).get("name")).lower()
    order = {
        "highest": 0,
        "high": 1,
        "medium": 2,
        "low": 3,
        "lowest": 4,
    }
    return order.get(priority, 5)


def updated_at(issue):
    raw = clean(fields(issue).get("updated") or fields(issue).get("created"))
    if not raw:
        return datetime.min
    normalized = raw.replace("Z", "+0000")
    for fmt in ("%Y-%m-%dT%H:%M:%S.%f%z", "%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%d"):
        try:
            return datetime.strptime(normalized, fmt).replace(tzinfo=None)
        except ValueError:
            pass
    return datetime.min


def slug_for(issue):
    key = issue_key(issue).lower()
    summary_words = re.findall(r"[a-z0-9]+", issue_summary(issue).lower())
    suffix = "-".join(summary_words[:4])
    return f"{key}-{suffix}" if suffix else key


def shell_double_quoted(value):
    return clean(value).replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$").replace("`", "\\`")


stats = {"done": 0, "in_progress": 0, "todo": 0, "other_open": 0}
for issue in issues:
    stats[bucket(issue)] += 1

candidates = [
    issue
    for issue in issues
    if issue_type(issue) in allowed_types and bucket(issue) != "done"
]

rank_order = {"in_progress": 0, "todo": 1, "other_open": 2}
candidates.sort(
    key=lambda issue: (
        rank_order.get(bucket(issue), 9),
        priority_rank(issue),
        key_number(issue),
    )
)

done_issues = [issue for issue in issues if bucket(issue) == "done"]
done_issues.sort(key=lambda issue: (updated_at(issue), key_number(issue)), reverse=True)
recommendations = candidates[:limit]

print("## Jira Next Work Recommendations")
print()
print(f"- Project: `{project_key}`")
print(f"- Read: `{len(issues)}` Jira issues")
print(f"- Done: `{stats['done']}`")
print(f"- In Progress: `{stats['in_progress']}`")
print(f"- To Do: `{stats['todo']}`")
print(f"- Other Open: `{stats['other_open']}`")
print()

if not recommendations:
    print("No unfinished implementation tasks were found.")
else:
    print(f"### Recommended Next {len(recommendations)}")
    for index, issue in enumerate(recommendations, start=1):
        key = issue_key(issue)
        status = status_name(issue)
        summary = issue_summary(issue)
        why = {
            "in_progress": "Already in progress; finish this before opening new work.",
            "todo": "Unstarted implementation task; good next candidate after completed work.",
            "other_open": "Not done yet; inspect status before starting.",
        }.get(bucket(issue), "Not done yet.")
        print(f"{index}. `{key}` - {summary}")
        print(f"   - Status: `{status}`")
        print(f"   - URL: {base_url}/browse/{key}")
        print(f"   - Why: {why}")
        print(f"   - Start: `scripts/task/init-task.sh {slug_for(issue)} \"{shell_double_quoted(summary)}\" --jira {key}`")
    print()

if done_issues:
    print("### Recent Done Context")
    for issue in done_issues[:5]:
        print(f"- `{issue_key(issue)}` - {issue_summary(issue)}")
PY
  rm -f "${issues_file}"
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
  recommend-next)
    shift
    jira_board_recommend_next "$@"
    ;;
  *)
    fail "usage: jira-board.sh {issue-url|validate-start|start-task|done-task|recommend-next} ..."
    ;;
esac
