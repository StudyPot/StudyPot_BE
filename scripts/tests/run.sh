#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${SCRIPT_DIR}/test_task_scripts.sh"
"${SCRIPT_DIR}/test_jira_board_sync.sh"
"${SCRIPT_DIR}/test_jira_auto_done_on_merge.sh"
"${SCRIPT_DIR}/test_jira_next_recommendations.sh"
"${SCRIPT_DIR}/test_commit_convention.sh"
"${SCRIPT_DIR}/test_hook_enforcement.sh"
"${SCRIPT_DIR}/test_pr_scripts_static.sh"
"${SCRIPT_DIR}/test_auto_merge_notification.sh"
"${SCRIPT_DIR}/test_coderabbit_review_gate.sh"
"${SCRIPT_DIR}/test_copilot_review_gate.sh"
"${SCRIPT_DIR}/test_role_review_evidence.sh"
"${SCRIPT_DIR}/test_role_review_gate_validation.sh"
"${SCRIPT_DIR}/test_quality_gate_contracts.sh"
"${SCRIPT_DIR}/test_docs_source_of_truth.sh"
"${SCRIPT_DIR}/test_error_ledger_contracts.sh"
"${SCRIPT_DIR}/test_auth_api_contracts.sh"
"${SCRIPT_DIR}/test_local_dev_verification_contracts.sh"
"${SCRIPT_DIR}/test_ai_golden_path_verification_contracts.sh"
"${SCRIPT_DIR}/test_deployment_contracts.sh"
"${SCRIPT_DIR}/test_swagger_docs_contracts.sh"
