#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../task/common.sh
source "${SCRIPT_DIR}/../task/common.sh"

repo="$(repo_root)"
branch="$(git -C "${repo}" branch --show-current)"
staged_files="$(git -C "${repo}" diff --cached --name-only --diff-filter=ACMR)"

contains_path() {
  local regex="$1"
  [[ -n "${staged_files}" ]] && grep -Eq "${regex}" <<<"${staged_files}"
}

run_verify_command() {
  local repo="$1"
  local verify_command="$2"
  (
    cd "${repo}"
    while IFS= read -r git_env_var; do
      unset "${git_env_var}"
    done < <(git rev-parse --local-env-vars)
    eval "${verify_command}"
  )
}

if [[ "${branch}" == "main" ]] && contains_path '^src/'; then
  fail "main branch에서 src/를 직접 수정할 수 없습니다. worktree와 codex/<slug> 브랜치를 사용하세요."
fi

[[ "${branch}" =~ ^codex/([a-z0-9][a-z0-9-]*)$ ]] || fail "commit은 codex/<slug> 브랜치에서만 허용됩니다."
slug="${BASH_REMATCH[1]}"

env_file="$(task_env_path "${slug}")"
[[ -f "${env_file}" ]] || fail "task state file이 없습니다: ${env_file}"

load_task_env "${slug}"
[[ -n "${EXEC_PLAN}" && -f "${EXEC_PLAN}" ]] || fail "EXEC_PLAN이 없습니다."
plan_section_filled "${EXEC_PLAN}" "Goal" || fail "EXEC_PLAN의 Goal을 채워야 합니다."
plan_section_filled "${EXEC_PLAN}" "Approach" || fail "EXEC_PLAN의 Approach를 채워야 합니다."
plan_section_filled "${EXEC_PLAN}" "Step Plan" || fail "EXEC_PLAN의 Step Plan을 채워야 합니다."
plan_section_filled "${EXEC_PLAN}" "Done Criteria" || fail "EXEC_PLAN의 Done Criteria를 채워야 합니다."
plan_has_checked_required_read "${EXEC_PLAN}" "AGENTS.md" || fail "AGENTS.md를 필수 읽기 목록에 체크해야 합니다."
plan_has_checked_required_read "${EXEC_PLAN}" "ARCHITECTURE.md" || fail "ARCHITECTURE.md를 필수 읽기 목록에 체크해야 합니다."
plan_has_checked_required_read "${EXEC_PLAN}" "docs/index.md" || fail "docs/index.md를 필수 읽기 목록에 체크해야 합니다."
plan_has_checked_related_doc "${EXEC_PLAN}" || fail "작업 관련 docs를 최소 1개 이상 Related Docs에 기록해야 합니다."
plan_has_checked_feature_id "${EXEC_PLAN}" || fail "EXEC_PLAN의 Related Feature IDs에 체크된 feature id를 최소 1개 이상 기록해야 합니다."
plan_doc_notes_filled "${EXEC_PLAN}" || fail "Doc Notes에 문서에서 반영한 내용을 적어야 합니다."

if contains_path '^src/main/' && ! contains_path '^src/test/'; then
  fail "src/main 변경에는 같은 commit 안에 src/test 변경이 반드시 포함되어야 합니다."
fi

verify_command="${STRICT_VERIFY_COMMAND:-TODO: set verification command}"
run_verify_command "${repo}" "${verify_command}" || fail "verification command failed: ${verify_command}"

LAST_VERIFY_COMMAND="${verify_command}"
LAST_VERIFY_STATUS="passed"
LAST_VERIFY_AT="$(date '+%Y-%m-%dT%H:%M:%S%z')"
write_task_env "${slug}"
