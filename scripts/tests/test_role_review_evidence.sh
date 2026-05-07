#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

fake_gh="${tmp}/gh"
cat > "${fake_gh}" <<'STUB'
#!/usr/bin/env bash

set -euo pipefail

if [[ "$1" == "pr" && "$2" == "view" ]]; then
  printf 'fake-head-sha\n'
  exit 0
fi

if [[ "$1" == "pr" && "$2" == "comment" ]]; then
  body_file=""
  while [[ "$#" -gt 0 ]]; do
    case "$1" in
      --body-file)
        body_file="${2:-}"
        shift 2
        ;;
      *)
        shift
        ;;
    esac
  done
  [[ -n "${body_file}" && -f "${body_file}" ]] || exit 1
  cp "${body_file}" "${GH_FAKE_COMMENT_FILE:?GH_FAKE_COMMENT_FILE is required}"
  exit 0
fi

echo "unexpected gh call: $*" >&2
exit 1
STUB
chmod +x "${fake_gh}"

if PATH="${tmp}:${PATH}" "${TEST_ROOT}/scripts/task/post-role-review-pass.sh" 7 cto-architecture >/dev/null 2>&1; then
  fail "expected role review marker posting to require evidence"
fi

bad_evidence="${tmp}/bad.md"
cat > "${bad_evidence}" <<'EOF'
## 증거
- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.
- 아키텍처 검토: package boundary 확인
EOF

if PATH="${tmp}:${PATH}" "${TEST_ROOT}/scripts/task/post-role-review-pass.sh" 7 cto-architecture "${bad_evidence}" >/dev/null 2>&1; then
  fail "expected incomplete CTO evidence to fail"
fi

empty_evidence="${tmp}/empty.md"
cat > "${empty_evidence}" <<'EOF'
## 증거
- 사용자 결정:
- 아키텍처 검토:
- 작업 분해:
- 위험:
EOF

if PATH="${tmp}:${PATH}" "${TEST_ROOT}/scripts/task/post-role-review-pass.sh" 7 cto-architecture "${empty_evidence}" >/dev/null 2>&1; then
  fail "expected empty CTO evidence entries to fail"
fi

write_evidence() {
  local gate="$1"
  local target="$2"

  case "${gate}" in
    cto-architecture)
      cat > "${target}" <<'EOF'
## 증거
- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.
- 아키텍처 검토: domain/API 경계를 확인했습니다.
- 작업 분해: 구현, 테스트, 문서 변경을 함께 검토했습니다.
- 위험: 추적되지 않은 API/DB 계약 drift는 발견하지 못했습니다.
EOF
      ;;
    qa-verification)
      cat > "${target}" <<'EOF'
## 증거
- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.
- 실행한 명령: ./gradlew check build --no-daemon
- 검증 시나리오: happy path, validation, regression check를 확인했습니다.
- 결과: 검토한 check가 모두 통과했습니다.
EOF
      ;;
    product-value)
      cat > "${target}" <<'EOF'
## 증거
- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.
- 사용자 가치: 의도한 사용자 workflow를 지원합니다.
- 리텐션 영향: 해로운 이탈 위험은 발견하지 못했습니다.
- 범위 결정: 후속 아이디어는 이 PR 범위에서 분리했습니다.
EOF
      ;;
    final-cto-merge)
      cat > "${target}" <<'EOF'
## 증거
- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.
- 이전 게이트 확인: CTO, QA, Product gate를 확인했습니다.
- 미해결 스레드: final review 시점에 열린 thread가 없습니다.
- merge 결정: merge 진행을 승인합니다.
EOF
      ;;
    *)
      fail "unknown test gate: ${gate}"
      ;;
  esac
}

for gate in cto-architecture qa-verification product-value final-cto-merge; do
  evidence="${tmp}/${gate}.md"
  comment="${tmp}/${gate}.comment"
  write_evidence "${gate}" "${evidence}"
  GH_FAKE_COMMENT_FILE="${comment}" PATH="${tmp}:${PATH}" \
    "${TEST_ROOT}/scripts/task/post-role-review-pass.sh" 7 "${gate}" "${evidence}" >/dev/null
  assert_contains "Head: fake-head-sha" "${comment}"
  assert_contains "## 증거" "${comment}"
  assert_contains "범위:" "${comment}"
done

assert_contains "CTO Architecture Gate: PASS" "${tmp}/cto-architecture.comment"
assert_contains "QA Verification Gate: PASS" "${tmp}/qa-verification.comment"
assert_contains "Product Value Gate: PASS" "${tmp}/product-value.comment"
assert_contains "Final CTO Merge Gate: PASS" "${tmp}/final-cto-merge.comment"
