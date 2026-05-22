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

shell_quote() {
  printf '%q' "$1"
}

# Production DB-first AI endpoint coverage:
# POST ${BASE_URL}/api/v1/groups
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/join
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/onboarding/me
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/start
# GET ${BASE_URL}/api/v1/groups/${GROUP_ID}/weeks/current
# GET ${BASE_URL}/api/v1/weeks/${WEEK_ID}/tasks
# PUT ${BASE_URL}/api/v1/weeks/${WEEK_ID}/progress/me
# POST ${BASE_URL}/api/v1/tasks/${TASK_ID}/completion/me
# POST ${BASE_URL}/api/v1/weeks/${WEEK_ID}/retrospectives/me
# POST ${BASE_URL}/api/v1/groups/${GROUP_ID}/ai-conversations
# POST ${BASE_URL}/api/v1/ai-conversations/${CONVERSATION_ID}/messages

BASE_URL="${STUDYPOT_PROD_BASE_URL:-https://studypot.rumiclean.com}"
SSH_HOST="${STUDYPOT_PROD_SSH_HOST:-rumiclean}"
COMPOSE_DIR="${STUDYPOT_PROD_COMPOSE_DIR:-/home/ec2-user/compose-studypot}"
RUN_ID="${STUDYPOT_DB_FIRST_AI_PROD_RUN_ID:-$(date -u '+%Y%m%dT%H%M%SZ')-$$}"
LOG_DIR="${STUDYPOT_DB_FIRST_AI_PROD_LOG_DIR:-${ROOT_DIR}/build/ai-db-first-prod/${RUN_ID}}"
RAW_LOG="${LOG_DIR}/remote-output.log"
SUMMARY_FILE="${LOG_DIR}/ai-db-first-prod-summary.md"

mkdir -p "${LOG_DIR}"

note "DB-first AI production verification"
note "base_url=${BASE_URL}"
note "ssh_host=${SSH_HOST}"
note "compose_dir=${COMPOSE_DIR}"
note "run_id=${RUN_ID}"

KEEP_DATA="${STUDYPOT_DB_FIRST_AI_PROD_KEEP_DATA:-false}"
remote_command="cd $(shell_quote "${COMPOSE_DIR}") && STUDYPOT_PROD_BASE_URL=$(shell_quote "${BASE_URL}") STUDYPOT_DB_FIRST_AI_PROD_RUN_ID=$(shell_quote "${RUN_ID}") STUDYPOT_DB_FIRST_AI_PROD_KEEP_DATA=$(shell_quote "${KEEP_DATA}") bash -s"

# Intentionally expand the shell-quoted local run settings before SSH so the
# remote harness receives this run's target URL, run id, and cleanup mode.
# shellcheck disable=SC2029
ssh "${SSH_HOST}" "${remote_command}" <<'REMOTE' | tee "${RAW_LOG}"
set -euo pipefail

fail() {
  echo "remote_error=$*" >&2
  exit 1
}

note() {
  printf '%s\n' "$*"
}

load_env_file() {
  local file="$1"
  [[ -f "${file}" ]] || return 0
  local line
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line%$'\r'}"
    [[ -z "${line}" ]] && continue
    case "${line}" in
      \#*) continue ;;
      *=*) export "${line%%=*}=${line#*=}" ;;
    esac
  done <"${file}"
}

uuid_bin_sql() {
  local value="$1"
  printf "unhex(replace('%s','-',''))" "${value}"
}

sql_string_literal() {
  SQL_VALUE="$1" python3 - <<'PY'
import os

value = os.environ["SQL_VALUE"].encode("utf-8")
print(f"cast(0x{value.hex()} as char character set utf8mb4)")
PY
}

new_uuid() {
  python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
}

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
  local path="$2"
  local min_length="$3"
  python3 - "${file}" "${path}" "${min_length}" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    value = json.load(handle)
for part in sys.argv[2].split("."):
    if not part:
        continue
    value = value[int(part)] if isinstance(value, list) else value[part]
if not isinstance(value, list):
    raise SystemExit("expected JSON array")
if len(value) < int(sys.argv[3]):
    raise SystemExit(f"expected at least {sys.argv[3]} rows, got {len(value)}")
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
    "exp": now + 1800,
}
signing_input = ".".join([
    b64url(json.dumps(header, separators=(",", ":")).encode()),
    b64url(json.dumps(payload, separators=(",", ":")).encode()),
])
signature = hmac.new(
    os.environ["STUDYPOT_AUTH_JWT_SECRET"].encode("utf-8"),
    signing_input.encode("ascii"),
    hashlib.sha256,
).digest()
print(signing_input + "." + b64url(signature))
PY
}

write_json() {
  local path="$1"
  local content="$2"
  printf '%s\n' "${content}" >"${path}"
}

request_json() {
  local label="$1"
  local method="$2"
  local path="$3"
  local token="$4"
  local body_file="$5"
  local output_file="$6"
  local expected_regex="$7"
  local status url

  url="${STUDYPOT_PROD_BASE_URL}${path}"
  if [[ -n "${body_file}" ]]; then
    status="$(curl -sS --max-time 180 -o "${output_file}" -w "%{http_code}" \
      -X "${method}" "${url}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      --data @"${body_file}")"
  else
    status="$(curl -sS --max-time 180 -o "${output_file}" -w "%{http_code}" \
      -X "${method}" "${url}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json")"
  fi

  note "http.${label}=${status}"
  if [[ ! "${status}" =~ ${expected_regex} ]]; then
    printf 'remote_error=%s %s returned HTTP %s; body_prefix=' "${method}" "${path}" "${status}" >&2
    head -c 800 "${output_file}" >&2 || true
    printf '\n' >&2
    exit 1
  fi
}

mysql_exec() {
  docker exec -e MYSQL_PWD="${SPRING_DATASOURCE_PASSWORD}" studypot-mysql \
    mysql -u"${SPRING_DATASOURCE_USERNAME}" --batch --raw --skip-column-names "${STUDYPOT_MYSQL_DATABASE}" "$@"
}

db_scalar() {
  mysql_exec -e "$1" | tr -d '\r' | head -n 1
}

seed_user() {
  local user_id="$1"
  local email="$2"
  local nickname="$3"
  local email_sql nickname_sql
  email_sql="$(sql_string_literal "${email}")"
  nickname_sql="$(sql_string_literal "${nickname}")"
  mysql_exec -e "
    insert into users (id, email, email_live_key, nickname, last_login_at, created_at, updated_at)
    values ($(uuid_bin_sql "${user_id}"), ${email_sql}, ${email_sql}, ${nickname_sql}, current_timestamp(6), current_timestamp(6), current_timestamp(6))
    on duplicate key update deleted_at = null, updated_at = current_timestamp(6)
  " >/dev/null
}

cleanup_data() {
  [[ "${STUDYPOT_DB_FIRST_AI_PROD_KEEP_DATA:-false}" != "true" ]] || return 0

  local host_bin member_bin group_bin
  host_bin="$(uuid_bin_sql "${HOST_USER_ID}")"
  member_bin="$(uuid_bin_sql "${MEMBER_USER_ID}")"

  if [[ -n "${GROUP_ID:-}" ]]; then
    group_bin="$(uuid_bin_sql "${GROUP_ID}")"
    mysql_exec -e "delete from notification where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete from ai_conversation_message where conversation_id in (select id from ai_conversation where group_id = ${group_bin})" >/dev/null 2>&1 || true
    mysql_exec -e "delete from ai_conversation where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete r from retrospective r join curriculum_week cw on cw.id = r.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete rv from rule_violation rv join group_rule gr on gr.id = rv.rule_id where gr.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete tc from task_completion tc join weekly_task wt on wt.id = tc.weekly_task_id join curriculum_week cw on cw.id = wt.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete mwp from member_week_progress mwp join curriculum_week cw on cw.id = mwp.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete wt from weekly_task wt join curriculum_week cw on cw.id = wt.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete cw from curriculum_week cw join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete from curriculum where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete mas from member_availability_slot mas join group_member gm on gm.id = mas.member_id where gm.group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete from group_onboarding_response where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete from group_rule where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete from group_member where group_id = ${group_bin}" >/dev/null 2>&1 || true
    mysql_exec -e "delete from llm_usage where group_id = ${group_bin} or user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
    mysql_exec -e "delete from study_group where id = ${group_bin}" >/dev/null 2>&1 || true
  else
    mysql_exec -e "delete from llm_usage where user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
  fi

  mysql_exec -e "delete from refresh_token where user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
  mysql_exec -e "delete from oauth_account where user_id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
  mysql_exec -e "delete from users where id in (${host_bin}, ${member_bin})" >/dev/null 2>&1 || true
}

cleanup() {
  cleanup_data || true
}

load_env_file .env
load_env_file .runtime.env

[[ -n "${STUDYPOT_AUTH_JWT_SECRET:-}" ]] || fail "STUDYPOT_AUTH_JWT_SECRET is required on production host"
[[ -n "${SPRING_DATASOURCE_USERNAME:-}" ]] || fail "SPRING_DATASOURCE_USERNAME is required on production host"
[[ -n "${SPRING_DATASOURCE_PASSWORD:-}" ]] || fail "SPRING_DATASOURCE_PASSWORD is required on production host"
[[ -n "${STUDYPOT_MYSQL_DATABASE:-}" ]] || fail "STUDYPOT_MYSQL_DATABASE is required on production host"

GROUP_ID=""
HOST_USER_ID="$(new_uuid)"
MEMBER_USER_ID="$(new_uuid)"
HOST_EMAIL="db-first-host-${STUDYPOT_DB_FIRST_AI_PROD_RUN_ID}-${HOST_USER_ID}@studypot.invalid"
MEMBER_EMAIL="db-first-member-${STUDYPOT_DB_FIRST_AI_PROD_RUN_ID}-${MEMBER_USER_ID}@studypot.invalid"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"; cleanup' EXIT

note "env.ai_mode=${STUDYPOT_AI_OPENAI_API_MODE:-missing}"
note "env.ai_model=${STUDYPOT_AI_OPENAI_MODEL:-missing}"
note "env.base_url=${STUDYPOT_PROD_BASE_URL}"
note "db.mysql_container=$(docker ps --format '{{.Names}}' | grep -Fx 'studypot-mysql' || true)"
note "db.api_container=$(docker ps --format '{{.Names}}' | grep -Fx 'studypot-api' || true)"

curl -fsS --max-time 20 "${STUDYPOT_PROD_BASE_URL}/actuator/health" >/dev/null
note "health=UP"
mysql_exec -e "select 1" >/dev/null
note "db.connectivity=OK"

cleanup_data
seed_user "${HOST_USER_ID}" "${HOST_EMAIL}" "DB First Host"
seed_user "${MEMBER_USER_ID}" "${MEMBER_EMAIL}" "DB First Member"
note "seed.users=2"

HOST_TOKEN="$(generate_jwt "${HOST_USER_ID}")"
MEMBER_TOKEN="$(generate_jwt "${MEMBER_USER_ID}")"
note "auth.jwt_generated=2"

read -r STARTS_AT ENDS_AT < <(python3 - <<'PY'
from datetime import date, timedelta
today = date.today()
print(today.isoformat(), (today + timedelta(days=42)).isoformat())
PY
)

write_json "${TMP_DIR}/create-group.json" "{
  \"name\": \"DB-first AI Prod ${STUDYPOT_DB_FIRST_AI_PROD_RUN_ID}\",
  \"topic\": \"Spring Boot\",
  \"detailKeywords\": [\"JPA\", \"Spring Security\", \"Testing\"],
  \"maxMembers\": 6,
  \"startsAt\": \"${STARTS_AT}\",
  \"endsAt\": \"${ENDS_AT}\",
  \"description\": \"DB-first AI production verification run.\"
}"
request_json create_group POST "/api/v1/groups" "${HOST_TOKEN}" "${TMP_DIR}/create-group.json" "${TMP_DIR}/create-group.response.json" '^201$'
GROUP_ID="$(json_get "${TMP_DIR}/create-group.response.json" "id")"
INVITE_CODE="$(json_get "${TMP_DIR}/create-group.response.json" "inviteCode")"
note "ids.group=${GROUP_ID}"

write_json "${TMP_DIR}/join-group.json" "{\"inviteCode\":\"${INVITE_CODE}\"}"
request_json join_group POST "/api/v1/groups/${GROUP_ID}/join" "${MEMBER_TOKEN}" "${TMP_DIR}/join-group.json" "${TMP_DIR}/join-group.response.json" '^2'

write_json "${TMP_DIR}/host-onboarding.json" '{
  "skillLevel": 4,
  "additionalNote": "Host wants JPA relation mapping, Spring Security filters, and testing practice. credential-like token=should-not-leak.",
  "availabilitySlots": [
    {"dayOfWeek": 2, "startTime": "20:00", "endTime": "22:00", "timezone": "Asia/Seoul"}
  ]
}'
request_json host_onboarding POST "/api/v1/groups/${GROUP_ID}/onboarding/me" "${HOST_TOKEN}" "${TMP_DIR}/host-onboarding.json" "${TMP_DIR}/host-onboarding.response.json" '^2'

write_json "${TMP_DIR}/member-onboarding.json" '{
  "skillLevel": 2,
  "additionalNote": "Member needs smaller assignments and concrete examples before implementation.",
  "availabilitySlots": [
    {"dayOfWeek": 4, "startTime": "19:30", "endTime": "21:30", "timezone": "Asia/Seoul"}
  ]
}'
request_json member_onboarding POST "/api/v1/groups/${GROUP_ID}/onboarding/me" "${MEMBER_TOKEN}" "${TMP_DIR}/member-onboarding.json" "${TMP_DIR}/member-onboarding.response.json" '^2'

request_json start_study POST "/api/v1/groups/${GROUP_ID}/start" "${HOST_TOKEN}" "" "${TMP_DIR}/start-study.response.json" '^201$'
CURRICULUM_ID="$(json_get "${TMP_DIR}/start-study.response.json" "id")"
[[ -n "${CURRICULUM_ID}" ]] || fail "curriculum id is required"
note "ids.curriculum=${CURRICULUM_ID}"

request_json current_week GET "/api/v1/groups/${GROUP_ID}/weeks/current" "${HOST_TOKEN}" "" "${TMP_DIR}/current-week.response.json" '^2'
WEEK_ID="$(json_get "${TMP_DIR}/current-week.response.json" "id")"
[[ -n "${WEEK_ID}" ]] || fail "week id is required"
note "ids.week=${WEEK_ID}"

request_json tasks GET "/api/v1/weeks/${WEEK_ID}/tasks" "${HOST_TOKEN}" "" "${TMP_DIR}/tasks.response.json" '^2'
json_assert_min_length "${TMP_DIR}/tasks.response.json" "" 1
TASK_ID="$(json_get "${TMP_DIR}/tasks.response.json" "0.id")"
note "ids.task=${TASK_ID}"
TASK_BIN="$(uuid_bin_sql "${TASK_ID}")"
mysql_exec -e "update weekly_task set due_at = timestampadd(second, -60, utc_timestamp(6)) where id = ${TASK_BIN}" >/dev/null
note "task.due_at=past"

write_json "${TMP_DIR}/progress.json" '{
  "status": "INCOMPLETE",
  "incompleteReason": "Need more time for Spring Security filter-chain practice."
}'
request_json progress PUT "/api/v1/weeks/${WEEK_ID}/progress/me" "${HOST_TOKEN}" "${TMP_DIR}/progress.json" "${TMP_DIR}/progress.response.json" '^2'

write_json "${TMP_DIR}/task-completion.json" '{
  "status": "INCOMPLETE",
  "incompleteReason": "Could not finish JWT authorization testing before the deadline."
}'
request_json task_completion POST "/api/v1/tasks/${TASK_ID}/completion/me" "${HOST_TOKEN}" "${TMP_DIR}/task-completion.json" "${TMP_DIR}/task-completion.response.json" '^2'

request_json retrospective POST "/api/v1/weeks/${WEEK_ID}/retrospectives/me" "${HOST_TOKEN}" "" "${TMP_DIR}/retrospective.response.json" '^2'
RETROSPECTIVE_ID="$(json_get "${TMP_DIR}/retrospective.response.json" "id")"
RETROSPECTIVE_STATUS="$(json_get "${TMP_DIR}/retrospective.response.json" "status")"
[[ "${RETROSPECTIVE_STATUS}" == "COMPLETED" ]] || fail "expected retrospective status COMPLETED, got ${RETROSPECTIVE_STATUS}"
note "ids.retrospective=${RETROSPECTIVE_ID}"
note "retrospective.status=${RETROSPECTIVE_STATUS}"

write_json "${TMP_DIR}/open-conversation.json" "{
  \"conversationType\": \"TEAM_LEAD_CHAT\",
  \"weekId\": \"${WEEK_ID}\"
}"
request_json open_conversation POST "/api/v1/groups/${GROUP_ID}/ai-conversations" "${HOST_TOKEN}" "${TMP_DIR}/open-conversation.json" "${TMP_DIR}/open-conversation.response.json" '^201$'
CONVERSATION_ID="$(json_get "${TMP_DIR}/open-conversation.response.json" "id")"
note "ids.conversation=${CONVERSATION_ID}"

write_json "${TMP_DIR}/send-message.json" '{
  "content": "이번 주 미완료 사유를 보고 다음 주 학습량과 난이도 조정을 짧게 제안해줘."
}'
request_json send_message POST "/api/v1/ai-conversations/${CONVERSATION_ID}/messages" "${HOST_TOKEN}" "${TMP_DIR}/send-message.json" "${TMP_DIR}/send-message.response.json" '^201$'
CHAT_SENDER="$(json_get "${TMP_DIR}/send-message.response.json" "senderType")"
[[ "${CHAT_SENDER}" == "ASSISTANT" ]] || fail "expected assistant response, got ${CHAT_SENDER}"
note "chat.sender=${CHAT_SENDER}"

group_bin="$(uuid_bin_sql "${GROUP_ID}")"
conversation_bin="$(uuid_bin_sql "${CONVERSATION_ID}")"
retrospective_bin="$(uuid_bin_sql "${RETROSPECTIVE_ID}")"

INPUT_SUMMARY="$(db_scalar "select input_summary from retrospective where id = ${retrospective_bin}")"
INPUT_SUMMARY_JSON="${INPUT_SUMMARY}" python3 - <<'PY'
import json
import os

summary = json.loads(os.environ["INPUT_SUMMARY_JSON"])
required = [
    "progress",
    "taskCompletionCounts",
    "tasks",
    "onboarding",
    "rules",
    "ruleViolations",
    "priorRetrospectives",
    "conversationSummary",
]
missing = [key for key in required if key not in summary]
if missing:
    raise SystemExit("missing input_summary keys: " + ",".join(missing))
if not summary["tasks"]:
    raise SystemExit("input_summary tasks must not be empty")
progress = summary["progress"]
if progress.get("status") != "INCOMPLETE":
    raise SystemExit("input_summary progress status must be INCOMPLETE")
if "Need more time" not in progress.get("incompleteReason", ""):
    raise SystemExit("input_summary progress incompleteReason was not captured")
task = summary["tasks"][0]
if task.get("status") != "INCOMPLETE":
    raise SystemExit("input_summary task status must be INCOMPLETE")
if "JWT authorization" not in task.get("incompleteReason", ""):
    raise SystemExit("input_summary task incompleteReason was not captured")
if summary["onboarding"].get("status") not in {"SUBMITTED", "COMPLETED"}:
    raise SystemExit("input_summary onboarding was not available")
print("context.retrospective.input_summary_keys=" + ",".join(required))
print("context.retrospective.task_count=" + str(len(summary["tasks"])))
print("context.retrospective.progress_status=" + progress.get("status", ""))
PY

CHAT_METADATA_VERSION="$(db_scalar "select json_unquote(json_extract(metadata, '$.retrievalContextVersion')) from ai_conversation_message where conversation_id = ${conversation_bin} and sender_type = 'ASSISTANT' order by created_at desc limit 1")"
[[ "${CHAT_METADATA_VERSION}" == "db-first-v1" ]] || fail "expected retrievalContextVersion db-first-v1, got ${CHAT_METADATA_VERSION}"
note "chat.metadata.retrievalContextVersion=${CHAT_METADATA_VERSION}"

CHAT_SUMMARY_LENGTH="$(db_scalar "select length(coalesce(summary, '')) from ai_conversation where id = ${conversation_bin}")"
[[ "${CHAT_SUMMARY_LENGTH}" -gt 0 ]] || fail "expected conversation summary to be updated"
note "chat.summary_length=${CHAT_SUMMARY_LENGTH}"

USAGE_ROWS="$(mysql_exec -e "
  select purpose, status, model, input_tokens, output_tokens, latency_ms, request_payload
  from llm_usage
  where group_id = ${group_bin}
    and purpose in ('CURRICULUM_GENERATE','RETROSPECTIVE_FEEDBACK','TEAM_LEAD_CHAT')
  order by created_at asc
")"
USAGE_ROWS="${USAGE_ROWS}" python3 - <<'PY'
import json
import os

required = {"CURRICULUM_GENERATE", "RETROSPECTIVE_FEEDBACK", "TEAM_LEAD_CHAT"}
rows = []
for line in os.environ["USAGE_ROWS"].splitlines():
    if not line.strip():
        continue
    parts = line.split("\t", 6)
    if len(parts) != 7:
        raise SystemExit("unexpected usage row shape")
    purpose, status, model, input_tokens, output_tokens, latency_ms, payload = parts
    rows.append((purpose, status, model, int(input_tokens), int(output_tokens), int(latency_ms), json.loads(payload)))

seen = {row[0] for row in rows}
missing = required - seen
if missing:
    raise SystemExit("missing llm_usage purposes: " + ",".join(sorted(missing)))

latest = {}
for row in rows:
    latest[row[0]] = row

for purpose in sorted(required):
    purpose, status, model, input_tokens, output_tokens, latency_ms, payload = latest[purpose]
    if status != "SUCCESS":
        raise SystemExit(f"{purpose} status must be SUCCESS, got {status}")
    if input_tokens <= 0 or output_tokens <= 0 or latency_ms <= 0:
        raise SystemExit(f"{purpose} token/latency evidence must be positive")
    print(f"usage.{purpose}=status:{status},model:{model},input_tokens:{input_tokens},output_tokens:{output_tokens},latency_ms:{latency_ms}")

chat_payload = latest["TEAM_LEAD_CHAT"][6]
if chat_payload.get("purpose") != "TEAM_LEAD_CHAT":
    raise SystemExit("TEAM_LEAD_CHAT request payload purpose mismatch")
if chat_payload.get("conversationType") != "TEAM_LEAD_CHAT":
    raise SystemExit("TEAM_LEAD_CHAT request payload conversationType mismatch")
if int(chat_payload.get("recentMessageCount", 0)) < 1:
    raise SystemExit("TEAM_LEAD_CHAT request payload recentMessageCount must be positive")
if int(chat_payload.get("taskCount", 0)) < 1:
    raise SystemExit("TEAM_LEAD_CHAT request payload taskCount must be positive")
retro_payload = latest["RETROSPECTIVE_FEEDBACK"][6]
if int(retro_payload.get("taskCount", 0)) < 1:
    raise SystemExit("RETROSPECTIVE_FEEDBACK request payload taskCount must be positive")
print("usage.TEAM_LEAD_CHAT.request_payload=conversationType:TEAM_LEAD_CHAT,recentMessageCount:%s,taskCount:%s" % (
    chat_payload.get("recentMessageCount"),
    chat_payload.get("taskCount"),
))
print("usage.RETROSPECTIVE_FEEDBACK.request_payload=taskCount:%s,conversationSummaryStatus:%s" % (
    retro_payload.get("taskCount"),
    retro_payload.get("conversationSummaryStatus"),
))
PY

row_counts="$(mysql_exec -e "
  select 'retrospective', count(*) from retrospective r join curriculum_week cw on cw.id = r.curriculum_week_id join curriculum c on c.id = cw.curriculum_id where c.group_id = ${group_bin}
  union all select 'ai_conversation', count(*) from ai_conversation where group_id = ${group_bin}
  union all select 'ai_conversation_message', count(*) from ai_conversation_message m join ai_conversation c on c.id = m.conversation_id where c.group_id = ${group_bin}
  union all select 'llm_usage', count(*) from llm_usage where group_id = ${group_bin}
")"
printf '%s\n' "${row_counts}" | while IFS=$'\t' read -r label count; do
  note "db.count.${label}=${count}"
done

cleanup_data
leftover_users="$(db_scalar "select count(*) from users where email like 'db-first-%-${STUDYPOT_DB_FIRST_AI_PROD_RUN_ID}-%@studypot.invalid'")"
leftover_groups="$(db_scalar "select count(*) from study_group where name = 'DB-first AI Prod ${STUDYPOT_DB_FIRST_AI_PROD_RUN_ID}'")"
note "cleanup.leftover_test_users=${leftover_users}"
note "cleanup.leftover_test_groups=${leftover_groups}"
[[ "${leftover_users}" == "0" ]] || fail "test users were not cleaned up"
[[ "${leftover_groups}" == "0" ]] || fail "test group was not cleaned up"

note "result=PASS"
REMOTE

python3 - "${RAW_LOG}" "${SUMMARY_FILE}" "${RUN_ID}" "${BASE_URL}" <<'PY'
import sys
from pathlib import Path

raw_log = Path(sys.argv[1])
summary = Path(sys.argv[2])
run_id = sys.argv[3]
base_url = sys.argv[4]

lines = [
    line.strip()
    for line in raw_log.read_text(encoding="utf-8", errors="replace").splitlines()
    if line.strip()
]

summary.write_text(
    "\n".join([
        "# DB-first AI Production Verification",
        "",
        f"- run_id: `{run_id}`",
        f"- base_url: `{base_url}`",
        "- secret_handling: `redacted`",
        "- vector_rag: `not verified / not implemented by this script`",
        "- db_first_context: `verified through deployed backend APIs and MySQL evidence`",
        "",
        "## Evidence",
        *[f"- `{line}`" for line in lines if not line.startswith("remote_error=")],
        "",
    ]),
    encoding="utf-8",
)
PY

grep -Fq 'result=PASS' "${RAW_LOG}" || fail "production DB-first AI verification did not report PASS"

note "DB-first AI production verification passed."
note "Redacted evidence log: ${RAW_LOG}"
note "Redacted evidence summary: ${SUMMARY_FILE}"
