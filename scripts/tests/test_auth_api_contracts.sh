#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/tests/testlib.sh
source "${SCRIPT_DIR}/testlib.sh"

assert_file_exists "${TEST_ROOT}/docs/specs/change-requests/CR-20260506-auth-api-entrypoints.md"
assert_file_exists "${TEST_ROOT}/docs/specs/adr/ADR-20260506-auth-api-entrypoints.md"
assert_contains "POST /api/v1/auth/oauth/google" "${TEST_ROOT}/docs/specs/change-requests/CR-20260506-auth-api-entrypoints.md"
assert_contains "ADR-20260506-auth-api-entrypoints" "${TEST_ROOT}/docs/specs/change-requests/CR-20260506-auth-api-entrypoints.md"
assert_contains "CR-20260506-auth-api-entrypoints" "${TEST_ROOT}/docs/specs/adr/ADR-20260506-auth-api-entrypoints.md"
assert_contains "CR-20260506-auth-api-entrypoints" "${TEST_ROOT}/docs/specs/change-control-v1.md"

assert_contains "/api/v1/auth/oauth/google" "${TEST_ROOT}/docs/specs/api-contract-v1.md"
assert_contains "/api/v1/auth/csrf" "${TEST_ROOT}/docs/specs/api-contract-v1.md"
assert_contains "/api/v1/auth/refresh" "${TEST_ROOT}/docs/specs/api-contract-v1.md"
assert_contains "/api/v1/auth/logout" "${TEST_ROOT}/docs/specs/api-contract-v1.md"
assert_contains "/api/v1/auth/logout-all" "${TEST_ROOT}/docs/specs/api-contract-v1.md"
assert_contains "QA-ID-007" "${TEST_ROOT}/docs/specs/qa-acceptance-v1.md"
assert_contains "QA-ID-001\` to \`QA-ID-007" "${TEST_ROOT}/docs/specs/feature-coverage-matrix.md"
assert_contains "Public auth endpoints" "${TEST_ROOT}/docs/specs/api-contract-v1.md"
assert_contains "Refresh tokens must be stored as hashes and rotated on refresh" "${TEST_ROOT}/docs/specs/auth-permissions-v1.md"

# shellcheck disable=SC2016
ruby -ryaml -e '
doc = YAML.load_file("docs/specs/openapi.yaml")
paths = doc.fetch("paths")
required_paths = [
  "/auth/oauth/google",
  "/auth/csrf",
  "/auth/refresh",
  "/auth/logout",
  "/auth/logout-all"
]
required_paths.each { |path| abort("missing auth path #{path}") unless paths.key?(path) }

abort("oauth login must be public") unless paths.fetch("/auth/oauth/google").fetch("post").fetch("security") == []
abort("csrf bootstrap must be public") unless paths.fetch("/auth/csrf").fetch("get").fetch("security") == []
abort("refresh must be public") unless paths.fetch("/auth/refresh").fetch("post").fetch("security") == []
abort("logout should inherit bearer security") if paths.fetch("/auth/logout").fetch("post").key?("security")
abort("logout-all should inherit bearer security") if paths.fetch("/auth/logout-all").fetch("post").key?("security")

schemas = doc.fetch("components").fetch("schemas")
%w[
  GoogleOAuthLoginRequest
  CsrfTokenResponse
  RefreshTokenRequest
  LogoutRequest
  AuthTokenResponse
  AuthSessionResponse
].each { |schema| abort("missing auth schema #{schema}") unless schemas.key?(schema) }

login_required = schemas.fetch("GoogleOAuthLoginRequest").fetch("required")
abort("authorizationCode required") unless login_required.include?("authorizationCode")
abort("redirectUri required") unless login_required.include?("redirectUri")

token_required = schemas.fetch("AuthTokenResponse").fetch("required")
%w[accessToken refreshToken tokenType expiresIn user].each do |field|
  abort("AuthTokenResponse missing required #{field}") unless token_required.include?(field)
end

refresh_schema = paths.fetch("/auth/refresh").fetch("post").fetch("responses").fetch("200")
  .fetch("content").fetch("application/json").fetch("schema").fetch(%q[$ref])
abort("refresh must return AuthSessionResponse") unless refresh_schema.end_with?("/AuthSessionResponse")
session_required = schemas.fetch("AuthSessionResponse").fetch("required")
%w[tokenType expiresIn user].each do |field|
  abort("AuthSessionResponse missing required #{field}") unless session_required.include?(field)
end
'
