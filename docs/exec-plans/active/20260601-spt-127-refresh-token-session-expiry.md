# EXEC_PLAN: [identity] refresh token 만료 시 세션 끊김 처리 개선

- Task slug: `spt-127-refresh-token-session-expiry`
- Base branch: `develop`
- Feature branch: `codex/spt-127-refresh-token-session-expiry`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-127-refresh-token-session-expiry`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-127-refresh-token-session-expiry`
- Jira issue: `SPT-127`
- Jira URL: https://studypot.atlassian.net/browse/SPT-127
- Jira summary: [identity] refresh token 만료 시 세션 끊김 처리 개선
- Status: `ready-for-pr`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] identity-core

## Doc Notes
- `POST /api/v1/auth/refresh` is a public auth endpoint that rotates a refresh token and issues new application token cookies.
- `api-contract-v1.md` says the refresh token may come from the `studypot_refresh_token` HttpOnly cookie or an optional JSON `refreshToken` body, with the cookie taking precedence.
- `auth-permissions-v1.md` requires refresh tokens to be hashed, rotated on refresh, rejected after rotation/revocation, and not logged raw.
- `qa-acceptance-v1.md` covers refresh token rotation, old refresh-token reuse rejection, logout revocation, and no raw token leakage.
- Current controller implementation only reads the cookie path and returns 401 on refresh rejection without clearing stale token cookies.

## Goal
Make refresh-token expiry/revocation handling stable for frontend session management: keep valid refresh rotation working, support the documented JSON-body fallback when no cookie is present, return a distinguishable 401 problem detail for rejected refresh tokens, and clear token cookies on refresh failure to prevent repeated refresh loops.

## Approach
Use the existing MySQL-backed refresh token session as the source of truth. Keep cookie precedence over JSON body. Add refresh-token-specific rejection reasons without exposing raw token values. Clear both application token cookies when refresh is rejected, then rethrow so the global problem handler returns the locked 401 Problem Detail shape with a stable refresh error code. Update OpenAPI/API docs only for the concrete refresh response/error contract drift touched here.

## Step Plan
1. Add RED controller tests for JSON-body refresh fallback and refresh rejection cookie clearing/problem code.
2. Add RED service test for expired refresh-token rejection reason and no replacement session creation.
3. Implement minimal refresh request-body support in `AuthController`.
4. Add refresh rejection reason/code support in `RefreshTokenRejectedException` and `AuthSessionService`.
5. Add refresh-token-specific problem detail handling and clear token cookies on refresh failure.
6. Update OpenAPI/API docs to match cookie-backed refresh response and rejected-refresh error contract.
7. Run focused auth tests, then `./gradlew check build --no-daemon`.
8. Create PR through `scripts/task/create-pr.sh`, run CodeRabbit review, address once if needed, verify review gate, and finish with `scripts/task/finish-pr.sh`.

## Done Criteria
- Refresh with a valid `studypot_refresh_token` cookie still rotates access/refresh cookies and returns current user session summary.
- Refresh with a valid JSON `refreshToken` body works when the cookie is absent.
- If both cookie and JSON body are present, the cookie value is used.
- Missing, invalid, expired, reused, or revoked refresh tokens return 401 Problem Detail with a stable refresh-token error code.
- Refresh-token rejection clears both application token cookies when cookie support is configured.
- Refresh-token rejection does not create a replacement refresh token.
- API/OpenAPI/Auth/QA docs remain consistent with the implemented contract.
- Focused tests and `./gradlew check build --no-daemon` pass.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest' --tests 'com.studypot.aistudyleader.auth.service.AuthSessionServiceTest' --no-daemon` failed before implementation with missing JSON body refresh support, missing rejected-refresh cookie clearing/problem code, and generic expired-refresh rejection.
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest.refreshIgnoresStaleAccessTokenCookieWhenRefreshTokenIsValid' --no-daemon` failed before resolver change because stale access-token cookies blocked the public refresh endpoint.
- Focused: `scripts/tests/test_auth_api_contracts.sh` passed.
- Focused: `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest' --tests 'com.studypot.aistudyleader.auth.service.AuthSessionServiceTest' --tests 'com.studypot.aistudyleader.global.security.SecurityConfigurationTest' --no-daemon` passed.
- Full: `./gradlew check build --no-daemon` passed.
- Reviewdog follow-up: `shellcheck -x scripts/tests/test_auth_api_contracts.sh`, `scripts/tests/test_auth_api_contracts.sh`, and `./gradlew check build --no-daemon` passed after adding the explicit `SC2016` shellcheck directive for the embedded Ruby contract check.
