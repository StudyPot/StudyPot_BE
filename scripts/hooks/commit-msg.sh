#!/usr/bin/env bash

set -euo pipefail

message_file="${1:-}"
[[ -n "${message_file}" && -f "${message_file}" ]] || {
  echo "Error: commit message file is required." >&2
  exit 1
}

subject="$(head -n 1 "${message_file}")"
[[ "${subject}" =~ ^\[feat\]\ .+ ]] || {
  echo "Error: commit message must match '[feat] 설명'." >&2
  exit 1
}
