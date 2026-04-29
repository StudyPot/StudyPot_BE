#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./common.sh
source "${SCRIPT_DIR}/common.sh"

slug="${1:-}"
[[ -n "${slug}" ]] || fail "usage: allocate-port.sh <slug>"

validate_slug "${slug}"
load_task_env "${slug}"

if [[ -z "${PORT}" ]]; then
  for port in $(seq 18080 18199); do
    if ! port_reserved_by_other_slug "${slug}" "${port}" && ! port_listening "${port}"; then
      PORT="${port}"
      break
    fi
  done
fi

[[ -n "${PORT}" ]] || fail "no free port found"
replace_port_registry_entry "${slug}" "${PORT}"
write_task_env "${slug}"
printf '%s\n' "${PORT}"
