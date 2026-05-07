#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"
# shellcheck source=scripts/task/jira-board.sh
JIRA_BOARD_SOURCE_ONLY=1 source "${SCRIPT_DIR}/jira-board.sh"

event_path="${GITHUB_EVENT_PATH:-}"
[[ -n "${event_path}" && -f "${event_path}" ]] || fail "GITHUB_EVENT_PATH must point to a pull_request event payload."

IFS=$'\t' read -r mode pr_number issue_key reason < <(
  JIRA_PROJECT_KEY="$(jira_board_project_key)" \
  python3 - "${event_path}" <<'PY'
import json
import os
import re
import sys

event_path = sys.argv[1]
project_key = os.environ["JIRA_PROJECT_KEY"]

with open(event_path, encoding="utf-8") as handle:
    event = json.load(handle)

pull_request = event.get("pull_request") or {}
number = str(pull_request.get("number") or event.get("number") or "")
merged = bool(pull_request.get("merged"))
base_ref = ((pull_request.get("base") or {}).get("ref") or "")
head_ref = ((pull_request.get("head") or {}).get("ref") or "")
title = pull_request.get("title") or ""
body = pull_request.get("body") or ""

if not merged:
    print(f"skip\t{number}\t\tpull request was closed without merge")
    raise SystemExit(0)

if base_ref != "develop":
    print(f"skip\t{number}\t\tbase branch is {base_ref}")
    raise SystemExit(0)

if not head_ref.startswith("codex/"):
    print(f"skip\t{number}\t\thead branch is {head_ref}")
    raise SystemExit(0)

explicit = re.search(r"(?im)^\s*Jira-Key:\s*`?([A-Z][A-Z0-9]+-\d+)`?\s*$", body)
if explicit:
    issue_key = explicit.group(1).upper()
else:
    pattern = re.compile(rf"\b{re.escape(project_key)}-\d+\b", re.IGNORECASE)
    match = pattern.search("\n".join([body, title, head_ref]))
    issue_key = match.group(0).upper() if match else ""

if not issue_key:
    print(f"fail\t{number}\t\tmerged codex PR does not include a Jira key")
    raise SystemExit(0)

print(f"done\t{number}\t{issue_key}\tmerged PR links to {issue_key}")
PY
)

case "${mode}" in
  skip)
    printf 'Jira auto-done skipped for PR %s: %s\n' "${pr_number:-unknown}" "${reason}"
    ;;
  fail)
    fail "Jira auto-done failed for PR ${pr_number:-unknown}: ${reason}"
    ;;
  done)
    [[ -n "${pr_number}" && -n "${issue_key}" ]] || fail "parsed PR number and Jira issue key are required."
    jira_board_mark_done_for_task "github-pr-${pr_number}" "${issue_key}"
    ;;
  *)
    fail "unknown Jira auto-done mode: ${mode}"
    ;;
esac
