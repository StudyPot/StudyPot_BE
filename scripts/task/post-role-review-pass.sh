#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
gate="${2:-}"
notes_file="${3:-}"

[[ -n "${pr}" && -n "${gate}" ]] || fail "usage: post-role-review-pass.sh <PR_NUMBER> <GATE> [notes_file]"
[[ -z "${notes_file}" || -f "${notes_file}" ]] || fail "notes file does not exist: ${notes_file}"
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

head_sha="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
review_marker="$(company_review_marker "${gate}")"
scope="$(company_review_scope "${gate}")"

body="$(mktemp)"
trap 'rm -f "${body:-}"' EXIT
{
  printf '%s\n' "${review_marker}"
  printf 'Head: %s\n' "${head_sha}"
  printf 'Scope: %s\n' "${scope}"
  if [[ -n "${notes_file}" ]]; then
    printf '\n## Notes\n\n'
    sed -n '1,220p' "${notes_file}"
  fi
} > "${body}"

gh pr comment "${pr}" --body-file "${body}"
