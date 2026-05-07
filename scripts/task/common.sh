#!/usr/bin/env bash

set -euo pipefail

fail() {
  echo "Error: $*" >&2
  exit 1
}

commit_subject_types() {
  printf '%s\n' "feat fix chore docs test refactor ci build perf style revert"
}

commit_subject_contains_korean() {
  SUBJECT_DESCRIPTION="$1" python3 - <<'PY'
import os
import sys

description = os.environ["SUBJECT_DESCRIPTION"]
sys.exit(0 if any("\uac00" <= char <= "\ud7a3" for char in description) else 1)
PY
}

validate_commit_subject() {
  local subject="$1"
  local label="${2:-commit subject}"
  local types_regex subject_regex description

  types_regex="$(commit_subject_types | tr ' ' '|')"
  subject_regex="^\\[(${types_regex})\\] .+"
  [[ "${subject}" =~ ${subject_regex} ]] || fail "${label} must match '[type] 한글 내용' where type is one of: $(commit_subject_types)"

  description="${subject#*] }"
  commit_subject_contains_korean "${description}" || fail "${label} description must include Korean text."
}

repo_root() {
  if [[ -n "${STRICT_REPO_ROOT:-}" ]]; then
    (cd "${STRICT_REPO_ROOT}" && pwd)
  else
    git rev-parse --show-toplevel
  fi
}

current_branch_or_main() {
  local repo branch
  repo="$(repo_root)"
  branch="$(git -C "${repo}" branch --show-current 2>/dev/null || true)"
  [[ -n "${branch}" ]] && printf '%s\n' "${branch}" || printf 'main\n'
}

validate_slug() {
  local slug="$1"
  [[ "${slug}" =~ ^[a-z0-9][a-z0-9-]*$ ]] || fail "slug must match ^[a-z0-9][a-z0-9-]*$: ${slug}"
}

plan_dir() {
  if [[ -n "${STRICT_PLAN_DIR:-}" ]]; then
    mkdir -p "${STRICT_PLAN_DIR}"
    (cd "${STRICT_PLAN_DIR}" && pwd)
  else
    local repo
    repo="$(repo_root)"
    mkdir -p "${repo}/docs/exec-plans/active"
    (cd "${repo}/docs/exec-plans/active" && pwd)
  fi
}

task_state_dir() {
  if [[ -n "${STRICT_TASK_STATE_DIR:-}" ]]; then
    mkdir -p "${STRICT_TASK_STATE_DIR}"
    (cd "${STRICT_TASK_STATE_DIR}" && pwd)
  else
    local repo
    repo="$(repo_root)"
    mkdir -p "${repo}/.codex/task-state"
    (cd "${repo}/.codex/task-state" && pwd)
  fi
}

worktree_base_dir() {
  if [[ -n "${STRICT_WORKTREE_BASE_DIR:-}" ]]; then
    mkdir -p "${STRICT_WORKTREE_BASE_DIR}"
    (cd "${STRICT_WORKTREE_BASE_DIR}" && pwd)
  else
    printf '%s\n' "/Users/hyunwoo/Documents/New project 3-worktrees"
  fi
}

log_base_dir() {
  if [[ -n "${STRICT_LOG_BASE_DIR:-}" ]]; then
    mkdir -p "${STRICT_LOG_BASE_DIR}"
    (cd "${STRICT_LOG_BASE_DIR}" && pwd)
  else
    printf '%s\n' "/Users/hyunwoo/Documents/New project 3-logs"
  fi
}

ports_registry() {
  if [[ -n "${STRICT_PORT_REGISTRY:-}" ]]; then
    mkdir -p "$(dirname "${STRICT_PORT_REGISTRY}")"
    printf '%s\n' "${STRICT_PORT_REGISTRY}"
  else
    printf '%s/ports.tsv\n' "$(task_state_dir)"
  fi
}

task_env_path() {
  local slug="$1"
  printf '%s/%s.env\n' "$(task_state_dir)" "${slug}"
}

default_feature_branch() {
  local slug="$1"
  printf 'codex/%s\n' "${slug}"
}

expected_worktree_path() {
  local slug="$1"
  printf '%s/%s\n' "$(worktree_base_dir)" "${slug}"
}

expected_log_dir() {
  local slug="$1"
  printf '%s/%s\n' "$(log_base_dir)" "${slug}"
}

primary_worktree_path() {
  local repo
  repo="$(repo_root)"
  git -C "${repo}" worktree list --porcelain | awk '
    /^worktree / {
      sub(/^worktree /, "")
      print
      exit
    }
  '
}

today_stamp() {
  date '+%Y%m%d'
}

find_existing_plan_for_slug() {
  local slug="$1"
  find "$(plan_dir)" -maxdepth 1 -type f -name "*-${slug}.md" | head -n 1 || true
}

load_task_env() {
  local slug="$1"
  local env_file

  TASK_SLUG="${slug}"
  TASK_TITLE="${slug}"
  BASE_BRANCH="$(current_branch_or_main)"
  FEATURE_BRANCH="$(default_feature_branch "${slug}")"
  EXEC_PLAN=""
  WORKTREE="$(expected_worktree_path "${slug}")"
  PORT=""
  LOG_DIR="$(expected_log_dir "${slug}")"
  LAST_VERIFY_COMMAND=""
  LAST_VERIFY_STATUS=""
  LAST_VERIFY_AT=""
  JIRA_ISSUE_KEY=""
  JIRA_ISSUE_URL=""
  JIRA_ISSUE_SUMMARY=""
  JIRA_STARTED_AT=""
  JIRA_DONE_AT=""

  env_file="$(task_env_path "${slug}")"
  if [[ -f "${env_file}" ]]; then
    # shellcheck disable=SC1090
    source "${env_file}"
  fi
}

write_task_env() {
  local slug="$1"
  local env_file mirror_env_file current_repo primary_repo primary_env_file
  env_file="$(task_env_path "${slug}")"

  mkdir -p "$(dirname "${env_file}")"
  {
    printf 'TASK_SLUG=%q\n' "${TASK_SLUG}"
    printf 'TASK_TITLE=%q\n' "${TASK_TITLE}"
    printf 'BASE_BRANCH=%q\n' "${BASE_BRANCH}"
    printf 'FEATURE_BRANCH=%q\n' "${FEATURE_BRANCH}"
    printf 'EXEC_PLAN=%q\n' "${EXEC_PLAN}"
    printf 'WORKTREE=%q\n' "${WORKTREE}"
    printf 'PORT=%q\n' "${PORT}"
    printf 'LOG_DIR=%q\n' "${LOG_DIR}"
    printf 'LAST_VERIFY_COMMAND=%q\n' "${LAST_VERIFY_COMMAND}"
    printf 'LAST_VERIFY_STATUS=%q\n' "${LAST_VERIFY_STATUS}"
    printf 'LAST_VERIFY_AT=%q\n' "${LAST_VERIFY_AT}"
    printf 'JIRA_ISSUE_KEY=%q\n' "${JIRA_ISSUE_KEY}"
    printf 'JIRA_ISSUE_URL=%q\n' "${JIRA_ISSUE_URL}"
    printf 'JIRA_ISSUE_SUMMARY=%q\n' "${JIRA_ISSUE_SUMMARY}"
    printf 'JIRA_STARTED_AT=%q\n' "${JIRA_STARTED_AT}"
    printf 'JIRA_DONE_AT=%q\n' "${JIRA_DONE_AT}"
  } > "${env_file}"

  current_repo="$(repo_root)"
  if [[ -n "${WORKTREE:-}" && -d "${WORKTREE}" && "${WORKTREE}" != "${current_repo}" ]]; then
    mirror_env_file="${WORKTREE}/.codex/task-state/${slug}.env"
    mkdir -p "$(dirname "${mirror_env_file}")"
    [[ "${env_file}" -ef "${mirror_env_file}" ]] || cp "${env_file}" "${mirror_env_file}"
  fi

  primary_repo="$(primary_worktree_path 2>/dev/null || true)"
  if [[ -n "${primary_repo}" && -d "${primary_repo}" && "${primary_repo}" != "${current_repo}" ]]; then
    primary_env_file="${primary_repo}/.codex/task-state/${slug}.env"
    mkdir -p "$(dirname "${primary_env_file}")"
    [[ "${env_file}" -ef "${primary_env_file}" ]] || cp "${env_file}" "${primary_env_file}"
  fi
}

replace_port_registry_entry() {
  local slug="$1"
  local port="$2"
  local registry tmp_file
  registry="$(ports_registry)"
  tmp_file="$(mktemp)"

  mkdir -p "$(dirname "${registry}")"
  touch "${registry}"
  awk -F '\t' -v slug="${slug}" '$1 != slug { print $0 }' "${registry}" > "${tmp_file}"
  printf '%s\t%s\n' "${slug}" "${port}" >> "${tmp_file}"
  mv "${tmp_file}" "${registry}"
}

port_reserved_by_other_slug() {
  local slug="$1"
  local port="$2"
  local registry
  registry="$(ports_registry)"
  [[ -f "${registry}" ]] || return 1
  awk -F '\t' -v slug="${slug}" -v port="${port}" '$1 != slug && $2 == port { found=1 } END { exit found ? 0 : 1 }' "${registry}"
}

port_listening() {
  local port="$1"
  command -v lsof >/dev/null 2>&1 && lsof -iTCP:"${port}" -sTCP:LISTEN -n -P >/dev/null 2>&1
}

plan_section_filled() {
  local plan_file="$1"
  local heading="$2"
  awk -v target="## ${heading}" '
    $0 == target { in_section = 1; next }
    in_section && /^## / { exit found ? 0 : 1 }
    in_section {
      if ($0 ~ /^[[:space:]]*$/) next
      if ($0 ~ /^[[:space:]]*(TBD|TODO)[[:space:]]*$/) next
      if ($0 ~ /^[[:space:]]*-\s*(TBD|TODO)?[[:space:]]*$/) next
      found = 1
      exit 0
    }
    END { exit found ? 0 : 1 }
  ' "${plan_file}"
}

plan_has_checked_required_read() {
  local plan_file="$1"
  local doc_path="$2"
  grep -Eq "^[[:space:]]*-[[:space:]]*\\[x\\][[:space:]]+${doc_path//\//\\/}[[:space:]]*$" "${plan_file}"
}

plan_has_checked_related_doc() {
  local plan_file="$1"
  awk '
    /^## Related Docs$/ { in_section = 1; next }
    in_section && /^## / { exit found ? 0 : 1 }
    in_section && $0 ~ /^[[:space:]]*-[[:space:]]*\[x\][[:space:]]+docs\/[^[:space:]]+/ { found = 1; exit 0 }
    END { exit found ? 0 : 1 }
  ' "${plan_file}"
}

plan_has_checked_feature_id() {
  local plan_file="$1"
  awk '
    /^## Related Feature IDs$/ { in_section = 1; next }
    in_section && /^## / { exit found ? 0 : 1 }
    in_section && $0 ~ /^[[:space:]]*-[[:space:]]*\[x\][[:space:]]+[^<[:space:]][^[:space:]]*/ { found = 1; exit 0 }
    END { exit found ? 0 : 1 }
  ' "${plan_file}"
}

plan_doc_notes_filled() {
  plan_section_filled "$1" "Doc Notes"
}
