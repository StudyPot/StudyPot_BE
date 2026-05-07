#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
pr_url="${2:-}"
head_sha="${3:-}"
status="${4:-ready}"

[[ -n "${pr}" && -n "${pr_url}" && -n "${head_sha}" ]] || fail "usage: notify-pr-ready.sh <PR_NUMBER> <PR_URL> <HEAD_SHA> [STATUS]"
[[ -n "${STUDYPOT_MM_WEBHOOK_URL:-}" ]] || fail "STUDYPOT_MM_WEBHOOK_URL is required for PR-ready Mattermost notification."
command -v curl >/dev/null 2>&1 || fail "curl is required for Mattermost notification."
command -v python3 >/dev/null 2>&1 || fail "python3 is required for Mattermost payload generation."

payload_file="$(mktemp)"
trap 'rm -f "${payload_file:-}"' EXIT

PR_NUMBER="${pr}" \
PR_URL="${pr_url}" \
PR_HEAD_SHA="${head_sha}" \
PR_READY_STATUS="${status}" \
MM_MENTIONS="${STUDYPOT_MM_MENTIONS:-}" \
python3 - <<'PY' > "${payload_file}"
import json
import os

mentions = os.environ.get("MM_MENTIONS", "").strip()
pr = os.environ["PR_NUMBER"]
pr_url = os.environ["PR_URL"]
head_sha = os.environ["PR_HEAD_SHA"]
status = os.environ["PR_READY_STATUS"]

lines = []
if mentions:
    lines.append(mentions)
lines.extend(
    [
        f"StudyPot PR #{pr} 수동 merge 준비가 끝났습니다.",
        f"- PR 링크: {pr_url}",
        f"- Head: `{head_sha}`",
        f"- 상태: `{status}`",
        "- 필요한 작업: GitHub에서 변경 내용을 확인한 뒤 사람이 직접 merge 버튼을 눌러주세요.",
        "- Jira 처리: GitHub merge 후 연결된 Jira Task는 자동으로 완료 처리됩니다.",
        f"- merge 후 정리: `scripts/task/finish-pr.sh cleanup-merged {pr}`",
    ]
)

print(json.dumps({"text": "\n".join(lines)}, ensure_ascii=False))
PY

curl -fsS \
  -X POST \
  -H 'Content-Type: application/json' \
  --data-binary @"${payload_file}" \
  "${STUDYPOT_MM_WEBHOOK_URL}" >/dev/null

printf 'Mattermost PR 준비 알림을 보냈습니다: PR %s.\n' "${pr}"
