# EXEC_PLAN: [fix] Cross-site CSRF 토큰 복구 경로 추가

- Task slug: `spt-116-cross-site-csrf-bootstrap`
- Base branch: `develop`
- Feature branch: `codex/spt-116-cross-site-csrf-bootstrap`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-116-cross-site-csrf-bootstrap`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-116-cross-site-csrf-bootstrap`
- Jira issue: `SPT-116`
- Jira URL: https://studypot.atlassian.net/browse/SPT-116
- Jira summary: [fix] Cross-site CSRF 토큰 복구 경로 추가
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/change-requests/CR-20260508-oauth2-cookie-login.md
- [x] docs/specs/adr/ADR-20260508-oauth2-cookie-login.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/operations/deployment.md
- [x] docs/operations/local-development.md
- [x] src/main/java/com/studypot/aistudyleader/global/security/SecurityConfiguration.java
- [x] src/main/java/com/studypot/aistudyleader/global/security/BrowserCsrfProtectionFilter.java
- [x] src/main/java/com/studypot/aistudyleader/auth/controller/AuthController.java
- [x] src/test/java/com/studypot/aistudyleader/auth/controller/AuthControllerTest.java
- [x] src/test/java/com/studypot/aistudyleader/auth/controller/GoogleOAuth2LoginFlowTest.java

## Related Feature IDs
- [ ] n/a-harness
- [x] identity-core

## Doc Notes
- `AGENTS.md`: work must start from a Jira SPT Task, generated worktree, EXEC_PLAN, tests, PR review gate, CodeRabbit review, and finish-pr cleanup.
- `ARCHITECTURE.md`: auth/cookie behavior belongs to the Spring Security and auth boundary; shared security changes need tests and docs.
- `docs/index.md`: v1 auth/API contracts are locked; new endpoint behavior must be covered by Change Request and ADR.
- `docs/specs/auth-permissions-v1.md`: cookie auth stays HttpOnly; production cookies must be Secure and compatible with the deployed frontend/API domains.
- `docs/specs/api-contract-v1.md`: `POST /api/v1/auth/refresh` is public but rotates refresh-token cookies; browser clients may rely on HttpOnly refresh cookies.
- `CR-20260508-oauth2-cookie-login` and `ADR-20260508-oauth2-cookie-login`: backend-owned OAuth login deliberately avoids exposing access/refresh token values to the frontend.
- `docs/specs/change-control-v1.md`: new endpoint and response shape require a Change Request and ADR; this task adds CR/ADR `20260527-cross-site-csrf-bootstrap`.
- `docs/specs/feature-coverage-matrix.md`: `identity-core` remains the feature ID; no new feature ID is introduced.
- `docs/operations/deployment.md`: Netlify frontend and rumiclean API are different sites; token cookies already require `SameSite=None; Secure` and frontend requests use credentials.
- Runtime evidence on 2026-05-27: Netlify Origin CORS preflight succeeds, OAuth entrypoint redirects to Google, container env has `STUDYPOT_AUTH_COOKIE_SAME_SITE=None` and `STUDYPOT_AUTH_COOKIE_SECURE=true`, but `POST /api/v1/auth/refresh` without an XSRF header returns 403.
- Frontend bundle evidence: requests use `credentials: "include"` and read `XSRF-TOKEN` from `document.cookie`; Netlify JavaScript cannot read a cookie scoped to `studypot.rumiclean.com`.
- Existing Spring/Security behavior sets `XSRF-TOKEN` without the auth cookie SameSite/Secure/domain policy, which is insufficient for cross-site XHR between different eTLD+1 domains.

## Goal
Allow the deployed Netlify frontend to bootstrap a CSRF token for cross-site credentialed requests without exposing access or refresh token values, so OAuth success can restore the session through cookie-backed `POST /api/v1/auth/refresh` instead of being stopped by CSRF 403.

## Approach
Add a safe CSRF bootstrap endpoint under the auth boundary. The endpoint will issue a frontend-readable token value in the response body and header, and set the matching `XSRF-TOKEN` cookie using the same cross-site-compatible cookie policy as the auth cookies except without `HttpOnly`. The existing CSRF filter will continue to require cookie/header equality for unsafe cookie-backed requests.

## Step Plan
1. Add a RED integration test that proves cross-site refresh without a readable/bootstrap XSRF token is rejected with 403.
2. Add a RED test for `GET /api/v1/auth/csrf`: Netlify Origin should receive CORS credentials, a response token/header, and an `XSRF-TOKEN` cookie with `SameSite=None; Secure`.
3. Implement a small auth controller endpoint and cookie issuer support for the readable CSRF cookie.
4. Confirm `POST /api/v1/auth/refresh` with the bootstrap token and cookie reaches auth validation instead of CSRF rejection.
5. Update API/deployment/local docs and OpenAPI/security tests for the new bootstrap contract.
6. Run focused tests, repository harness tests, and `./gradlew check build --no-daemon`.
7. Commit, create PR, run CodeRabbit review, satisfy review gate, merge with `finish-pr.sh`, and verify deployed CORS/CSRF behavior.

## Done Criteria
- `GET /api/v1/auth/csrf` is public, safe, documented, and returns the CSRF token without access/refresh token values.
- The endpoint sets `XSRF-TOKEN` with `Path=/`, auth cookie domain when configured, `SameSite=None`, and `Secure=true` in production cross-site config.
- Existing cookie-backed unsafe request CSRF protection remains active.
- Netlify Origin can use the bootstrap token to pass CSRF on `POST /api/v1/auth/refresh`.
- Focused tests, harness tests, and `./gradlew check build --no-daemon` pass.
- PR review gate and CodeRabbit marker pass, PR merges to `develop`, and local cleanup is recorded.

## Implementation Notes
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest.csrfBootstrapReturnsReadableTokenAndCrossSiteCookiePolicy' --no-daemon` failed as expected with `Status expected:<200> but was:<401>` before the endpoint was permitted/implemented.
- Added public `GET /api/v1/auth/csrf` in `AuthController`. It returns `cookieName`, `headerName`, and the raw CSRF token, and also sets the `X-XSRF-TOKEN` response header.
- Used Spring Security `DeferredCsrfToken` instead of the normal `CsrfToken` request attribute because the SPA request handler exposes a masked request token while the cookie stores the raw token.
- Configured `CookieCsrfTokenRepository` after `csrf.spa()` so the SPA request handler stays active while `XSRF-TOKEN` inherits the configured auth cookie path, domain, Secure flag, and SameSite policy.
- Added `GET /api/v1/auth/csrf` to public auth OpenAPI customization and security authorization.
- Added CR/ADR `20260527-cross-site-csrf-bootstrap` and updated locked API, auth-permission, OpenAPI, feature-coverage, deployment, and local-development docs.

## Verification
- `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest.csrfBootstrapReturnsReadableTokenAndCrossSiteCookiePolicy' --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest.crossSiteRefreshCanUseBootstrappedCsrfToken' --no-daemon` -> PASS.
- `./gradlew test --tests 'com.studypot.aistudyleader.global.security.SecurityConfigurationTest.localDiagnosticsAndOpenApiDocsArePublic' --no-daemon` -> PASS.
- `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.GoogleOAuth2LoginFlowTest.corsPreflightAllowsConfiguredFrontendOriginWithCredentials' --no-daemon` -> PASS.
- `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest' --no-daemon` -> PASS after rerunning alone; an earlier parallel Gradle run hit a `build/test-results` file race, not a test assertion failure.
- `bash scripts/tests/test_auth_api_contracts.sh` -> PASS.
- `bash scripts/tests/test_quality_gate_contracts.sh` -> PASS.
- `bash scripts/tests/test_deployment_contracts.sh` -> PASS.
- `bash scripts/tests/run.sh` -> PASS.
- `./gradlew check build --no-daemon` -> PASS (`BUILD SUCCESSFUL in 45s`).
