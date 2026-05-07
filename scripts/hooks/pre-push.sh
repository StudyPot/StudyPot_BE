#!/usr/bin/env bash

set -euo pipefail

SOURCE="${BASH_SOURCE[0]}"
while [[ -L "${SOURCE}" ]]; do
  DIR="$(cd -P "$(dirname "${SOURCE}")" && pwd)"
  SOURCE="$(readlink "${SOURCE}")"
  [[ "${SOURCE}" != /* ]] && SOURCE="${DIR}/${SOURCE}"
done
SCRIPT_DIR="$(cd -P "$(dirname "${SOURCE}")" && pwd)"
# shellcheck source=scripts/task/common.sh
source "${SCRIPT_DIR}/../task/common.sh"

repo="$(repo_root)"
branch="$(git -C "${repo}" branch --show-current)"

[[ "${branch}" != "main" ]] || fail "main branch에서는 push할 수 없습니다. codex/<slug> branch와 create-pr.sh를 사용하세요."
[[ "${branch}" =~ ^codex/([a-z0-9][a-z0-9-]*)$ ]] || fail "push는 codex/<slug> branch에서만 허용됩니다."
slug="${BASH_REMATCH[1]}"

env_file="$(task_env_path "${slug}")"
[[ -f "${env_file}" ]] || fail "task state file이 없습니다: ${env_file}"

load_task_env "${slug}"
[[ -n "${EXEC_PLAN}" && -f "${EXEC_PLAN}" ]] || fail "EXEC_PLAN이 없습니다."
plan_section_filled "${EXEC_PLAN}" "Goal" || fail "EXEC_PLAN의 Goal을 채워야 합니다."
plan_section_filled "${EXEC_PLAN}" "Approach" || fail "EXEC_PLAN의 Approach를 채워야 합니다."
plan_section_filled "${EXEC_PLAN}" "Step Plan" || fail "EXEC_PLAN의 Step Plan을 채워야 합니다."
plan_section_filled "${EXEC_PLAN}" "Done Criteria" || fail "EXEC_PLAN의 Done Criteria를 채워야 합니다."
plan_has_checked_related_doc "${EXEC_PLAN}" || fail "작업 관련 docs를 최소 1개 이상 Related Docs에 기록해야 합니다."
plan_has_checked_feature_id "${EXEC_PLAN}" || fail "EXEC_PLAN의 Related Feature IDs에 체크된 feature id를 최소 1개 이상 기록해야 합니다."
plan_doc_notes_filled "${EXEC_PLAN}" || fail "Doc Notes에 문서에서 반영한 내용을 적어야 합니다."

verify_command="${STRICT_VERIFY_COMMAND:-./gradlew check build --no-daemon}"
[[ "${verify_command}" != TODO:* ]] || fail "verification command is not configured."
(
  cd "${repo}"
  while IFS= read -r git_env_var; do
    unset "${git_env_var}"
  done < <(git rev-parse --local-env-vars)
  eval "${verify_command}"
) || fail "verification command failed: ${verify_command}"

LAST_VERIFY_COMMAND="${verify_command}"
LAST_VERIFY_STATUS="passed"
LAST_VERIFY_AT="$(date '+%Y-%m-%dT%H:%M:%S%z')"
export LAST_VERIFY_COMMAND LAST_VERIFY_STATUS LAST_VERIFY_AT
write_task_env "${slug}"
