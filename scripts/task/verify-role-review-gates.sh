#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
head_sha="${2:-}"
required_company_gates="${STRICT_REQUIRE_COMPANY_REVIEW_GATES:-cto-architecture qa-verification product-value final-cto-merge}"

[[ -n "${pr}" && -n "${head_sha}" ]] || fail "usage: verify-role-review-gates.sh <PR_NUMBER> <HEAD_SHA>"

if [[ "${required_company_gates}" == "0" || "${required_company_gates}" == "none" || -z "${required_company_gates}" ]]; then
  printf 'company role review gates disabled for PR %s.\n' "${pr}"
  exit 0
fi

command -v gh >/dev/null 2>&1 || fail "gh CLI is required."
command -v python3 >/dev/null 2>&1 || fail "python3 is required."

company_review_marker() {
  case "$1" in
    cto-architecture)
      printf 'CTO Architecture Gate: PASS'
      ;;
    qa-verification)
      printf 'QA Verification Gate: PASS'
      ;;
    product-value)
      printf 'Product Value Gate: PASS'
      ;;
    final-cto-merge)
      printf 'Final CTO Merge Gate: PASS'
      ;;
    *)
      fail "unknown company review gate: $1"
      ;;
  esac
}

company_review_required_labels() {
  case "$1" in
    cto-architecture)
      printf '%s\n' "사용자 결정" "아키텍처 검토" "작업 분해" "위험"
      ;;
    qa-verification)
      printf '%s\n' "사용자 결정" "실행한 명령" "검증 시나리오" "결과"
      ;;
    product-value)
      printf '%s\n' "사용자 결정" "사용자 가치" "리텐션 영향" "범위 결정"
      ;;
    final-cto-merge)
      printf '%s\n' "사용자 결정" "이전 게이트 확인" "미해결 스레드" "merge 결정"
      ;;
    *)
      fail "unknown company review gate: $1"
      ;;
  esac
}

comments_json="$(mktemp)"
trap 'rm -f "${comments_json}"' EXIT
gh pr view "${pr}" --json comments > "${comments_json}"

for gate in ${required_company_gates}; do
  marker="$(company_review_marker "${gate}")"
  required_labels="$(company_review_required_labels "${gate}")"

  HEAD_SHA="${head_sha}" \
    MARKER="${marker}" \
    REQUIRED_LABELS="${required_labels}" \
    COMMENTS_JSON="${comments_json}" \
    python3 - <<'PY'
import json
import os
import re
import sys

head_sha = os.environ["HEAD_SHA"]
marker = os.environ["MARKER"]
required_labels = [label for label in os.environ["REQUIRED_LABELS"].splitlines() if label]

with open(os.environ["COMMENTS_JSON"], encoding="utf-8") as handle:
    payload = json.load(handle)

for comment in payload.get("comments") or []:
    body = comment.get("body") or ""
    if marker not in body:
        continue
    if f"Head: {head_sha}" not in body:
        continue
    if "## 증거" not in body:
        continue
    missing = [
        label
        for label in required_labels
        if not re.search(r"(?m)^\s*-\s*" + re.escape(label) + r":\s*\S", body)
    ]
    if not missing:
        sys.exit(0)

print(
    f"최신 head의 증거 포함 company review gate pass marker가 없습니다: {marker}",
    file=sys.stderr,
)
sys.exit(1)
PY
done

printf 'company role review gates passed for PR %s at head %s.\n' "${pr}" "${head_sha}"
