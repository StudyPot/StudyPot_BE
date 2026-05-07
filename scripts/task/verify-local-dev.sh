#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

fail() {
  echo "Error: $*" >&2
  exit 1
}

LOCAL_CONFIG="${STUDYPOT_LOCAL_CONFIG:-${ROOT_DIR}/config/application-local.yml}"
[[ -f "${LOCAL_CONFIG}" ]] || fail "application-local.yml is required. Start from config/application-local.example.yml and keep real secrets outside src/main/resources."
[[ "${LOCAL_CONFIG}" != "${ROOT_DIR}/src/main/resources/"* ]] || fail "local secrets must not live under src/main/resources because build artifacts can package them."

PORT="${STUDYPOT_LOCAL_PORT:-18080}"
DB_HOST="${STUDYPOT_LOCAL_DB_HOST:-127.0.0.1}"
DB_PORT="${STUDYPOT_LOCAL_DB_PORT:-3306}"
DB_NAME="${STUDYPOT_LOCAL_DB_NAME:-studypot}"
DB_USER="${STUDYPOT_LOCAL_DB_USER:-${SPRING_DATASOURCE_USERNAME:-root}}"
DB_PASSWORD="${STUDYPOT_LOCAL_DB_PASSWORD:-${SPRING_DATASOURCE_PASSWORD:-}}"
LOG_DIR="${STUDYPOT_LOCAL_LOG_DIR:-${ROOT_DIR}/build/local-dev}"
APP_LOG="${LOG_DIR}/bootRun-${PORT}.log"
BASE_URL="http://localhost:${PORT}"

mkdir -p "${LOG_DIR}"

mysql_exec() {
  local database="${1}"
  local sql="${2}"
  if [[ -n "${database}" ]]; then
    MYSQL_PWD="${DB_PASSWORD}" mysql --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" \
      --batch --skip-column-names "${database}" -e "${sql}"
  else
    MYSQL_PWD="${DB_PASSWORD}" mysql --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" \
      --batch --skip-column-names -e "${sql}"
  fi
}

wait_for_http() {
  local url="${1}"
  local attempts="${2:-90}"
  local delay="${3:-2}"
  for _ in $(seq 1 "${attempts}"); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      return 0
    fi
    if ! kill -0 "${APP_PID}" >/dev/null 2>&1; then
      tail -120 "${APP_LOG}" >&2 || true
      fail "application exited before ${url} became ready"
    fi
    sleep "${delay}"
  done
  tail -120 "${APP_LOG}" >&2 || true
  fail "timed out waiting for ${url}"
}

cleanup() {
  if [[ -n "${APP_PID:-}" ]] && kill -0 "${APP_PID}" >/dev/null 2>&1; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
    wait "${APP_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "Checking MySQL connectivity on ${DB_HOST}:${DB_PORT}/${DB_NAME}"
mysql_exec "" "select 1" >/dev/null
mysql_exec "" "create database if not exists \`${DB_NAME}\` character set utf8mb4 collate utf8mb4_0900_ai_ci" >/dev/null

echo "Starting Spring Boot local profile on ${BASE_URL}"
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

echo "Checking OpenAPI and Swagger UI"
curl -fsS "${BASE_URL}/v3/api-docs" | python3 -c '
import json, sys
doc = json.load(sys.stdin)
assert doc.get("openapi"), "openapi version is required"
assert doc.get("info", {}).get("title") == "AI Study Leader API", "OpenAPI title mismatch"
assert doc.get("components", {}).get("securitySchemes", {}).get("bearerAuth", {}).get("scheme") == "bearer", "bearer scheme is required"
'

swagger_status="$(curl -sS -o /dev/null -w "%{http_code}" "${BASE_URL}/swagger-ui.html")"
case "${swagger_status}" in
  2*|3*) ;;
  *) fail "Swagger UI returned HTTP ${swagger_status}" ;;
esac

echo "Checking Flyway migration history and table creation"
mysql_exec "${DB_NAME}" "select count(*) from flyway_schema_history" | grep -Eq '^[1-9][0-9]*$'
mysql_exec "${DB_NAME}" "show tables" | grep -F "users" >/dev/null
table_count="$(mysql_exec "${DB_NAME}" "select count(*) from information_schema.tables where table_schema='${DB_NAME}' and table_name in ('users','oauth_account','refresh_token','study_group','group_member','group_onboarding_response','member_availability_slot','group_rule','rule_violation','curriculum','curriculum_week','weekly_task','member_week_progress','task_completion','retrospective','ai_conversation','ai_conversation_message','notification','llm_usage')")"
[[ "${table_count}" == "19" ]] || fail "expected 19 ERD tables, found ${table_count}"

echo "Checking auth service wiring"
refresh_status="$(curl -sS -o "${LOG_DIR}/auth-refresh-response.json" -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"local-dev-invalid-refresh-token"}')"
[[ "${refresh_status}" != "503" ]] || fail "auth service is still not configured"
[[ "${refresh_status}" == "401" ]] || fail "expected invalid refresh token to return 401, got ${refresh_status}"

echo "Local development verification passed: ${BASE_URL}"
