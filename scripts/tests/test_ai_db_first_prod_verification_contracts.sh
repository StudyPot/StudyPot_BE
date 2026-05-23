#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

target="${TEST_ROOT}/scripts/task/verify-ai-db-first-prod.sh"

assert_contains "test_ai_db_first_prod_verification_contracts.sh" "${TEST_ROOT}/scripts/tests/run.sh"
assert_file_exists "${target}"
[[ -x "${target}" ]] || fail "verify-ai-db-first-prod.sh must be executable"
bash -n "${target}"

assert_contains "STUDYPOT_PROD_BASE_URL" "${target}"
assert_contains "https://studypot.rumiclean.com" "${target}"
assert_contains "STUDYPOT_PROD_SSH_HOST" "${target}"
assert_contains "rumiclean" "${target}"
assert_contains "STUDYPOT_PROD_COMPOSE_DIR" "${target}"
assert_contains "/home/ec2-user/compose-studypot" "${target}"
assert_contains "STUDYPOT_DB_FIRST_AI_PROD_KEEP_DATA" "${target}"
assert_contains "load_env_file .runtime.env" "${target}"
assert_contains "docker exec -e MYSQL_PWD" "${target}"
assert_not_contains "docker exec -i" "${target}"

assert_contains "POST \${BASE_URL}/api/v1/groups" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/join" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/onboarding/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/start" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/groups/\${GROUP_ID}/weeks/current" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/weeks/\${WEEK_ID}/tasks" "${target}"
assert_contains "PUT \${BASE_URL}/api/v1/weeks/\${WEEK_ID}/progress/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/tasks/\${TASK_ID}/completion/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/weeks/\${WEEK_ID}/retrospectives/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/ai-conversations" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/ai-conversations/\${CONVERSATION_ID}/messages" "${target}"

assert_contains "RETROSPECTIVE_FEEDBACK" "${target}"
assert_contains "TEAM_LEAD_CHAT" "${target}"
assert_contains "input_summary" "${target}"
assert_contains "retrievalContextVersion" "${target}"
assert_contains "db-first-v1" "${target}"
assert_contains "llm_usage" "${target}"
assert_contains "request_payload" "${target}"
assert_contains "update weekly_task set due_at = timestampadd(second, -60, utc_timestamp(6))" "${target}"
assert_contains "input_tokens" "${target}"
assert_contains "output_tokens" "${target}"
assert_contains "latency_ms" "${target}"
assert_contains "outputBudget" "${target}"
assert_contains "apiMode" "${target}"
assert_contains "output_tokens exceeded outputBudget" "${target}"
assert_contains "cleanup.leftover_test_users" "${target}"
assert_contains "cleanup.leftover_test_groups" "${target}"
assert_contains "vector_rag: \`not verified / not implemented by this script\`" "${target}"
assert_contains "trap 'rm -rf \"\${TMP_DIR}\"; cleanup' EXIT" "${target}"

if grep -Eq 'echo .*(STUDYPOT_AI_OPENAI_API_KEY|OPENAI_API_KEY|AUTH_JWT_SECRET|HOST_TOKEN|MEMBER_TOKEN|Authorization: Bearer|MYSQL_PWD)' "${target}"; then
  fail "verify-ai-db-first-prod.sh must not echo secret-bearing variables"
fi

progress_fixture="$(awk '/progress.json/{capture=1} capture{print} /request_json progress/{exit}' "${target}")"
[[ "${progress_fixture}" == *'"status": "INCOMPLETE"'* ]] || fail "progress fixture must exercise INCOMPLETE"
[[ "${progress_fixture}" == *'"incompleteReason"'* ]] || fail "progress fixture must include incompleteReason"
[[ "${progress_fixture}" != *'"completionNote"'* ]] || fail "INCOMPLETE progress fixture must not include completionNote"

task_fixture="$(awk '/task-completion.json/{capture=1} capture{print} /request_json task_completion/{exit}' "${target}")"
[[ "${task_fixture}" == *'"status": "INCOMPLETE"'* ]] || fail "task completion fixture must exercise INCOMPLETE"
[[ "${task_fixture}" == *'"incompleteReason"'* ]] || fail "task completion fixture must include incompleteReason"
[[ "${task_fixture}" != *'"completionNote"'* ]] || fail "INCOMPLETE task completion fixture must not include completionNote"
