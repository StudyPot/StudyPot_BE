#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./common.sh
source "${SCRIPT_DIR}/common.sh"

slug="${1:-}"
[[ -n "${slug}" ]] || fail "usage: create-log-dir.sh <slug>"

validate_slug "${slug}"
load_task_env "${slug}"

mkdir -p "${LOG_DIR}"
write_task_env "${slug}"
printf '%s\n' "${LOG_DIR}"
