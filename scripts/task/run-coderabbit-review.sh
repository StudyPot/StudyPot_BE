#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
[[ -n "${pr}" ]] || fail "usage: run-coderabbit-review.sh <PR_NUMBER>"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."
command -v coderabbit >/dev/null 2>&1 || fail "coderabbit CLI is required. Install it and run 'coderabbit auth login --agent'."
command -v python3 >/dev/null 2>&1 || fail "python3 is required."

repo="$(repo_root)"
head_sha="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
base_ref="$(gh pr view "${pr}" --json baseRefName --jq .baseRefName)"

coderabbit --version >/dev/null || fail "coderabbit CLI version check failed."
coderabbit auth status --agent >/dev/null || fail "CodeRabbit agent auth is required. Run 'coderabbit auth login --agent'."

default_output() {
  local branch slug output_dir

  branch="$(git -C "${repo}" branch --show-current 2>/dev/null || true)"
  if [[ "${branch}" =~ ^codex/([a-z0-9][a-z0-9-]*)$ ]]; then
    slug="${BASH_REMATCH[1]}"
    load_task_env "${slug}"
    if [[ -n "${LOG_DIR:-}" ]]; then
      mkdir -p "${LOG_DIR}"
      printf '%s/coderabbit-review-%s.ndjson\n' "${LOG_DIR}" "${head_sha}"
      return 0
    fi
  fi

  output_dir="${repo}/.codex/coderabbit"
  mkdir -p "${output_dir}"
  printf '%s/review-%s.ndjson\n' "${output_dir}" "${head_sha}"
}

output="${STRICT_CODERABBIT_OUTPUT:-$(default_output)}"
mkdir -p "$(dirname "${output}")"

review_cmd=(coderabbit review --agent -t all --base "${base_ref}")
if [[ -f "${repo}/AGENTS.md" ]]; then
  review_cmd+=(-c AGENTS.md)
elif [[ -f "${repo}/.coderabbit.yaml" ]]; then
  review_cmd+=(-c .coderabbit.yaml)
fi

set +e
(cd "${repo}" && "${review_cmd[@]}") > "${output}"
review_status=$?
set -e
[[ "${review_status}" == "0" ]] || fail "CodeRabbit review command failed with exit ${review_status}. Output: ${output}"

parse_output="$(mktemp)"
body="$(mktemp)"
trap 'rm -f "${parse_output:-}" "${body:-}"' EXIT

CODERABBIT_OUTPUT="${output}" python3 - <<'PY' > "${parse_output}"
import json
import os
from collections import Counter

output = os.environ["CODERABBIT_OUTPUT"]
errors = []
findings = []

with open(output, encoding="utf-8") as handle:
    for line_number, line in enumerate(handle, start=1):
        line = line.strip()
        if not line:
            continue
        try:
            event = json.loads(line)
        except json.JSONDecodeError as exc:
            errors.append(f"invalid JSON line {line_number}: {exc}")
            continue

        event_type = str(event.get("type") or event.get("event") or "").lower()
        if event_type == "error" or "error" in event:
            message = event.get("message") or event.get("error") or event
            errors.append(str(message))
            continue

        if event_type == "finding" or "finding" in event:
            finding = event.get("finding") if isinstance(event.get("finding"), dict) else event
            findings.append(finding)

counts = Counter()
for finding in findings:
    severity = str(finding.get("severity") or finding.get("level") or "unknown").lower()
    if severity in {"critical", "high"}:
        counts["critical"] += 1
    elif severity in {"major", "medium"}:
        counts["major"] += 1
    elif severity in {"minor", "low", "info", "informational"}:
        counts["minor"] += 1
    else:
        counts["unknown"] += 1

print(
    "\t".join(
        str(value)
        for value in (
            len(errors),
            len(findings),
            counts["critical"],
            counts["major"],
            counts["minor"],
            counts["unknown"],
        )
    )
)
if errors:
    print(errors[0])
PY

IFS=$'\t' read -r error_count finding_count critical_count major_count minor_count unknown_count < "${parse_output}"
first_error="$(sed -n '2p' "${parse_output}")"
[[ "${error_count}" == "0" ]] || fail "CodeRabbit review returned an error: ${first_error}. Output: ${output}"

review_command_text="${review_cmd[*]}"
if [[ "${finding_count}" == "0" ]]; then
  {
    printf 'CodeRabbit Subagent Review: PASS\n'
    printf 'Head: %s\n' "${head_sha}"
    printf 'Output: %s\n' "${output}"
    printf '\n## 증거\n'
    printf -- '- 리뷰 결과: CodeRabbit raised 0 issues.\n'
    printf -- '- 검증: %s\n' "${review_command_text}"
  } > "${body}"
  gh pr comment "${pr}" --body-file "${body}"
  printf 'CodeRabbit raised 0 issues for PR %s at head %s.\n' "${pr}" "${head_sha}"
  exit 0
fi

{
  printf 'CodeRabbit Subagent Review: NEEDS_FIX\n'
  printf 'Head: %s\n' "${head_sha}"
  printf 'Output: %s\n' "${output}"
  printf '\n## 증거\n'
  printf -- '- 리뷰 결과: CodeRabbit raised %s issues. critical: %s, major: %s, minor: %s, unknown: %s\n' \
    "${finding_count}" "${critical_count}" "${major_count}" "${minor_count}" "${unknown_count}"
  printf -- '- 검증: %s\n' "${review_command_text}"
  printf -- '- 다음 단계: CodeRabbit 지적사항을 한 번 수정한 뒤 scripts/task/post-coderabbit-addressed.sh를 실행합니다.\n'
} > "${body}"
gh pr comment "${pr}" --body-file "${body}"
fail "CodeRabbit raised ${finding_count} issues. Fix them once, then post ADDRESSED evidence with post-coderabbit-addressed.sh."
