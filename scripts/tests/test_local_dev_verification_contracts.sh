#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

assert_contains "test_local_dev_verification_contracts.sh" "${TEST_ROOT}/scripts/tests/run.sh"
assert_file_exists "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
[[ -x "${TEST_ROOT}/scripts/task/verify-local-dev.sh" ]] || fail "verify-local-dev.sh must be executable"

assert_contains "config/application-local.yml" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "application-local.yml" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "SPRING_CONFIG_ADDITIONAL_LOCATION" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "SPRING_PROFILES_ACTIVE=local" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "STUDYPOT_OPENAPI_PUBLIC_DOCS_ENABLED=true" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "/actuator/health" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "/v3/api-docs" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "/swagger-ui.html" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "flyway_schema_history" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "show tables" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "/api/v1/auth/refresh" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"
assert_contains "trap" "${TEST_ROOT}/scripts/task/verify-local-dev.sh"

assert_file_exists "${TEST_ROOT}/config/application-local.example.yml"
assert_contains "spring:" "${TEST_ROOT}/config/application-local.example.yml"
assert_contains "datasource:" "${TEST_ROOT}/config/application-local.example.yml"
assert_contains "flyway:" "${TEST_ROOT}/config/application-local.example.yml"
assert_contains "springdoc:" "${TEST_ROOT}/config/application-local.example.yml"
assert_contains "public-docs-enabled: true" "${TEST_ROOT}/config/application-local.example.yml"
assert_contains "logging:" "${TEST_ROOT}/config/application-local.example.yml"
assert_contains "management:" "${TEST_ROOT}/config/application-local.example.yml"

export TEST_ROOT
python3 - <<'PY'
import os
from pathlib import Path
import sys

path = Path(os.environ["TEST_ROOT"]) / "config" / "application-local.example.yml"
text = path.read_text()
for forbidden in ("apps.googleusercontent.com", "GOCSPX-", "client-secret: \"\"", "password: \"\""):
    if forbidden in text:
        print(f"local example must not contain real or blank-looking secret material: {forbidden}", file=sys.stderr)
        sys.exit(1)
PY
