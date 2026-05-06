# EXEC_PLAN: [api] Auth 로그인/토큰 API 명세 추가

- Task slug: `auth-api-spec`
- Base branch: `develop`
- Feature branch: `codex/auth-api-spec`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/auth-api-spec`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/auth-api-spec`
- Jira issue: `SPT-58`
- Jira URL: https://studypot.atlassian.net/browse/SPT-58
- Jira summary: [api] Auth 로그인/토큰 API 명세 추가
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-request-template.md
- [x] docs/specs/adr-template.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/02-requirements.md
- [x] docs/confluence/07-permissions-state.md
- [x] docs/confluence/10-jira-mapping.md
- [x] scripts/tests/run.sh
- [x] scripts/tests/test_quality_gate_contracts.sh
- [x] scripts/tests/test_pr_scripts_static.sh
- [x] scripts/tests/test_auth_api_contracts.sh

## Related Feature IDs
- [x] identity-core

## Doc Notes
- `identity-core` already requires login/session lifecycle through `users`, `oauth_account`, and `refresh_token`, but the locked OpenAPI only exposes `GET /users/me`.
- New auth endpoints change API shape and authorization behavior, so Change Request + ADR is required.
- REST API baseline favors a JSON authorization-code exchange endpoint over backend-owned redirect/callback endpoints for MVP.
- Refresh tokens are stored hashed in `refresh_token`; raw refresh tokens must not be logged or stored outside the client-visible issuance/rotation response.
- Existing QA covers refresh token revocation but not explicit OAuth login, refresh rotation/reuse, or logout-all behavior.

## Goal
Add the missing MVP authentication entrypoints to the locked v1 API contract: Google OAuth login/code exchange, access token refresh, current-session logout, and all-session logout.

## Approach
Record an approved CR/ADR for the locked API change, update human and machine API contracts, extend auth/permission and QA docs, refresh the Confluence API draft and Notion mirror, then verify OpenAPI parsing plus the standard Gradle build.

## Step Plan
- [x] Add CR-20260506-auth-api-entrypoints and linked ADR.
- [x] Update `change-control-v1.md` current approved change list.
- [x] Add auth endpoints, request/response schemas, and public endpoint security overrides to `openapi.yaml`.
- [x] Update `api-contract-v1.md`, `auth-permissions-v1.md`, `qa-acceptance-v1.md`, and `feature-coverage-matrix.md`.
- [x] Refresh `docs/confluence/05-api-spec.md` and Notion API page.
- [x] Run OpenAPI parse, harness tests, and `./gradlew check build --no-daemon`.
- [ ] Commit, create PR, pass review gates, and finish PR.

## Done Criteria
- Change Request and ADR are present and approved.
- `docs/specs/openapi.yaml` defines `POST /auth/oauth/google`, `POST /auth/refresh`, `POST /auth/logout`, and `POST /auth/logout-all`.
- API docs mark auth login/refresh endpoints as explicitly anonymous/public and logout endpoints as authenticated.
- QA acceptance covers Google OAuth login, refresh token rotation/reuse protection, current-session logout, and logout-all.
- Notion API 명세서 v1 mirrors the new auth contract.
- `bash scripts/tests/run.sh` and `./gradlew check build --no-daemon` pass.

## Verification
- [x] `ruby -ryaml -e 'doc=YAML.load_file("docs/specs/openapi.yaml"); puts "openapi=#{doc.fetch("openapi")}, paths=#{doc.fetch("paths").length}, schemas=#{doc.fetch("components").fetch("schemas").length}"'`: PASS (`openapi=3.1.0`, `paths=25`, `schemas=31`)
- [x] `bash scripts/tests/test_auth_api_contracts.sh`: PASS
- [x] `bash scripts/tests/test_quality_gate_contracts.sh`: PASS
- [x] `bash scripts/tests/test_pr_scripts_static.sh`: PASS
- [x] `git diff --check`: PASS
- [x] `bash scripts/tests/run.sh`: PASS
- [x] `./gradlew check build --no-daemon`: PASS
- [x] Notion API page `AI Study Leader API 명세서 v1`: updated with auth endpoints and fetched successfully.
