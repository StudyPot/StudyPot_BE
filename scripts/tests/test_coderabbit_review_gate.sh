#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

verify_script="${TEST_ROOT}/scripts/task/verify-coderabbit-review.sh"
run_script="${TEST_ROOT}/scripts/task/run-coderabbit-review.sh"
post_script="${TEST_ROOT}/scripts/task/post-coderabbit-addressed.sh"

for script in "${verify_script}" "${run_script}" "${post_script}"; do
  assert_file_exists "${script}"
  bash -n "${script}"
done

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

fake_gh="${tmp}/gh"
cat > "${fake_gh}" <<'STUB'
#!/usr/bin/env bash

set -euo pipefail

apply_jq() {
  local jq_filter="$1"
  if [[ -n "${jq_filter}" ]]; then
    jq -r "${jq_filter}"
  else
    cat
  fi
}

jq_filter=""
body_file=""
args=("$@")
for ((i = 0; i < ${#args[@]}; i++)); do
  case "${args[$i]}" in
    --jq)
      jq_filter="${args[$((i + 1))]}"
      ;;
    --body-file)
      body_file="${args[$((i + 1))]}"
      ;;
  esac
done

if [[ "$1" == "pr" && "$2" == "view" ]]; then
  if [[ "$*" == *"headRefOid"* ]]; then
    printf '{"headRefOid":"%s"}\n' "${FAKE_HEAD_SHA:-new-head}" | apply_jq "${jq_filter}"
    exit 0
  fi
  if [[ "$*" == *"baseRefName"* ]]; then
    printf '{"baseRefName":"%s"}\n' "${FAKE_BASE_REF:-develop}" | apply_jq "${jq_filter}"
    exit 0
  fi
  if [[ "$*" == *"comments"* ]]; then
    cat "${GH_FAKE_PR_VIEW_JSON:?GH_FAKE_PR_VIEW_JSON is required}" | apply_jq "${jq_filter}"
    exit 0
  fi
fi

if [[ "$1" == "pr" && "$2" == "comment" ]]; then
  [[ -n "${body_file}" ]] || {
    echo "missing --body-file" >&2
    exit 1
  }
  cp "${body_file}" "${FAKE_COMMENT_CAPTURE:?FAKE_COMMENT_CAPTURE is required}"
  exit 0
fi

echo "unexpected gh call: $*" >&2
exit 1
STUB
chmod +x "${fake_gh}"

fake_coderabbit="${tmp}/coderabbit"
cat > "${fake_coderabbit}" <<'STUB'
#!/usr/bin/env bash

set -euo pipefail

if [[ "${1:-}" == "--version" ]]; then
  printf 'coderabbit 1.0.0\n'
  exit 0
fi

if [[ "${1:-}" == "auth" && "${2:-}" == "status" && "${3:-}" == "--agent" ]]; then
  exit 0
fi

if [[ "${1:-}" == "review" ]]; then
  printf '%s\n' "$*" > "${FAKE_CODERABBIT_ARGS_LOG:?FAKE_CODERABBIT_ARGS_LOG is required}"
  case "${FAKE_CODERABBIT_MODE:-pass}" in
    pass)
      printf '{"type":"status","message":"complete"}\n'
      ;;
    finding)
      printf '{"type":"finding","severity":"major","file_path":"scripts/task/example.sh","message":"example issue"}\n'
      ;;
    error)
      printf '{"type":"error","message":"authentication failed"}\n'
      ;;
    *)
      echo "unknown fake mode" >&2
      exit 2
      ;;
  esac
  exit 0
fi

echo "unexpected coderabbit call: $*" >&2
exit 1
STUB
chmod +x "${fake_coderabbit}"

write_comments_json() {
  local target="$1"
  local body="$2"

  jq -n --arg body "${body}" '{comments: [{body: $body}]}' > "${target}"
}

empty_comments="${tmp}/empty.json"
printf '{"comments":[]}\n' > "${empty_comments}"
if GH_FAKE_PR_VIEW_JSON="${empty_comments}" PATH="${tmp}:${PATH}" \
  "${verify_script}" 7 new-head >/dev/null 2>&1; then
  fail "expected missing CodeRabbit review marker to fail"
fi

pass_old="${tmp}/pass-old.json"
write_comments_json "${pass_old}" $'CodeRabbit Subagent Review: PASS\nHead: old-head\n\n## 증거\n- 리뷰 결과: CodeRabbit raised 0 issues.\n- 검증: coderabbit review --agent'
if GH_FAKE_PR_VIEW_JSON="${pass_old}" PATH="${tmp}:${PATH}" \
  "${verify_script}" 7 new-head >/dev/null 2>&1; then
  fail "expected stale-head CodeRabbit review marker to fail"
fi

needs_fix="${tmp}/needs-fix.json"
write_comments_json "${needs_fix}" $'CodeRabbit Subagent Review: NEEDS_FIX\nHead: new-head\n\n## 증거\n- 리뷰 결과: major 1건\n- 검증: coderabbit review --agent'
if GH_FAKE_PR_VIEW_JSON="${needs_fix}" PATH="${tmp}:${PATH}" \
  "${verify_script}" 7 new-head >/dev/null 2>&1; then
  fail "expected NEEDS_FIX marker not to satisfy ready gate"
fi

pass_new="${tmp}/pass-new.json"
write_comments_json "${pass_new}" $'CodeRabbit Subagent Review: PASS\nHead: new-head\n\n## 증거\n- 리뷰 결과: CodeRabbit raised 0 issues.\n- 검증: coderabbit review --agent'
GH_FAKE_PR_VIEW_JSON="${pass_new}" PATH="${tmp}:${PATH}" \
  "${verify_script}" 7 new-head >/dev/null

addressed_new="${tmp}/addressed-new.json"
write_comments_json "${addressed_new}" $'CodeRabbit Subagent Review: ADDRESSED\nHead: new-head\n\n## 증거\n- 리뷰 결과: major 1건\n- 수정 범위: scripts/task/example.sh\n- 검증: bash scripts/tests/test_coderabbit_review_gate.sh'
GH_FAKE_PR_VIEW_JSON="${addressed_new}" PATH="${tmp}:${PATH}" \
  "${verify_script}" 7 new-head >/dev/null

addressed_missing_evidence="${tmp}/addressed-missing-evidence.json"
write_comments_json "${addressed_missing_evidence}" $'CodeRabbit Subagent Review: ADDRESSED\nHead: new-head\n\n## 증거\n- 리뷰 결과: major 1건'
if GH_FAKE_PR_VIEW_JSON="${addressed_missing_evidence}" PATH="${tmp}:${PATH}" \
  "${verify_script}" 7 new-head >/dev/null 2>&1; then
  fail "expected ADDRESSED marker without required evidence labels to fail"
fi

STRICT_REQUIRE_CODERABBIT_REVIEW=0 \
GH_FAKE_PR_VIEW_JSON="${empty_comments}" PATH="${tmp}:${PATH}" \
  "${verify_script}" 7 new-head >/dev/null

good_evidence="${tmp}/good-evidence.md"
cat > "${good_evidence}" <<'EVIDENCE'
## 증거
- 리뷰 결과: major 1건
- 수정 범위: scripts/task/example.sh
- 검증: bash scripts/tests/test_coderabbit_review_gate.sh
EVIDENCE

comment_capture="${tmp}/addressed-comment.md"
PATH="${tmp}:${PATH}" FAKE_COMMENT_CAPTURE="${comment_capture}" \
  "${post_script}" 7 "${good_evidence}" >/dev/null
assert_contains "CodeRabbit Subagent Review: ADDRESSED" "${comment_capture}"
assert_contains "Head: new-head" "${comment_capture}"
assert_contains "수정 범위" "${comment_capture}"

bad_evidence="${tmp}/bad-evidence.md"
cat > "${bad_evidence}" <<'EVIDENCE'
## 증거
- 리뷰 결과: major 1건
EVIDENCE
if PATH="${tmp}:${PATH}" FAKE_COMMENT_CAPTURE="${comment_capture}" \
  "${post_script}" 7 "${bad_evidence}" >/dev/null 2>&1; then
  fail "expected post-coderabbit-addressed.sh to reject incomplete evidence"
fi

review_pass_comment="${tmp}/review-pass-comment.md"
review_args="${tmp}/coderabbit-args.log"
STRICT_CODERABBIT_OUTPUT="${tmp}/coderabbit-pass.ndjson" \
PATH="${tmp}:${PATH}" \
FAKE_COMMENT_CAPTURE="${review_pass_comment}" \
FAKE_CODERABBIT_ARGS_LOG="${review_args}" \
FAKE_CODERABBIT_MODE=pass \
  "${run_script}" 7 >/dev/null
assert_contains "CodeRabbit Subagent Review: PASS" "${review_pass_comment}"
assert_contains "Head: new-head" "${review_pass_comment}"
assert_contains "review --agent" "${review_args}"
assert_contains "-t all" "${review_args}"
assert_contains "--base develop" "${review_args}"
assert_contains "-c AGENTS.md" "${review_args}"

review_fix_comment="${tmp}/review-fix-comment.md"
if STRICT_CODERABBIT_OUTPUT="${tmp}/coderabbit-finding.ndjson" \
  PATH="${tmp}:${PATH}" \
  FAKE_COMMENT_CAPTURE="${review_fix_comment}" \
  FAKE_CODERABBIT_ARGS_LOG="${review_args}" \
  FAKE_CODERABBIT_MODE=finding \
    "${run_script}" 7 >/dev/null 2>&1; then
  fail "expected run-coderabbit-review.sh to fail when CodeRabbit raises issues"
fi
assert_contains "CodeRabbit Subagent Review: NEEDS_FIX" "${review_fix_comment}"
assert_contains "major: 1" "${review_fix_comment}"

if STRICT_CODERABBIT_OUTPUT="${tmp}/coderabbit-error.ndjson" \
  PATH="${tmp}:${PATH}" \
  FAKE_COMMENT_CAPTURE="${tmp}/review-error-comment.md" \
  FAKE_CODERABBIT_ARGS_LOG="${review_args}" \
  FAKE_CODERABBIT_MODE=error \
    "${run_script}" 7 >/dev/null 2>&1; then
  fail "expected run-coderabbit-review.sh to fail on CodeRabbit error events"
fi
