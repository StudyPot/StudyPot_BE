#!/usr/bin/env bash

set -euo pipefail

TEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_file_exists() {
  [[ -f "$1" ]] || fail "expected file to exist: $1"
}

assert_contains() {
  local needle="$1"
  local haystack="$2"
  grep -Fq -- "${needle}" "${haystack}" || fail "expected '${needle}' in ${haystack}"
}

setup_sandbox_repo() {
  local sandbox="$1"
  mkdir -p "${sandbox}"
  git init "${sandbox}" >/dev/null 2>&1
  git -C "${sandbox}" config user.email "test@example.com"
  git -C "${sandbox}" config user.name "Test User"
  printf '# sandbox\n' > "${sandbox}/README.md"
  git -C "${sandbox}" add README.md
  git -C "${sandbox}" commit -m "init" >/dev/null 2>&1
  git -C "${sandbox}" branch -M main
}

write_fake_jira_api() {
  local target="$1"
  cat > "${target}" <<'STUB'
#!/usr/bin/env bash

set -euo pipefail

method="$1"
path="$2"
body_file="${3:-}"
status_file="${JIRA_FAKE_STATUS_FILE:?JIRA_FAKE_STATUS_FILE is required}"
log_file="${JIRA_FAKE_LOG:-/dev/null}"
issue_type="${JIRA_FAKE_ISSUE_TYPE:-작업}"
summary="${JIRA_FAKE_SUMMARY:-Fake Jira task}"
status="$(cat "${status_file}")"

printf '%s\t%s' "${method}" "${path}" >> "${log_file}"
if [[ -n "${body_file}" && -f "${body_file}" ]]; then
  printf '\t%s' "$(tr -d '\n' < "${body_file}")" >> "${log_file}"
fi
printf '\n' >> "${log_file}"

issue_key="$(printf '%s' "${path}" | sed -E 's#^/rest/api/3/issue/([^/?]+).*$#\1#')"
project_key="${issue_key%%-*}"

if [[ "${method}" == "GET" && "${path}" == /rest/api/3/issue/*\?fields=* ]]; then
  cat <<JSON
{
  "key": "${issue_key}",
  "fields": {
    "project": { "key": "${project_key}" },
    "summary": "${summary}",
    "issuetype": { "name": "${issue_type}" },
    "status": { "name": "${status}" }
  }
}
JSON
  exit 0
fi

if [[ "${method}" == "GET" && "${path}" == /rest/api/3/issue/*/transitions ]]; then
  if [[ "${JIRA_FAKE_NO_TRANSITION:-0}" == "1" ]]; then
    printf '{"transitions":[]}\n'
  else
    cat <<'JSON'
{
  "transitions": [
    { "id": "11", "to": { "name": "해야 할 일" } },
    { "id": "21", "to": { "name": "진행 중" } },
    { "id": "41", "to": { "name": "완료" } }
  ]
}
JSON
  fi
  exit 0
fi

if [[ "${method}" == "POST" && "${path}" == /rest/api/3/issue/*/transitions ]]; then
  if grep -Fq '"id":"21"' "${body_file}"; then
    printf '진행 중\n' > "${status_file}"
  elif grep -Fq '"id":"41"' "${body_file}"; then
    printf '완료\n' > "${status_file}"
  else
    echo "unknown transition body" >&2
    exit 1
  fi
  printf '{}\n'
  exit 0
fi

echo "unexpected fake Jira request: ${method} ${path}" >&2
exit 1
STUB
  chmod +x "${target}"
}
