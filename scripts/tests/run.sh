#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${SCRIPT_DIR}/test_task_scripts.sh"
"${SCRIPT_DIR}/test_jira_board_sync.sh"
"${SCRIPT_DIR}/test_jira_next_recommendations.sh"
"${SCRIPT_DIR}/test_commit_convention.sh"
"${SCRIPT_DIR}/test_hook_enforcement.sh"
"${SCRIPT_DIR}/test_pr_scripts_static.sh"
"${SCRIPT_DIR}/test_role_review_evidence.sh"
"${SCRIPT_DIR}/test_quality_gate_contracts.sh"
"${SCRIPT_DIR}/test_docs_source_of_truth.sh"
"${SCRIPT_DIR}/test_error_ledger_contracts.sh"
"${SCRIPT_DIR}/test_auth_api_contracts.sh"
