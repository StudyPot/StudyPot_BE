#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

target="${TEST_ROOT}/scripts/task/verify-ai-golden-path.sh"

assert_contains "test_ai_golden_path_verification_contracts.sh" "${TEST_ROOT}/scripts/tests/run.sh"
assert_file_exists "${target}"
[[ -x "${target}" ]] || fail "verify-ai-golden-path.sh must be executable"
bash -n "${target}"

assert_contains "config/application-local.yml" "${target}"
assert_contains "SPRING_CONFIG_ADDITIONAL_LOCATION" "${target}"
assert_contains "SPRING_PROFILES_ACTIVE=local" "${target}"
assert_contains "STUDYPOT_AI_OPENAI_API_KEY" "${target}"
assert_contains "OPENAI_API_KEY" "${target}"
assert_contains "must be configured" "${target}"
assert_contains "OpenAI API key preflight" "${target}"
assert_contains "STUDYPOT_AI_OPENAI_BASE_URL" "${target}"
assert_contains "STUDYPOT_AI_OPENAI_API_MODE" "${target}"
assert_contains "chat/completions" "${target}"
assert_contains "gms-***" "${target}"
assert_contains "sk-***" "${target}"
assert_contains "sql_string_literal" "${target}"
assert_contains "cast(0x" "${target}"
assert_contains "escaped_email_sql" "${target}"
assert_contains "escaped_nickname_sql" "${target}"
assert_contains "Generating local bearer tokens" "${target}"
assert_contains "Authorization: Bearer" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/detail-keyword-suggestions" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/join" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/onboarding/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/start" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/groups/\${GROUP_ID}/curriculum" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/groups/\${GROUP_ID}/weeks/current" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/weeks/\${WEEK_ID}/tasks" "${target}"
assert_contains "PUT \${BASE_URL}/api/v1/weeks/\${WEEK_ID}/progress/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/tasks/\${TASK_ID}/completion/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/weeks/\${WEEK_ID}/retrospectives/me" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/weeks/\${WEEK_ID}/retrospectives/me" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/groups/\${GROUP_ID}/ai-conversations" "${target}"
assert_contains "POST \${BASE_URL}/api/v1/ai-conversations/\${CONVERSATION_ID}/messages" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/users/me/notifications" "${target}"
assert_contains "GET \${BASE_URL}/api/v1/groups/\${GROUP_ID}/llm-usage" "${target}"
assert_contains "llm_usage" "${target}"
assert_contains "notification" "${target}"
assert_contains "ai-golden-path-summary.md" "${target}"
assert_contains "redacted" "${target}"
assert_contains "trap cleanup EXIT" "${target}"

if grep -Eq 'echo .*(STUDYPOT_AI_OPENAI_API_KEY|OPENAI_API_KEY|AUTH_JWT_SECRET|ACCESS_TOKEN|REFRESH_TOKEN)' "${target}"; then
  fail "verify-ai-golden-path.sh must not echo secret-bearing variables"
fi
