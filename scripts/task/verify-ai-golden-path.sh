#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

fail() {
  echo "Error: $*" >&2
  exit 1
}

note() {
  printf '%s\n' "$*"
}

# Endpoint coverage:
# POST ${BASE_URL}/api/v1/groups/detail-keyword-suggestions
# POST ${BASE_URL}/api/v1/groups
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/join
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/onboarding/me
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/start
# GET ${BASE_URL}/api/v1/groups/${GROUP_ID}/curriculum
# GET ${BASE_URL}/api/v1/groups/${GROUP_ID}/weeks/current
# GET ${BASE_URL}/api/v1/weeks/${WEEK_ID}/tasks
# PUT ${BASE_URL}/api/v1/weeks/${WEEK_ID}/progress/me
# POST ${BASE_URL}/api/v1/tasks/${TASK_ID}/completion/me
# POST ${BASE_URL}/api/v1/weeks/${WEEK_ID}/retrospectives/me
# GET ${BASE_URL}/api/v1/weeks/${WEEK_ID}/retrospectives/me
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/ai-conversations
# POST ${BASE_URL}/api/v1/ai-conversations/${CONVERSATION_ID}/messages
# GET ${BASE_URL}/api/v1/users/me/notifications
# GET ${BASE_URL}/api/v1/groups/${GROUP_ID}/llm-usage

LOCAL_CONFIG="${STUDYPOT_LOCAL_CONFIG:-${ROOT_DIR}/config/application-local.yml}"
[[ -f "${LOCAL_CONFIG}" ]] || fail "config/application-local.yml is required. Start from config/application-local.example.yml and keep real secrets outside src/main/resources."
[[ "${LOCAL_CONFIG}" != "${ROOT_DIR}/src/main/resources/"* ]] || fail "local secrets must not live under src/main/resources because build artifacts can package them."

PORT="${STUDYPOT_LOCAL_PORT:-18080}"
DB_HOST="${STUDYPOT_LOCAL_DB_HOST:-127.0.0.1}"
DB_PORT="${STUDYPOT_LOCAL_DB_PORT:-3306}"
DB_NAME="${STUDYPOT_LOCAL_DB_NAME:-studypot}"
DB_USER="${STUDYPOT_LOCAL_DB_USER:-${SPRING_DATASOURCE_USERNAME:-root}}"
DB_PASSWORD="${STUDYPOT_LOCAL_DB_PASSWORD:-${SPRING_DATASOURCE_PASSWORD:-}}"
RUN_ID="${STUDYPOT_AI_GOLDEN_PATH_RUN_ID:-$(date -u '+%Y%m%dT%H%M%SZ')-$$}"
LOG_DIR="${STUDYPOT_AI_GOLDEN_PATH_LOG_DIR:-${ROOT_DIR}/build/ai-golden-path/${RUN_ID}}"
APP_LOG="${LOG_DIR}/bootRun-${PORT}.log"
SUMMARY_FILE="${LOG_DIR}/ai-golden-path-summary.md"
BASE_URL="http://localhost:${PORT}"

mkdir -p "${LOG_DIR}/requests" "${LOG_DIR}/responses"

STUDYPOT_AI_OPENAI_API_KEY="${STUDYPOT_AI_OPENAI_API_KEY:-${OPENAI_API_KEY:-}}"
[[ -n "${STUDYPOT_AI_OPENAI_API_KEY}" ]] || fail "STUDYPOT_AI_OPENAI_API_KEY or OPENAI_API_KEY must be configured for real AI golden path verification."
STUDYPOT_AI_OPENAI_BASE_URL="${STUDYPOT_AI_OPENAI_BASE_URL:-https://api.openai.com/v1}"
STUDYPOT_AI_OPENAI_MODEL="${STUDYPOT_AI_OPENAI_MODEL:-gpt-4o-mini}"
STUDYPOT_AI_OPENAI_API_MODE="${STUDYPOT_AI_OPENAI_API_MODE:-responses}"
export STUDYPOT_AI_OPENAI_API_KEY
export STUDYPOT_AI_OPENAI_BASE_URL
export STUDYPOT_AI_OPENAI_MODEL
export STUDYPOT_AI_OPENAI_API_MODE

STUDYPOT_AUTH_JWT_SECRET="${STUDYPOT_AUTH_JWT_SECRET:-studypot-local-golden-path-jwt-secret-32-bytes-minimum}"
STUDYPOT_AUTH_JWT_ISSUER="${STUDYPOT_AUTH_JWT_ISSUER:-${BASE_URL}}"
STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI="${STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI:-http://localhost:3000/auth/success}"
STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI="${STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI:-http://localhost:3000/auth/failure}"
STUDYPOT_AUTH_COOKIE_SECURE="${STUDYPOT_AUTH_COOKIE_SECURE:-false}"
STUDYPOT_NOTIFICATION_RABBITMQ_ENABLED="${STUDYPOT_NOTIFICATION_RABBITMQ_ENABLED:-false}"
STUDYPOT_REDIS_HEALTH_ENABLED="${STUDYPOT_REDIS_HEALTH_ENABLED:-false}"
STUDYPOT_RABBITMQ_HEALTH_ENABLED="${STUDYPOT_RABBITMQ_HEALTH_ENABLED:-false}"
export STUDYPOT_AUTH_JWT_SECRET
export STUDYPOT_AUTH_JWT_ISSUER
export STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI
export STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI
export STUDYPOT_AUTH_COOKIE_SECURE
export STUDYPOT_NOTIFICATION_RABBITMQ_ENABLED
export STUDYPOT_REDIS_HEALTH_ENABLED
export STUDYPOT_RABBITMQ_HEALTH_ENABLED

APP_PID=""
GROUP_ID=""
WEEK_ID=""
TASK_ID=""
CONVERSATION_ID=""

mysql_exec() {
  local database="$1"
  local sql="$2"
  if [[ -n "${database}" ]]; then
    MYSQL_PWD="${DB_PASSWORD}" mysql --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" \
      --batch --skip-column-names "${database}" -e "${sql}"
  else
    MYSQL_PWD="${DB_PASSWORD}" mysql --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" \
      --batch --skip-column-names -e "${sql}"
  fi
}

uuid_bin_sql() {
  local value="$1"
  printf "unhex(replace('%s','-',''))" "${value}"
}

sql_string_literal() {
  SQL_VALUE="$1" python3 - <<'PY'
import os

value = os.environ["SQL_VALUE"].encode("utf-8")
if not value:
    print("''")
else:
    print(f"cast(0x{value.hex()} as char character set utf8mb4)")
PY
}

wait_for_http() {
  local url="$1"
  local attempts="${2:-90}"
  local delay="${3:-2}"
  for _ in $(seq 1 "${attempts}"); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      return 0
    fi
    if [[ -n "${APP_PID}" ]] && ! kill -0 "${APP_PID}" >/dev/null 2>&1; then
      tail -160 "${APP_LOG}" >&2 || true
      fail "application exited before ${url} became ready"
    fi
    sleep "${delay}"
  done
  tail -160 "${APP_LOG}" >&2 || true
  fail "timed out waiting for ${url}"
}

check_openai_api_key() {
  note "OpenAI API key preflight for configured model"
  python3 - <<'PY'
import json
import os
import re
import sys
import urllib.error
import urllib.request

base_url = os.environ["STUDYPOT_AI_OPENAI_BASE_URL"].rstrip("/")
model = os.environ["STUDYPOT_AI_OPENAI_MODEL"]
api_key = os.environ["STUDYPOT_AI_OPENAI_API_KEY"]
api_mode = os.environ.get("STUDYPOT_AI_OPENAI_API_MODE", "responses").strip().lower().replace("_", "-")


def sanitize(text: str) -> str:
    redacted = re.sub(r"sk(?:-proj)?-[^\s\"']+", "sk-***", text)
    return re.sub(
        r"S[0-9A-Za-z]+-[0-9a-fA-F-]{8}-[0-9a-fA-F-]{4}-[0-9a-fA-F-]{4}-[0-9a-fA-F-]{4}-[0-9a-fA-F-]{12}",
        "gms-***",
        redacted,
    )


def build_request() -> urllib.request.Request:
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Accept": "application/json",
    }
    if api_mode == "chat-completions":
        body = json.dumps({
            "model": model,
            "messages": [
                {"role": "developer", "content": "Answer in Korean"},
                {"role": "user", "content": "StudyPot AI preflight. Reply with one short Korean sentence."},
            ],
            "max_completion_tokens": 32,
        }).encode("utf-8")
        return urllib.request.Request(
            f"{base_url}/chat/completions",
            data=body,
            headers={**headers, "Content-Type": "application/json"},
        )
    return urllib.request.Request(
        f"{base_url}/models/{model}",
        headers=headers,
    )

try:
    with urllib.request.urlopen(build_request(), timeout=20) as response:
        if response.status < 200 or response.status >= 300:
            raise SystemExit(f"OpenAI API key preflight failed: status={response.status}")
except urllib.error.HTTPError as exc:
    body = exc.read().decode("utf-8", errors="replace")
    try:
        parsed = json.loads(body)
        body = json.dumps(parsed, ensure_ascii=False)
    except json.JSONDecodeError:
        pass
    print(
        f"OpenAI API key preflight failed: status={exc.code}; body={sanitize(body)}",
        file=sys.stderr,
    )
    raise SystemExit(1)
except urllib.error.URLError as exc:
    print(f"OpenAI API key preflight failed: {sanitize(str(exc.reason))}", file=sys.stderr)
    raise SystemExit(1)
PY
}

cleanup_data() {
  [[ "${STUDYPOT_AI_GOLDEN_PATH_KEEP_DATA:-false}" != "true" ]] || return 0

  local host_bin member_bin group_bin
  host_bin="$(uuid_bin_sql "${HOST_USER_ID}")"
  member_bin="$(uuid_bin_sql "${MEMBER_USER_ID}")"

  if [[ -n "${GROUP_ID:-}" ]]; then
    group_bin="$(uuid_bin_sql "${GROUP_ID}")"
    mysql_exec "${DB_NAME}" "delete from notification where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete from ai_conversation_message where conversation_id in (select id from ai_conversation where group_id = ${group_bin})" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete from ai_conversation where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete r from retrospective r join curriculum_week cw on cw.id = r.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete tc from task_completion tc join weekly_task wt on wt.id = tc.weekly_task_id join curriculum_week cw on cw.id = wt.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete mwp from member_week_progress mwp join curriculum_week cw on cw.id = mwp.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete wt from weekly_task wt join curriculum_week cw on cw.id = wt.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete cw from curriculum_week cw join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete from curriculum where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete mas from member_availability_slot mas join group_member gm on gm.id = mas.member_id where gm.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete from group_onboarding_response where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete from group_member where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete from llm_usage where group_id = ${group_bin} or user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
    mysql_exec "${DB_NAME}" "delete from study_group where id = ${group_bin}" >/dev/null 2>&1 || true
  else
    mysql_exec "${DB_NAME}" "delete from llm_usage where user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
  fi

  mysql_exec "${DB_NAME}" "delete from refresh_token where user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
  mysql_exec "${DB_NAME}" "delete from oauth_account where user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
  mysql_exec "${DB_NAME}" "delete from users where id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
}

cleanup() {
  cleanup_data || true
  if [[ -n "${APP_PID}" ]] && kill -0 "${APP_PID}" >/dev/null 2>&1; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
    wait "${APP_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

json_get() {
  local file="$1"
  local path="$2"
  python3 - "${file}" "${path}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    value = json.load(handle)

for part in sys.argv[2].split("."):
    if isinstance(value, list):
        value = value[int(part)]
    else:
        value = value[part]

if isinstance(value, (dict, list)):
    print(json.dumps(value, ensure_ascii=False))
elif value is None:
    print("")
else:
    print(value)
PY
}

json_assert_min_length() {
  local file="$1"
  local min_length="$2"
  python3 - "${file}" "${min_length}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    value = json.load(handle)
if not isinstance(value, list):
    raise SystemExit("expected JSON array")
if len(value) < int(sys.argv[2]):
    raise SystemExit(f"expected at least {sys.argv[2]} rows, got {len(value)}")
PY
}

json_assert_purposes() {
  local file="$1"
  shift
  python3 - "${file}" "$@" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    rows = json.load(handle)
purposes = {row.get("purpose") for row in rows}
missing = [purpose for purpose in sys.argv[2:] if purpose not in purposes]
if missing:
    raise SystemExit(f"missing purposes: {', '.join(missing)}; found={sorted(purposes)}")
PY
}

write_json() {
  local path="$1"
  local content="$2"
  printf '%s\n' "${content}" > "${path}"
}

request_json() {
  local method="$1"
  local url="$2"
  local token="$3"
  local body_file="$4"
  local output_file="$5"
  local expected_regex="$6"
  local status

  note "${method} ${url}"
  if [[ -n "${body_file}" ]]; then
    status="$(curl -sS -o "${output_file}" -w "%{http_code}" \
      -X "${method}" "${url}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      --data @"${body_file}")"
  else
    status="$(curl -sS -o "${output_file}" -w "%{http_code}" \
      -X "${method}" "${url}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json")"
  fi

  if [[ ! "${status}" =~ ${expected_regex} ]]; then
    printf 'Unexpected HTTP status %s for %s %s\n' "${status}" "${method}" "${url}" >&2
    cat "${output_file}" >&2 || true
    exit 1
  fi
}

new_uuid() {
  python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
}

generate_jwt() {
  local subject="$1"
  JWT_SUBJECT="${subject}" python3 - <<'PY'
import base64
import hashlib
import hmac
import json
import os
import time

def b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")

now = int(time.time())
header = {"alg": "HS256", "typ": "JWT"}
payload = {
    "iss": os.environ["STUDYPOT_AUTH_JWT_ISSUER"],
    "sub": os.environ["JWT_SUBJECT"],
    "iat": now,
    "exp": now + 3600,
}
signing_input = ".".join([
    b64url(json.dumps(header, separators=(",", ":")).encode()),
    b64url(json.dumps(payload, separators=(",", ":")).encode()),
])
signature = hmac.new(
    os.environ["STUDYPOT_AUTH_JWT_SECRET"].encode(),
    signing_input.encode(),
    hashlib.sha256,
).digest()
print(signing_input + "." + b64url(signature))
PY
}

seed_user() {
  local user_id="$1"
  local email="$2"
  local nickname="$3"
  local escaped_email_sql escaped_nickname_sql
  escaped_email_sql="$(sql_string_literal "${email}")"
  escaped_nickname_sql="$(sql_string_literal "${nickname}")"
  mysql_exec "${DB_NAME}" "
    insert into users (id, email, email_live_key, nickname, last_login_at, created_at, updated_at)
    values ($(uuid_bin_sql "${user_id}"), ${escaped_email_sql}, ${escaped_email_sql}, ${escaped_nickname_sql}, current_timestamp(6), current_timestamp(6), current_timestamp(6))
    on duplicate key update deleted_at = null, updated_at = current_timestamp(6)
  " >/dev/null
}

db_count() {
  local sql="$1"
  mysql_exec "${DB_NAME}" "${sql}" | tr -d '[:space:]'
}

write_summary() {
  local curriculum_count="$1"
  local week_count="$2"
  local task_count="$3"
  local retrospective_count="$4"
  local conversation_count="$5"
  local message_count="$6"
  local notification_count="$7"
  local usage_count="$8"

  cat > "${SUMMARY_FILE}" <<EOF
# AI Golden Path Verification

- run_id: \`${RUN_ID}\`
- base_url: \`${BASE_URL}\`
- group_id: \`${GROUP_ID}\`
- week_id: \`${WEEK_ID}\`
- task_id: \`${TASK_ID}\`
- conversation_id: \`${CONVERSATION_ID}\`
- evidence_dir: \`${LOG_DIR}\`
- secret_handling: redacted

## DB Counts
- curriculum: \`${curriculum_count}\`
- curriculum_week: \`${week_count}\`
- weekly_task: \`${task_count}\`
- retrospective: \`${retrospective_count}\`
- ai_conversation: \`${conversation_count}\`
- ai_conversation_message: \`${message_count}\`
- notification: \`${notification_count}\`
- llm_usage: \`${usage_count}\`

## Response Evidence
- detail keywords: \`responses/detail-keyword-suggestions.json\`
- group create: \`responses/create-group.json\`
- onboarding: \`responses/host-onboarding.json\`, \`responses/member-onboarding.json\`
- curriculum: \`responses/start-study.json\`, \`responses/curriculum.json\`
- weekly todo: \`responses/current-week.json\`, \`responses/tasks.json\`, \`responses/progress.json\`, \`responses/task-completion.json\`
- retrospective: \`responses/retrospective-request.json\`, \`responses/retrospective-read.json\`
- AI conversation: \`responses/open-conversation.json\`, \`responses/send-message.json\`
- notification and usage: \`responses/my-notifications.json\`, \`responses/group-llm-usage.json\`
EOF
}

HOST_USER_ID="$(new_uuid)"
MEMBER_USER_ID="$(new_uuid)"
HOST_EMAIL="golden-host-${RUN_ID}@studypot.local"
MEMBER_EMAIL="golden-member-${RUN_ID}@studypot.local"
HOST_TOKEN=""
MEMBER_TOKEN=""

note "Checking MySQL connectivity on ${DB_HOST}:${DB_PORT}/${DB_NAME}"
mysql_exec "" "select 1" >/dev/null
mysql_exec "" "create database if not exists \`${DB_NAME}\` character set utf8mb4 collate utf8mb4_0900_ai_ci" >/dev/null

check_openai_api_key

note "Starting Spring Boot local profile on ${BASE_URL}"
(
  cd "${ROOT_DIR}"
  SPRING_PROFILES_ACTIVE=local \
  SPRING_CONFIG_ADDITIONAL_LOCATION="file:${LOCAL_CONFIG}" \
  SERVER_PORT="${PORT}" \
  STUDYPOT_OPENAPI_ENABLED=true \
  STUDYPOT_SWAGGER_UI_ENABLED=true \
  STUDYPOT_OPENAPI_PUBLIC_DOCS_ENABLED=true \
  SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&connectionTimeZone=UTC" \
  SPRING_DATASOURCE_USERNAME="${DB_USER}" \
  SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}" \
  ./gradlew bootRun --no-daemon
) >"${APP_LOG}" 2>&1 &
APP_PID="$!"

wait_for_http "${BASE_URL}/actuator/health"
curl -fsS "${BASE_URL}/actuator/health" | grep -F '"status":"UP"' >/dev/null
curl -fsS "${BASE_URL}/v3/api-docs" >/dev/null

note "Seeding local verification users"
seed_user "${HOST_USER_ID}" "${HOST_EMAIL}" "Golden Host"
seed_user "${MEMBER_USER_ID}" "${MEMBER_EMAIL}" "Golden Member"

note "Generating local bearer tokens"
HOST_TOKEN="$(generate_jwt "${HOST_USER_ID}")"
MEMBER_TOKEN="$(generate_jwt "${MEMBER_USER_ID}")"

request_json GET "${BASE_URL}/api/v1/users/me" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/current-user.json" '^2'

write_json "${LOG_DIR}/requests/detail-keyword-suggestions.json" '{
  "topic": "Spring Boot",
  "hintKeywords": ["JPA", "Security"],
  "maxCandidates": 3
}'
request_json POST "${BASE_URL}/api/v1/groups/detail-keyword-suggestions" "${HOST_TOKEN}" "${LOG_DIR}/requests/detail-keyword-suggestions.json" "${LOG_DIR}/responses/detail-keyword-suggestions.json" '^2'
json_assert_min_length "${LOG_DIR}/responses/detail-keyword-suggestions.json" 1

read -r STARTS_AT ENDS_AT < <(python3 - <<'PY'
from datetime import date, timedelta
today = date.today()
print(today.isoformat(), (today + timedelta(days=42)).isoformat())
PY
)
write_json "${LOG_DIR}/requests/create-group.json" "{
  \"name\": \"AI Golden Path ${RUN_ID}\",
  \"topic\": \"Spring Boot\",
  \"detailKeywords\": [\"JPA\", \"Spring Security\", \"Testing\"],
  \"maxMembers\": 6,
  \"startsAt\": \"${STARTS_AT}\",
  \"endsAt\": \"${ENDS_AT}\",
  \"description\": \"Backend-only golden path verification run.\"
}"
request_json POST "${BASE_URL}/api/v1/groups" "${HOST_TOKEN}" "${LOG_DIR}/requests/create-group.json" "${LOG_DIR}/responses/create-group.json" '^201$'
GROUP_ID="$(json_get "${LOG_DIR}/responses/create-group.json" "id")"
INVITE_CODE="$(json_get "${LOG_DIR}/responses/create-group.json" "inviteCode")"

write_json "${LOG_DIR}/requests/join-group.json" "{
  \"inviteCode\": \"${INVITE_CODE}\"
}"
request_json POST "${BASE_URL}/api/v1/groups/${GROUP_ID}/join" "${MEMBER_TOKEN}" "${LOG_DIR}/requests/join-group.json" "${LOG_DIR}/responses/join-group.json" '^2'

write_json "${LOG_DIR}/requests/host-onboarding.json" '{
  "skillLevel": 4,
  "additionalNote": "JPA and Spring Security practice first. No secrets: apiKey=redacted-token should stay redacted.",
  "availabilitySlots": [
    {"dayOfWeek": 2, "startTime": "20:00", "endTime": "22:00", "timezone": "Asia/Seoul"}
  ]
}'
request_json POST "${BASE_URL}/api/v1/groups/${GROUP_ID}/onboarding/me" "${HOST_TOKEN}" "${LOG_DIR}/requests/host-onboarding.json" "${LOG_DIR}/responses/host-onboarding.json" '^2'

write_json "${LOG_DIR}/requests/member-onboarding.json" '{
  "skillLevel": 2,
  "additionalNote": "I need smaller assignments and examples before implementation.",
  "availabilitySlots": [
    {"dayOfWeek": 4, "startTime": "19:30", "endTime": "21:30", "timezone": "Asia/Seoul"}
  ]
}'
request_json POST "${BASE_URL}/api/v1/groups/${GROUP_ID}/onboarding/me" "${MEMBER_TOKEN}" "${LOG_DIR}/requests/member-onboarding.json" "${LOG_DIR}/responses/member-onboarding.json" '^2'

request_json POST "${BASE_URL}/api/v1/groups/${GROUP_ID}/start" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/start-study.json" '^201$'
CURRICULUM_ID="$(json_get "${LOG_DIR}/responses/start-study.json" "id")"
[[ -n "${CURRICULUM_ID}" ]] || fail "curriculum id is required"

request_json GET "${BASE_URL}/api/v1/groups/${GROUP_ID}/curriculum" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/curriculum.json" '^2'
request_json GET "${BASE_URL}/api/v1/groups/${GROUP_ID}/weeks/current" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/current-week.json" '^2'
WEEK_ID="$(json_get "${LOG_DIR}/responses/current-week.json" "id")"
request_json GET "${BASE_URL}/api/v1/weeks/${WEEK_ID}/tasks" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/tasks.json" '^2'
json_assert_min_length "${LOG_DIR}/responses/tasks.json" 1
TASK_ID="$(json_get "${LOG_DIR}/responses/tasks.json" "0.id")"

write_json "${LOG_DIR}/requests/progress.json" '{
  "status": "IN_PROGRESS",
  "completionNote": "Started the first week from API verification.",
  "incompleteReason": null
}'
request_json PUT "${BASE_URL}/api/v1/weeks/${WEEK_ID}/progress/me" "${HOST_TOKEN}" "${LOG_DIR}/requests/progress.json" "${LOG_DIR}/responses/progress.json" '^2'

write_json "${LOG_DIR}/requests/task-completion.json" '{
  "status": "DONE",
  "completionNote": "Completed through backend golden path verification.",
  "incompleteReason": null,
  "evidenceUrl": "https://example.com/studypot/golden-path"
}'
request_json POST "${BASE_URL}/api/v1/tasks/${TASK_ID}/completion/me" "${HOST_TOKEN}" "${LOG_DIR}/requests/task-completion.json" "${LOG_DIR}/responses/task-completion.json" '^2'

request_json POST "${BASE_URL}/api/v1/weeks/${WEEK_ID}/retrospectives/me" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/retrospective-request.json" '^202$'
RETROSPECTIVE_STATUS="$(json_get "${LOG_DIR}/responses/retrospective-request.json" "status")"
[[ "${RETROSPECTIVE_STATUS}" == "COMPLETED" ]] || fail "expected retrospective status COMPLETED, got ${RETROSPECTIVE_STATUS}"
RETROSPECTIVE_ID="$(json_get "${LOG_DIR}/responses/retrospective-request.json" "id")"
request_json GET "${BASE_URL}/api/v1/weeks/${WEEK_ID}/retrospectives/me" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/retrospective-read.json" '^2'

write_json "${LOG_DIR}/requests/open-conversation.json" "{
  \"conversationType\": \"RETROSPECTIVE\",
  \"weekId\": \"${WEEK_ID}\",
  \"retrospectiveId\": \"${RETROSPECTIVE_ID}\"
}"
request_json POST "${BASE_URL}/api/v1/groups/${GROUP_ID}/ai-conversations" "${HOST_TOKEN}" "${LOG_DIR}/requests/open-conversation.json" "${LOG_DIR}/responses/open-conversation.json" '^201$'
CONVERSATION_ID="$(json_get "${LOG_DIR}/responses/open-conversation.json" "id")"

write_json "${LOG_DIR}/requests/send-message.json" '{
  "content": "이번 주 학습량과 다음 주 난이도 조정을 짧게 제안해줘."
}'
request_json POST "${BASE_URL}/api/v1/ai-conversations/${CONVERSATION_ID}/messages" "${HOST_TOKEN}" "${LOG_DIR}/requests/send-message.json" "${LOG_DIR}/responses/send-message.json" '^201$'

request_json GET "${BASE_URL}/api/v1/users/me/notifications" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/my-notifications.json" '^2'
request_json POST "${BASE_URL}/api/v1/users/me/notifications/read-all" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/read-all-notifications.txt" '^204$'
request_json GET "${BASE_URL}/api/v1/groups/${GROUP_ID}/notifications" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/group-notifications.json" '^2'
request_json GET "${BASE_URL}/api/v1/groups/${GROUP_ID}/llm-usage" "${HOST_TOKEN}" "" "${LOG_DIR}/responses/group-llm-usage.json" '^2'
json_assert_min_length "${LOG_DIR}/responses/group-llm-usage.json" 3
json_assert_purposes "${LOG_DIR}/responses/group-llm-usage.json" CURRICULUM_GENERATE RETROSPECTIVE_FEEDBACK TEAM_LEAD_CHAT

group_bin="$(uuid_bin_sql "${GROUP_ID}")"
curriculum_count="$(db_count "select count(*) from curriculum where group_id = ${group_bin}")"
week_count="$(db_count "select count(*) from curriculum_week cw join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}")"
task_count="$(db_count "select count(*) from weekly_task wt join curriculum_week cw on cw.id = wt.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}")"
retrospective_count="$(db_count "select count(*) from retrospective r join curriculum_week cw on cw.id = r.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}")"
conversation_count="$(db_count "select count(*) from ai_conversation where group_id = ${group_bin}")"
message_count="$(db_count "select count(*) from ai_conversation_message m join ai_conversation c on c.id = m.conversation_id where c.group_id = ${group_bin}")"
notification_count="$(db_count "select count(*) from notification where group_id = ${group_bin}")"
usage_count="$(db_count "select count(*) from llm_usage where group_id = ${group_bin}")"

[[ "${curriculum_count}" -ge 1 ]] || fail "expected curriculum rows"
[[ "${week_count}" -ge 1 ]] || fail "expected curriculum_week rows"
[[ "${task_count}" -ge 1 ]] || fail "expected weekly_task rows"
[[ "${retrospective_count}" -ge 1 ]] || fail "expected retrospective rows"
[[ "${conversation_count}" -ge 1 ]] || fail "expected ai_conversation rows"
[[ "${message_count}" -ge 2 ]] || fail "expected user and assistant ai_conversation_message rows"
[[ "${notification_count}" -ge 1 ]] || fail "expected notification rows"
[[ "${usage_count}" -ge 3 ]] || fail "expected group llm_usage rows"

write_summary \
  "${curriculum_count}" \
  "${week_count}" \
  "${task_count}" \
  "${retrospective_count}" \
  "${conversation_count}" \
  "${message_count}" \
  "${notification_count}" \
  "${usage_count}"

note "AI golden path verification passed."
note "Redacted evidence summary: ${SUMMARY_FILE}"
