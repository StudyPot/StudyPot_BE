#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

gate_script="${TEST_ROOT}/scripts/task/verify-copilot-review.sh"
assert_file_exists "${gate_script}"
bash -n "${gate_script}"

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
args=("$@")
for ((i = 0; i < ${#args[@]}; i++)); do
  if [[ "${args[$i]}" == "--jq" ]]; then
    jq_filter="${args[$((i + 1))]}"
    break
  fi
done

if [[ "$1" == "pr" && "$2" == "view" ]]; then
  if [[ "$*" == *"headRefOid"* ]]; then
    printf '{"headRefOid":"%s"}\n' "${FAKE_HEAD_SHA:-new-head}" | apply_jq "${jq_filter}"
    exit 0
  fi
  if [[ "$*" == *"reviews"* ]]; then
    if [[ "${FAKE_COPILOT_REVIEW_COUNT:-0}" == "0" ]]; then
      printf '{"reviews":[]}\n'
    else
      cat <<JSON
{
  "reviews": [
    {
      "author": { "login": "copilot-pull-request-reviewer" },
      "commit": { "oid": "${FAKE_COPILOT_REVIEW_COMMIT:-new-head}" },
      "state": "COMMENTED"
    }
  ]
}
JSON
    fi | apply_jq "${jq_filter}"
    exit 0
  fi
fi

if [[ "$1" == "repo" && "$2" == "view" ]]; then
  printf '{"nameWithOwner":"StudyPot/StudyPot_BE"}\n' | apply_jq "${jq_filter}"
  exit 0
fi

if [[ "$1" == "api" && "$2" == "graphql" ]]; then
  if [[ "${FAKE_COPILOT_UNRESOLVED_THREADS:-0}" == "0" ]]; then
    nodes='[]'
  else
    nodes='[
      {
        "isResolved": false,
        "comments": {
          "pageInfo": { "hasNextPage": false },
          "nodes": [
            { "author": { "login": "copilot-pull-request-reviewer" } }
          ]
        }
      }
    ]'
  fi

  cat <<JSON | jq --argjson nodes "${nodes}" \
    --argjson threadNext "${FAKE_COPILOT_THREAD_HAS_NEXT:-false}" \
    --argjson commentNext "${FAKE_COPILOT_COMMENT_HAS_NEXT:-false}" \
    '.data.repository.pullRequest.reviewThreads.pageInfo.hasNextPage = $threadNext
      | .data.repository.pullRequest.reviewThreads.nodes = $nodes
      | if $commentNext and (($nodes | length) > 0) then
          .data.repository.pullRequest.reviewThreads.nodes[0].comments.pageInfo.hasNextPage = true
        else . end' | apply_jq "${jq_filter}"
{
  "data": {
    "repository": {
      "pullRequest": {
        "reviewThreads": {
          "pageInfo": { "hasNextPage": false },
          "nodes": []
        }
      }
    }
  }
}
JSON
  exit 0
fi

echo "unexpected gh call: $*" >&2
exit 1
STUB
chmod +x "${fake_gh}"

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=0 \
  FAKE_COPILOT_UNRESOLVED_THREADS=0 \
  STRICT_COPILOT_REVIEW_WAIT_SECONDS=0 \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected missing Copilot review activity to fail"
fi

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=1 \
  FAKE_COPILOT_REVIEW_COMMIT=old-head \
  FAKE_COPILOT_UNRESOLVED_THREADS=0 \
  STRICT_COPILOT_REVIEW_WAIT_SECONDS=0 \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected stale-head Copilot review activity to fail by default"
fi

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=0 \
  FAKE_COPILOT_UNRESOLVED_THREADS=0 \
  STRICT_COPILOT_REVIEW_WAIT_SECONDS=invalid \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected invalid Copilot review wait seconds to fail"
fi

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=0 \
  FAKE_COPILOT_UNRESOLVED_THREADS=0 \
  STRICT_COPILOT_REVIEW_POLL_INTERVAL_SECONDS=0 \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected invalid Copilot review poll interval to fail"
fi

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=1 \
  FAKE_COPILOT_UNRESOLVED_THREADS=1 \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected unresolved Copilot review threads to fail"
fi

PATH="${tmp}:${PATH}" \
FAKE_COPILOT_REVIEW_COUNT=1 \
FAKE_COPILOT_UNRESOLVED_THREADS=0 \
  "${gate_script}" 32 >/dev/null

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=1 \
  FAKE_COPILOT_UNRESOLVED_THREADS=0 \
  FAKE_COPILOT_THREAD_HAS_NEXT=true \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected paginated review threads to fail closed"
fi

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=1 \
  FAKE_COPILOT_UNRESOLVED_THREADS=1 \
  FAKE_COPILOT_COMMENT_HAS_NEXT=true \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected paginated review thread comments to fail closed"
fi

STRICT_REQUIRE_COPILOT_REVIEW=0 \
PATH="${tmp}:${PATH}" \
FAKE_COPILOT_REVIEW_COUNT=0 \
FAKE_COPILOT_UNRESOLVED_THREADS=9 \
  "${gate_script}" 32 >/dev/null
