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
  cat "${GH_FAKE_PR_VIEW_JSON:?GH_FAKE_PR_VIEW_JSON is required}"
  exit 0
fi

echo "unexpected gh call: $*" >&2
exit 1
STUB
chmod +x "${fake_gh}"

write_comments_json() {
  local target="$1"
  local head="$2"

  cat > "${target}" <<JSON
{
  "comments": [
    {
      "body": "CTO Architecture Gate: PASS\nHead: ${head}\n\n## 증거\n- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.\n- 아키텍처 검토: domain/API 경계를 확인했습니다.\n- 작업 분해: 구현, 테스트, 문서 변경을 함께 검토했습니다.\n- 위험: 추적되지 않은 API/DB 계약 drift는 발견하지 못했습니다."
    },
    {
      "body": "QA Verification Gate: PASS\nHead: ${head}\n\n## 증거\n- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.\n- 실행한 명령: ./gradlew check build --no-daemon\n- 검증 시나리오: happy path, validation, regression check를 확인했습니다.\n- 결과: 검토한 check가 모두 통과했습니다."
    },
    {
      "body": "Product Value Gate: PASS\nHead: ${head}\n\n## 증거\n- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.\n- 사용자 가치: 의도한 사용자 workflow를 지원합니다.\n- 리텐션 영향: 해로운 이탈 위험은 발견하지 못했습니다.\n- 범위 결정: 후속 아이디어는 이 PR 범위에서 분리했습니다."
    },
    {
      "body": "Final CTO Merge Gate: PASS\nHead: ${head}\n\n## 증거\n- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.\n- 이전 게이트 확인: CTO, QA, Product gate를 확인했습니다.\n- 미해결 스레드: final review 시점에 열린 thread가 없습니다.\n- merge 결정: merge 진행을 승인합니다."
    }
  ]
}
JSON
}

empty_comments="${tmp}/empty.json"
printf '{"comments":[]}\n' > "${empty_comments}"
if GH_FAKE_PR_VIEW_JSON="${empty_comments}" PATH="${tmp}:${PATH}" \
  "${TEST_ROOT}/scripts/task/verify-role-review-gates.sh" 7 new-head >/dev/null 2>&1; then
  fail "expected missing role gate comments to fail"
fi

old_head_comments="${tmp}/old-head.json"
write_comments_json "${old_head_comments}" "old-head"
if GH_FAKE_PR_VIEW_JSON="${old_head_comments}" PATH="${tmp}:${PATH}" \
  "${TEST_ROOT}/scripts/task/verify-role-review-gates.sh" 7 new-head >/dev/null 2>&1; then
  fail "expected stale-head role gate comments to fail"
fi

missing_evidence="${tmp}/missing-evidence.json"
cat > "${missing_evidence}" <<'JSON'
{
  "comments": [
    {
      "body": "CTO Architecture Gate: PASS\nHead: new-head\n\n## 증거\n- 사용자 결정: 추가 사용자 결정은 필요하지 않습니다.\n- 아키텍처 검토: domain/API 경계를 확인했습니다."
    }
  ]
}
JSON
if GH_FAKE_PR_VIEW_JSON="${missing_evidence}" PATH="${tmp}:${PATH}" \
  STRICT_REQUIRE_COMPANY_REVIEW_GATES="cto-architecture" \
  "${TEST_ROOT}/scripts/task/verify-role-review-gates.sh" 7 new-head >/dev/null 2>&1; then
  fail "expected incomplete role gate evidence to fail"
fi

complete_comments="${tmp}/complete.json"
write_comments_json "${complete_comments}" "new-head"
GH_FAKE_PR_VIEW_JSON="${complete_comments}" PATH="${tmp}:${PATH}" \
  "${TEST_ROOT}/scripts/task/verify-role-review-gates.sh" 7 new-head >/dev/null

STRICT_REQUIRE_COMPANY_REVIEW_GATES=0 \
  "${TEST_ROOT}/scripts/task/verify-role-review-gates.sh" 7 new-head >/dev/null
