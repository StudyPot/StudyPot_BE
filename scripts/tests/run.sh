#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${SCRIPT_DIR}/test_task_scripts.sh"
"${SCRIPT_DIR}/test_jira_board_sync.sh"
"${SCRIPT_DIR}/test_hook_enforcement.sh"
"${SCRIPT_DIR}/test_pr_scripts_static.sh"
