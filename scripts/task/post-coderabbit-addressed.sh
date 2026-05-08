#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
evidence_file="${2:-}"

[[ -n "${pr}" && -n "${evidence_file}" ]] || fail "usage: post-coderabbit-addressed.sh <PR_NUMBER> <evidence_file>"
[[ -f "${evidence_file}" ]] || fail "증거 파일이 없습니다: ${evidence_file}"
[[ -s "${evidence_file}" ]] || fail "증거 파일이 비어 있습니다: ${evidence_file}"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."

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

grep -Fq "## 증거" "${evidence_file}" || fail "증거 파일에 '## 증거' 섹션이 필요합니다."
require_evidence_entry "${evidence_file}" "리뷰 결과"
require_evidence_entry "${evidence_file}" "수정 범위"
require_evidence_entry "${evidence_file}" "검증"

head_sha="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
body="$(mktemp)"
trap 'rm -f "${body:-}"' EXIT
{
  printf 'CodeRabbit Subagent Review: ADDRESSED\n'
  printf 'Head: %s\n' "${head_sha}"
  printf '범위: CodeRabbit 지적사항 1회 수정 완료\n'
  printf '\n'
  sed -n '1,220p' "${evidence_file}"
} > "${body}"

gh pr comment "${pr}" --body-file "${body}"
