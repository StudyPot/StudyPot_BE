#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
gate="${2:-}"
evidence_file="${3:-}"

[[ -n "${pr}" && -n "${gate}" ]] || fail "usage: post-role-review-pass.sh <PR_NUMBER> <GATE> <evidence_file>"
[[ -n "${evidence_file}" ]] || fail "evidence file is required."
[[ -f "${evidence_file}" ]] || fail "evidence file does not exist: ${evidence_file}"
[[ -s "${evidence_file}" ]] || fail "evidence file is empty: ${evidence_file}"
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
      printf 'CTO architecture and work breakdown'
      ;;
    qa-verification)
      printf 'QA browser/API verification'
      ;;
    product-value)
      printf 'Product/CBO value and retention review'
      ;;
    final-cto-merge)
      printf 'Final CTO merge approval'
      ;;
    *)
      fail "unknown company review gate: $1"
      ;;
  esac
}

require_evidence_label() {
  local file="$1"
  local label="$2"
  grep -Fq "${label}" "${file}" || fail "evidence file must include '${label}'."
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
  ' "${file}" || fail "evidence file must include a non-empty '${label}' entry."
}

validate_evidence_file() {
  local gate="$1"
  local file="$2"

  require_evidence_label "${file}" "## Evidence"
  require_evidence_entry "${file}" "User Decision"
  case "${gate}" in
    cto-architecture)
      require_evidence_entry "${file}" "Architecture Reviewed"
      require_evidence_entry "${file}" "Work Breakdown"
      require_evidence_entry "${file}" "Risks"
      ;;
    qa-verification)
      require_evidence_entry "${file}" "Commands Run"
      require_evidence_entry "${file}" "Scenarios Tested"
      require_evidence_entry "${file}" "Results"
      ;;
    product-value)
      require_evidence_entry "${file}" "User Value"
      require_evidence_entry "${file}" "Retention Impact"
      require_evidence_entry "${file}" "Scope Decision"
      ;;
    final-cto-merge)
      require_evidence_entry "${file}" "Prior Gates Checked"
      require_evidence_entry "${file}" "Unresolved Threads"
      require_evidence_entry "${file}" "Merge Decision"
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
  printf 'Scope: %s\n' "${scope}"
  printf '\n'
  sed -n '1,220p' "${evidence_file}"
} > "${body}"

gh pr comment "${pr}" --body-file "${body}"
