# EXEC_PLAN: [fix] OAuth 쿠키 로그인 보안 리뷰 반영

- Task slug: `spt-70-oauth-cookie-security-review`
- Base branch: `develop`
- Feature branch: `codex/spt-70-oauth-cookie-security-review`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-70-oauth-cookie-security-review`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-70-oauth-cookie-security-review`
- Jira issue: `SPT-70`
- Jira URL: https://studypot.atlassian.net/browse/SPT-70
- Jira summary: [fix] OAuth 쿠키 로그인 보안 리뷰 반영
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
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/operations/local-development.md
- [x] docs/architecture/backend-map.md
- [x] docs/specs/adr/ADR-20260508-oauth2-cookie-login.md
- [x] docs/specs/change-requests/CR-20260508-oauth2-cookie-login.md

## Related Feature IDs
- [ ] n/a-harness
- [x] identity-core

## Doc Notes
- Review item 1 is valid: `GoogleOAuthRedirectController` currently derives `redirect_uri` from the incoming request URI/Host. The callback URI should be fixed by configuration to avoid Host header spoofing and reverse proxy misconfiguration surprises.
- Review item 2 is valid at least for `AuthController`: controller currently imports `identity.infrastructure.security.AuthTokenCookieIssuer`. Introduce a service-facing cookie port so controllers depend on a contract, not the infrastructure implementation.
- Review item 3 is valid: `refreshTokenFrom(RefreshTokenRequest, ...)` and `refreshTokenFrom(LogoutRequest, ...)` duplicate the same fallback logic.
- Review item 4 is valid: `completeGoogleLogin` catches broad `IllegalArgumentException`; malformed OAuth cookie data can be handled before service invocation, but unexpected service/provider bugs should not be hidden as normal login failure redirects.
- Existing CR/ADR for backend OAuth cookie login already covers this behavior family; this task hardens implementation details without changing endpoint shape or DB schema.
- User decision update: replace hand-rolled Google redirect/callback code with Spring Security `oauth2-client` and `AuthenticationSuccessHandler`/`AuthenticationFailureHandler`. Prefer framework/library behavior for authorization request creation, state handling, PKCE, callback processing, token exchange, and user-info loading.
- User decision update: use Lombok and other focused library support where it removes boilerplate without changing the locked API behavior.
- CI finding: CodeQL flagged globally disabled Spring CSRF after OAuth2 Login introduced session-backed browser flow. Re-enable Spring CSRF token support and add a browser CSRF guard for non-bearer unsafe requests while keeping bearer API requests compatible.

## Goal
Harden the backend-owned Google OAuth cookie login implementation by moving browser OAuth login onto Spring Security `oauth2-client` handlers, using a configured callback URI, tightening controller dependencies and exception handling, and reducing duplicate refresh-token fallback code.

## Approach
1. Add RED tests for Spring Security OAuth2 authorization redirect behavior, fixed callback URI use under hostile Host headers, success-handler HttpOnly cookie issuance, and failure-handler frontend redirect behavior.
2. Add Lombok dependency and use it for focused boilerplate reduction in new OAuth handler/configuration classes.
3. Configure `spring-boot-starter-oauth2-client` for Google with configured backend callback URI and PKCE via Spring Security authorization request customizer.
4. Replace the manual Google redirect/callback controller with `oauth2Login` success/failure handlers.
5. Reuse the existing application session service and cookie issuer through service-facing ports so the handler issues app JWT/refresh cookies after Spring Security has authenticated the Google principal.
6. Keep the locked JSON OAuth API compatible for existing clients.
7. Update local/example configuration and docs for the library-backed OAuth login flow.
8. Run focused auth tests, full Gradle verification, harness tests, and PR workflow.
9. Address CI CodeQL feedback by requiring `XSRF-TOKEN`/`X-XSRF-TOKEN` for cookie-backed unsafe browser requests and allowing the CSRF headers through CORS.

## Step Plan
- [x] RED: add Spring Security OAuth2 Login redirect and handler tests.
- [x] Add Lombok dependency.
- [x] Configure Google `ClientRegistrationRepository` and PKCE authorization resolver.
- [x] Replace manual redirect/callback controller with OAuth2 success/failure handlers.
- [x] Introduce cookie port and remove direct cookie implementation dependency from `AuthController`.
- [x] Deduplicate refresh-token fallback logic.
- [x] Update local config/docs.
- [x] Run focused auth tests.
- [x] Address CodeQL CSRF alert for cookie-backed browser requests.
- [x] Run `./gradlew check build --no-daemon`.
- [x] Run `bash scripts/tests/run.sh`.
- [ ] Create PR and complete review gate/Copilot feedback flow.

## Done Criteria
- Google browser OAuth authorization/callback flow is backed by Spring Security `oauth2-client`, not a hand-built redirect/callback controller.
- Google authorization redirects always use configured backend callback URI, not request Host header.
- Google authorization requests use PKCE via Spring Security configuration.
- Missing backend callback URI fails closed before redirecting to Google.
- Controllers no longer depend directly on the cookie issuer implementation.
- Refresh/logout refresh-token fallback logic is shared.
- OAuth2 login success issues app access/refresh tokens only as HttpOnly cookies and redirects to the configured frontend success URI without token query parameters.
- OAuth2 login failure redirects to the configured frontend failure URI and does not issue app token cookies.
- Focused tests, `./gradlew check build --no-daemon`, `bash scripts/tests/run.sh`, and `git diff --check` pass.
