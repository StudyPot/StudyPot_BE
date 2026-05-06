#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
round="${2:-}"
notes_file="${3:-}"

[[ -n "${pr}" && -n "${round}" ]] || fail "usage: post-subagent-review-pass.sh <PR_NUMBER> <ROUND: 1|2|3> [notes_file]"
[[ "${round}" =~ ^[1-3]$ ]] || fail "round must be 1, 2, or 3."
[[ -z "${notes_file}" || -f "${notes_file}" ]] || fail "notes file does not exist: ${notes_file}"
command -v gh >/dev/null 2>&1 || fail "gh CLI is required."

review_scope() {
  case "$1" in
    1)
      printf 'flexible architecture and direction review'
      ;;
    2)
      printf 'focused fix verification review'
      ;;
    3)
      printf 'strict final merge-readiness review'
      ;;
    *)
      fail "unknown Codex subagent review round: $1"
      ;;
  esac
}

head_sha="$(gh pr view "${pr}" --json headRefOid --jq .headRefOid)"
scope="$(review_scope "${round}")"
marker="Codex Subagent Review Round ${round}: PASS"

body="$(mktemp)"
trap 'rm -f "${body:-}"' EXIT
{
  printf '%s\n' "${marker}"
  printf 'Head: %s\n' "${head_sha}"
  printf 'Scope: %s\n' "${scope}"
  if [[ -n "${notes_file}" ]]; then
    printf '\n## Notes\n\n'
    sed -n '1,220p' "${notes_file}"
  fi
} > "${body}"

gh pr comment "${pr}" --body-file "${body}"
