#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
HOOKS_DIR="${REPO_ROOT}/.githooks"

mkdir -p "${HOOKS_DIR}"
for hook in pre-commit commit-msg pre-push; do
  ln -sf "../scripts/hooks/${hook}.sh" "${HOOKS_DIR}/${hook}"
done
git -C "${REPO_ROOT}" config core.hooksPath .githooks
printf 'Configured core.hooksPath to .githooks in %s\n' "${REPO_ROOT}"
