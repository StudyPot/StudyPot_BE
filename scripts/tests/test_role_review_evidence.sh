#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./testlib.sh
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
## Evidence
- Architecture Reviewed: package boundaries checked
EOF

if PATH="${tmp}:${PATH}" "${TEST_ROOT}/scripts/task/post-role-review-pass.sh" 7 cto-architecture "${bad_evidence}" >/dev/null 2>&1; then
  fail "expected incomplete CTO evidence to fail"
fi

write_evidence() {
  local gate="$1"
  local target="$2"

  case "${gate}" in
    cto-architecture)
      cat > "${target}" <<'EOF'
## Evidence
- Architecture Reviewed: domain/API boundaries checked.
- Work Breakdown: implementation, tests, and docs reviewed.
- Risks: no untracked API/DB contract drift found.
EOF
      ;;
    qa-verification)
      cat > "${target}" <<'EOF'
## Evidence
- Commands Run: ./gradlew check build --no-daemon
- Scenarios Tested: happy path, validation, and regression checks.
- Results: all reviewed checks passed.
EOF
      ;;
    product-value)
      cat > "${target}" <<'EOF'
## Evidence
- User Value: supports the intended user workflow.
- Retention Impact: no harmful churn risk identified.
- Scope Decision: follow-up ideas are separated from this PR.
EOF
      ;;
    final-cto-merge)
      cat > "${target}" <<'EOF'
## Evidence
- Prior Gates Checked: CTO, QA, and Product gates reviewed.
- Unresolved Threads: none open at final review time.
- Merge Decision: approved for merge.
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
  assert_contains "## Evidence" "${comment}"
done

assert_contains "CTO Architecture Gate: PASS" "${tmp}/cto-architecture.comment"
assert_contains "QA Verification Gate: PASS" "${tmp}/qa-verification.comment"
assert_contains "Product Value Gate: PASS" "${tmp}/product-value.comment"
assert_contains "Final CTO Merge Gate: PASS" "${tmp}/final-cto-merge.comment"
