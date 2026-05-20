#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

notify_script="${TEST_ROOT}/scripts/task/notify-pr-ready.sh"
assert_file_exists "${notify_script}"
bash -n "${notify_script}"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

mkdir -p "${tmp}/bin"
cat > "${tmp}/bin/curl" <<'STUB'
#!/usr/bin/env bash

set -euo pipefail

payload_file=""
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --data|--data-binary)
      payload_file="${2#@}"
      shift 2
      ;;
    -X|-H|-o|-w)
      shift 2
      ;;
    -fsS|-sS)
      shift
      ;;
    *)
      shift
      ;;
  esac
done

[[ -n "${payload_file}" && -f "${payload_file}" ]] || {
  echo "missing payload file" >&2
  exit 1
}

cp "${payload_file}" "${FAKE_CURL_PAYLOAD:?FAKE_CURL_PAYLOAD is required}"
printf 'ok\n'
STUB
chmod +x "${tmp}/bin/curl"

payload="${tmp}/payload.json"
PATH="${tmp}/bin:${PATH}" \
FAKE_CURL_PAYLOAD="${payload}" \
STUDYPOT_MM_WEBHOOK_URL="https://mattermost.example/hooks/test" \
STUDYPOT_MM_MENTIONS="@hw62459930 @yjhn0410" \
  "${notify_script}" "17" "https://github.com/StudyPot/StudyPot_BE/pull/17" "abc123" "merged"

python3 - "${payload}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    doc = json.load(handle)

text = doc.get("text", "")
required = [
    "@hw62459930 @yjhn0410",
    "PR #17",
    "https://github.com/StudyPot/StudyPot_BE/pull/17",
    "abc123",
    "자동 merge가 완료",
    "review gate",
    "scripts/task/finish-pr.sh",
    "Jira Task",
]
missing = [value for value in required if value not in text]
if missing:
    raise SystemExit(f"missing notification text: {missing}")
PY

missing_env_stderr="${tmp}/missing-env.stderr"
if PATH="${tmp}/bin:${PATH}" \
  FAKE_CURL_PAYLOAD="${tmp}/unused.json" \
  STUDYPOT_MM_MENTIONS="@hw62459930 @yjhn0410" \
  env -u STUDYPOT_MM_WEBHOOK_URL "${notify_script}" "17" "https://github.com/StudyPot/StudyPot_BE/pull/17" "abc123" "ready" 2>"${missing_env_stderr}"; then
  fail "notify-pr-ready.sh should fail when STUDYPOT_MM_WEBHOOK_URL is missing"
fi
assert_contains "STUDYPOT_MM_WEBHOOK_URL is required" "${missing_env_stderr}"
assert_not_contains "https://mattermost.example/hooks/test" "${missing_env_stderr}"
