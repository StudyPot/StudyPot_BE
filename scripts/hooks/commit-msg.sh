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

message_file="${1:-}"
[[ -n "${message_file}" && -f "${message_file}" ]] || {
  echo "Error: commit message file is required." >&2
  exit 1
}

subject="$(head -n 1 "${message_file}")"
validate_commit_subject "${subject}" "commit message subject"
