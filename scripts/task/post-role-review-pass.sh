#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
gate="${2:-}"
evidence_file="${3:-}"

[[ -n "${pr}" && -n "${gate}" ]] || fail "usage: post-role-review-pass.sh <PR_NUMBER> <GATE> <evidence_file>"
[[ -n "${evidence_file}" ]] || fail "증거 파일이 필요합니다."
[[ -f "${evidence_file}" ]] || fail "증거 파일이 없습니다: ${evidence_file}"
[[ -s "${evidence_file}" ]] || fail "증거 파일이 비어 있습니다: ${evidence_file}"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."

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

company_review_scope() {
  case "$1" in
    cto-architecture)
      printf 'CTO 아키텍처 및 작업 분해'
      ;;
    qa-verification)
      printf 'QA 브라우저/API 검증'
      ;;
    product-value)
      printf 'Product/CBO 가치 및 리텐션 검토'
      ;;
    final-cto-merge)
      printf 'Final CTO merge 승인'
      ;;
    *)
      fail "unknown company review gate: $1"
      ;;
  esac
}

require_evidence_label() {
  local file="$1"
  local label="$2"
  grep -Fq "${label}" "${file}" || fail "증거 파일에 '${label}' 항목이 필요합니다."
}

require_evidence_entry() {
  local file="$1"
  local label="$2"
  awk -v label="${label}" '
    $0 ~ "^[[:space:]]*-[[:space:]]*" label ":" {
      value = $0
      sub("^[[:space:]]*-[[:space:]]*" label ":[[:space:]]*", "", value)
      if (value ~ /[^[:space:]]/) {
        found = 1
      }
    }
    END {
      exit found ? 0 : 1
    }
  ' "${file}" || fail "증거 파일에는 비어 있지 않은 '${label}' 항목이 필요합니다."
}

validate_evidence_file() {
  local gate="$1"
  local file="$2"

  require_evidence_label "${file}" "## 증거"
  require_evidence_entry "${file}" "사용자 결정"
  case "${gate}" in
    cto-architecture)
      require_evidence_entry "${file}" "아키텍처 검토"
      require_evidence_entry "${file}" "작업 분해"
      require_evidence_entry "${file}" "위험"
      ;;
    qa-verification)
      require_evidence_entry "${file}" "실행한 명령"
      require_evidence_entry "${file}" "검증 시나리오"
      require_evidence_entry "${file}" "결과"
      ;;
    product-value)
      require_evidence_entry "${file}" "사용자 가치"
      require_evidence_entry "${file}" "리텐션 영향"
      require_evidence_entry "${file}" "범위 결정"
      ;;
    final-cto-merge)
      require_evidence_entry "${file}" "이전 게이트 확인"
      require_evidence_entry "${file}" "미해결 스레드"
      require_evidence_entry "${file}" "merge 결정"
      ;;
    *)
      fail "unknown company review gate: ${gate}"
      ;;
  esac
}

validate_evidence_file "${gate}" "${evidence_file}"
head_sha="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
review_marker="$(company_review_marker "${gate}")"
scope="$(company_review_scope "${gate}")"

body="$(mktemp)"
trap 'rm -f "${body:-}"' EXIT
{
  printf '%s\n' "${review_marker}"
  printf 'Head: %s\n' "${head_sha}"
  printf '범위: %s\n' "${scope}"
  printf '\n'
  sed -n '1,220p' "${evidence_file}"
} > "${body}"

gh pr comment "${pr}" --body-file "${body}"
