#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

run_commit_msg() {
  local subject="$1"
  local hook="${2:-${TEST_ROOT}/scripts/hooks/commit-msg.sh}"
  local message_file="${tmp}/message.txt"
  printf '%s\n' "${subject}" > "${message_file}"
  LC_ALL=C "${hook}" "${message_file}" >/dev/null 2>"${tmp}/commit-msg.err"
}

for subject in \
  "[feat] 로그인 API 추가" \
  "[chore] 커밋 컨벤션 정리" \
  "[fix] 토큰 재사용 오류 수정" \
  "[docs] API 명세 보강" \
  "[test] 인증 회귀 테스트 추가"; do
  run_commit_msg "${subject}" || fail "expected valid commit subject: ${subject}"
done

for subject in \
  "[FEAT] : 커밋 컨벤션 정리" \
  "[feat] English only" \
  "feat: 커밋 컨벤션 정리" \
  "[feature] 커밋 컨벤션 정리" \
  "[chore]"; do
  if run_commit_msg "${subject}"; then
    fail "expected invalid commit subject: ${subject}"
  fi
done

installed_hooks="${tmp}/installed-hooks"
mkdir -p "${installed_hooks}"
ln -sf "${TEST_ROOT}/scripts/hooks/commit-msg.sh" "${installed_hooks}/commit-msg"
run_commit_msg "[chore] 설치 훅 경로 검증" "${installed_hooks}/commit-msg" \
  || fail "expected installed commit-msg symlink to validate subject"
