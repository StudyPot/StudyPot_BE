# EXEC_PLAN: [dev-env] Swagger 쿠키 인증 자동 사용

- Task slug: `spt-75-swagger-cookie-auth`
- Base branch: `develop`
- Feature branch: `codex/spt-75-swagger-cookie-auth`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-75-swagger-cookie-auth`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-75-swagger-cookie-auth`
- Jira issue: `SPT-75`
- Jira URL: https://studypot.atlassian.net/browse/SPT-75
- Jira summary: [dev-env] Swagger 쿠키 인증 자동 사용
- Status: `active`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/local-development.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/openapi.yaml

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- Jira SPT-75 scope is a local developer Swagger/auth UX improvement, not a product API expansion.
- Locked `docs/specs/openapi.yaml` already declares both `bearerAuth` and `cookieAccessToken`; this task aligns runtime SpringDoc output and Swagger UI behavior with that locked contract.
- `docs/specs/auth-permissions-v1.md` already allows bearer token authentication or `studypot_access_token` HttpOnly cookie authentication for protected `/api/v1` endpoints.
- `docs/operations/local-development.md` says browser JavaScript must include credentials instead of reading HttpOnly token values directly, and cookie-backed unsafe requests must echo `XSRF-TOKEN` via `X-XSRF-TOKEN`.
- User decision: implement Swagger so Google login-issued cookies are automatically used by Swagger requests; do not make Swagger JavaScript read the HttpOnly token value.

## Goal
Google OAuth 로그인으로 `studypot_access_token` HttpOnly 쿠키가 발급된 같은 브라우저에서 Swagger UI `Try it out` 요청이 매번 bearer token Authorize 입력 없이 쿠키 인증으로 동작하도록 런타임 OpenAPI/SpringDoc 설정을 보강한다.

## Approach
1. Add regression coverage for runtime `/v3/api-docs` so protected APIs advertise both bearer and access-token cookie security schemes while public auth endpoints stay anonymous.
2. Add regression coverage for Swagger UI config so requests include browser credentials and the existing `XSRF-TOKEN`/`X-XSRF-TOKEN` CSRF contract is exposed to Swagger UI.
3. Update `OpenApiConfiguration` to register `cookieAccessToken` alongside `bearerAuth` without changing the locked external YAML contract.
4. Update SpringDoc YAML defaults and local example settings for `with-credentials` and CSRF header/cookie names.
5. Run focused security/OpenAPI tests first, then `./gradlew check build --no-daemon`.

## Step Plan
- [x] RED: update/add `SecurityConfigurationTest` expectations for `cookieAccessToken` and Swagger UI credentials/CSRF config, then run the focused test and confirm it fails for the missing runtime configuration.
- [x] GREEN: add runtime cookie security scheme and Swagger UI settings.
- [x] REFACTOR: keep names/constants small and aligned with existing `studypot_access_token`, `XSRF-TOKEN`, and `X-XSRF-TOKEN` contracts.
- [x] VERIFY: run focused tests for global security/OpenAPI configuration.
- [x] VERIFY: run `./gradlew check build --no-daemon`.
- [ ] PR: create the PR with `scripts/task/create-pr.sh`, run CodeRabbit review, and finish through the review gate if external services are available.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.global.security.SecurityConfigurationTest.localDiagnosticsAndOpenApiDocsArePublic' --no-daemon` -> FAIL as expected because runtime OpenAPI did not expose `cookieAccessToken`.
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.global.security.SecurityConfigurationTest.localDiagnosticsAndOpenApiDocsArePublic' --no-daemon` -> PASS after adding `cookieAccessToken` and Swagger UI credentials/CSRF settings.
- Focused: `./gradlew test --tests 'com.studypot.aistudyleader.global.security.*' --no-daemon` -> PASS.
- Final: `./gradlew check build --no-daemon` -> PASS.

## Done Criteria
- `/v3/api-docs` contains `bearerAuth` and `cookieAccessToken` security schemes.
- Runtime OpenAPI global security allows either bearer token or `studypot_access_token` cookie authentication.
- Public auth endpoints remain `security: []`.
- Swagger UI config enables credentialed requests and maps CSRF cookie/header names to the existing browser CSRF contract.
- Focused tests and `./gradlew check build --no-daemon` pass.
- PR body includes SPT-75, EXEC_PLAN, verification evidence, and review gate checklist.
