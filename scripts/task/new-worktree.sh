#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./common.sh
source "${SCRIPT_DIR}/common.sh"

slug="${1:-}"
[[ -n "${slug}" ]] || fail "usage: new-worktree.sh <slug>"

validate_slug "${slug}"
load_task_env "${slug}"

repo="$(repo_root)"
git -C "${repo}" rev-parse --verify "${BASE_BRANCH}" >/dev/null 2>&1 || fail "base branch does not exist: ${BASE_BRANCH}"
git -C "${repo}" show-ref --verify --quiet "refs/heads/${FEATURE_BRANCH}" && fail "feature branch already exists: ${FEATURE_BRANCH}"
[[ ! -e "${WORKTREE}" ]] || fail "worktree path already exists: ${WORKTREE}"

mkdir -p "$(dirname "${WORKTREE}")"
git -C "${repo}" worktree add "${WORKTREE}" -b "${FEATURE_BRANCH}" "${BASE_BRANCH}" >/dev/null

write_task_env "${slug}"
printf '%s\n' "${WORKTREE}"
