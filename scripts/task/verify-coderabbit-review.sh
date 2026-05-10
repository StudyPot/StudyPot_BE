#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
head_sha="${2:-}"
[[ -n "${pr}" ]] || fail "usage: verify-coderabbit-review.sh <PR_NUMBER> [HEAD_SHA]"

if [[ "${STRICT_REQUIRE_CODERABBIT_REVIEW:-1}" == "0" ]]; then
  printf 'CodeRabbit review gate skipped for PR %s.\n' "${pr}"
  exit 0
fi

command -v gh >/dev/null 2>&1 || fail "gh CLI is required."
command -v python3 >/dev/null 2>&1 || fail "python3 is required."

if [[ -z "${head_sha}" ]]; then
  head_sha="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
fi

comments_json="$(mktemp)"
trap 'rm -f "${comments_json}"' EXIT
gh pr view "${pr}" --json comments > "${comments_json}"

HEAD_SHA="${head_sha}" COMMENTS_JSON="${comments_json}" python3 - <<'PY'
import json
import os
import re
import sys

head_sha = os.environ["HEAD_SHA"]

with open(os.environ["COMMENTS_JSON"], encoding="utf-8") as handle:
    payload = json.load(handle)


def has_non_empty_label(body: str, label: str) -> bool:
    return bool(re.search(r"(?m)^\s*-\s*" + re.escape(label) + r":\s*\S", body))


accepted_markers = {
    "CodeRabbit Subagent Review: PASS": ["리뷰 결과", "검증"],
    "CodeRabbit Subagent Review: ADDRESSED": ["리뷰 결과", "수정 범위", "검증"],
}

for comment in payload.get("comments") or []:
    body = comment.get("body") or ""
    if f"Head: {head_sha}" not in body:
        continue
    if "## 증거" not in body:
        continue

    for marker, required_labels in accepted_markers.items():
        if marker not in body:
            continue
        missing = [label for label in required_labels if not has_non_empty_label(body, label)]
        if not missing:
            sys.exit(0)

print(
    f"최신 head의 CodeRabbit subagent review PASS 또는 ADDRESSED marker가 없습니다: {head_sha}",
    file=sys.stderr,
)
sys.exit(1)
PY

printf 'CodeRabbit review gate passed for PR %s at head %s.\n' "${pr}" "${head_sha}"
