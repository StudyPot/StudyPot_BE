# EXEC_PLAN: [feat] 백엔드 Google OAuth2 로그인 쿠키 발급

- Task slug: `spt-69-oauth2-cookie-login`
- Base branch: `develop`
- Feature branch: `codex/spt-69-oauth2-cookie-login`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-69-oauth2-cookie-login`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-69-oauth2-cookie-login`
- Jira issue: `SPT-69`
- Jira URL: https://studypot.atlassian.net/browse/SPT-69
- Jira summary: [feat] 백엔드 Google OAuth2 로그인 리다이렉트와 보안 쿠키 발급
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/operations/local-development.md
- [x] docs/architecture/backend-map.md

## Related Feature IDs
- [ ] n/a-harness
- [x] identity-core

## Doc Notes
- User decision: backend should own the Google OAuth redirect entrypoint at `/api/oauth2/authorization/google`, then redirect to the frontend after success.
- User decision: login tokens must be delivered through secure cookies so frontend JavaScript cannot directly manage token values.
- Existing locked v1 contract says the client sends Google authorization code to `POST /api/v1/auth/oauth/google`; this task intentionally changes/extends auth API behavior, so Change Request and ADR documents are required.
- Preserve existing `POST /api/v1/auth/oauth/google` code-exchange endpoint for compatibility while adding the backend redirect/callback flow.
- Use current identity layering: controller for redirect/callback and REST auth endpoints, service for token/session issuing, repository for persistence, infrastructure for Google/JWT/cookie/CORS technical details.
- Spring Security docs confirm OAuth2 authorization endpoints can be customized, but this implementation reuses the existing manual `GoogleOAuthClient` to keep refresh-token hash rotation, user upsert, and JWT issuing behavior centralized.

## Goal
Add a backend-owned Google OAuth2 redirect login flow with CORS and HttpOnly cookie token delivery, while preserving the existing JSON code-exchange endpoint and current session semantics.

## Approach
1. Document the locked-contract change with a Change Request and ADR.
2. Add RED controller/security tests for:
   - `GET /api/oauth2/authorization/google` redirects to Google and sets temporary OAuth state cookies.
   - `GET /api/login/oauth2/code/google` validates state, issues access/refresh cookies, clears temporary cookies, and redirects to configured frontend success URL without token query parameters.
   - `/api/v1/users/me` accepts the access-token cookie as authentication.
   - refresh/logout can read refresh-token cookies and rotate/clear cookies.
   - configured frontend origins receive credentialed CORS headers.
3. Implement OAuth redirect settings and cookie settings as typed properties with safe defaults.
4. Add a small cookie utility/issuer in `identity.infrastructure.security`.
5. Add a backend OAuth redirect controller in `identity.controller` that reuses `AuthSessionService`.
6. Extend security config with CORS, public OAuth redirect/callback endpoints, and cookie-based bearer token resolution.
7. Update local docs/example config/OpenAPI/auth contract docs.
8. Run focused tests, full Gradle verification, and harness verification.

## Step Plan
- [x] RED: write failing tests for backend OAuth2 redirect/callback cookie flow.
- [x] Implement Google authorization redirect and callback controller.
- [x] Implement auth cookie properties/issuer and cookie bearer token resolver.
- [x] Wire CORS and security permit rules.
- [x] Extend refresh/logout endpoints to support HttpOnly refresh-token cookie while keeping body compatibility.
- [x] Add Change Request, ADR, API/auth/local docs and OpenAPI updates.
- [x] Run focused auth/security tests.
- [x] Run `./gradlew check build --no-daemon`.
- [ ] Create PR through the harness and address Copilot/CI feedback.

## Done Criteria
- `GET /api/oauth2/authorization/google` starts Google OAuth login without requiring bearer auth.
- OAuth callback validates state and redirects to the configured frontend success URI.
- Access and refresh tokens are set as HttpOnly cookies; successful redirect URLs do not contain token values.
- `/api/v1` authentication accepts the access token from the configured cookie.
- Refresh/logout flows can use cookie tokens and update/clear cookies.
- CORS allows configured frontend origins with credentials.
- Existing JSON `POST /api/v1/auth/oauth/google` remains compatible.
- Locked contract change is recorded with Change Request + ADR and affected docs are updated.
- Focused tests and `./gradlew check build --no-daemon` pass.
