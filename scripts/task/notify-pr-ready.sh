#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/common.sh"

pr="${1:-}"
pr_url="${2:-}"
head_sha="${3:-}"
status="${4:-merged}"

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
if status == "merged":
    lines.extend(
        [
            f"StudyPot PR #{pr} 자동 merge가 완료됐습니다.",
            f"- PR 링크: {pr_url}",
            f"- Head: `{head_sha}`",
            "- 처리: review gate 통과 후 `scripts/task/finish-pr.sh`가 GitHub merge와 로컬 정리를 진행했습니다.",
            "- Jira 처리: GitHub merge 후 연결된 Jira Task는 자동 또는 하네스 정리 단계에서 완료 처리됩니다.",
        ]
    )
else:
    lines.extend(
        [
            f"StudyPot PR #{pr} 자동 merge 상태 알림입니다.",
            f"- PR 링크: {pr_url}",
            f"- Head: `{head_sha}`",
            f"- 상태: `{status}`",
        ]
    )

print(json.dumps({"text": "\n".join(lines)}, ensure_ascii=False))
PY

curl -fsS \
  -X POST \
  -H 'Content-Type: application/json' \
  --data-binary @"${payload_file}" \
  "${STUDYPOT_MM_WEBHOOK_URL}" >/dev/null

printf 'Mattermost PR 자동 merge 알림을 보냈습니다: PR %s.\n' "${pr}"
