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

if [[ "$1" == "pr" && "$2" == "view" ]]; then
  if [[ "$*" == *"headRefOid"* ]]; then
    printf '%s\n' "${FAKE_HEAD_SHA:-new-head}"
    exit 0
  fi
  if [[ "$*" == *"reviews"* ]]; then
    printf '%s\n' "${FAKE_COPILOT_REVIEW_COUNT:-0}"
    exit 0
  fi
fi

if [[ "$1" == "repo" && "$2" == "view" ]]; then
  printf 'StudyPot/StudyPot_BE\n'
  exit 0
fi

if [[ "$1" == "api" && "$2" == "graphql" ]]; then
  printf '%s\n' "${FAKE_COPILOT_UNRESOLVED_THREADS:-0}"
  exit 0
fi

echo "unexpected gh call: $*" >&2
exit 1
STUB
chmod +x "${fake_gh}"

if PATH="${tmp}:${PATH}" \
  FAKE_COPILOT_REVIEW_COUNT=0 \
  FAKE_COPILOT_UNRESOLVED_THREADS=0 \
  "${gate_script}" 32 >/dev/null 2>&1; then
  fail "expected missing latest-head Copilot review activity to fail"
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

STRICT_REQUIRE_COPILOT_REVIEW=0 \
PATH="${tmp}:${PATH}" \
FAKE_COPILOT_REVIEW_COUNT=0 \
FAKE_COPILOT_UNRESOLVED_THREADS=9 \
  "${gate_script}" 32 >/dev/null
