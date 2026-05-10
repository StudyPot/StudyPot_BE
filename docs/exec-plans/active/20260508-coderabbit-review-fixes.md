# EXEC_PLAN: [identity] 인증/인가 테스트 추가

- Task slug: `coderabbit-review-fixes`
- Base branch: `develop`
- Feature branch: `codex/coderabbit-review-fixes`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/coderabbit-review-fixes`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/coderabbit-review-fixes`
- Jira issue: `SPT-27`
- Jira URL: https://studypot.atlassian.net/browse/SPT-27
- Jira summary: [identity] 인증/인가 테스트 추가
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/architecture/backend-map.md
- [x] docs/operations/local-development.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/adr/ADR-20260508-oauth2-cookie-login.md
- [x] docs/specs/change-requests/CR-20260508-oauth2-cookie-login.md

## Related Feature IDs
- [x] identity-core

## Doc Notes
- User requested fixing the CodeRabbit review findings from the repository-wide review.
- Base checkout was fast-forwarded from `80d0e31` to `cf6c54f` before creating this worktree, so the initial 64 findings were refreshed against the latest `develop`.
- Latest CodeRabbit run in this worktree: `/tmp/studypot-coderabbit-review-latest.ndjson`; result count is 55 issues (`critical 10`, `major 33`, `minor 12`).
- The approved OAuth cookie login CR/ADR allows clarifying auth cookie behavior in locked v1 docs without introducing a new endpoint or DB shape.
- Security-sensitive fixes should avoid adding new product behavior beyond the review findings: fail-fast config validation, no PII in token/log/toString output, cookie-port abstraction, and stricter test coverage.
- False-positive note: CodeRabbit suggested `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc`, but this project is on Spring Boot 4.0.6 with `spring-boot-starter-webmvc-test`; the available annotation package is `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- Verification: focused auth/security/domain tests passed after RED/GREEN implementation.
- Verification: `./gradlew check build --no-daemon` passed on 2026-05-08.
- Follow-up CodeRabbit run in this worktree: `/tmp/studypot-coderabbit-review-final.ndjson`; remaining actionable items were `AuthenticatedUser` component validation, IP metadata validation, repository soft-delete upsert handling, and explicit CR/ADR references near the locked API contract and public auth endpoints.
- False-positive note: CodeRabbit suggested Boot 3 `org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration`, but Spring Boot 4.0.6 provides the test exclusion class at `org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration`; changing to the Boot 3 package fails compilation.
- False-positive note: CodeRabbit repeated Google OAuth HTTPS, CORS wildcard/credential, JWT secret placeholder, and CSRF negative-verification findings after the code already contained runtime validation or assertions for those cases.
- Scope decision: `GoogleOAuthProfile.tokenExpiresAt` remains nullable because backend-owned Spring Security OAuth2 login does not expose a reliable token expiry in the success handler. The profile now preserves explicit empty scopes, but unknown token expiry is intentionally represented as `null` and persisted as absent token metadata.
- Final successful CodeRabbit rerun before rate limiting: `/tmp/studypot-coderabbit-review-final-pass-retry.ndjson`; actionable items from that run were addressed: OAuth provider HTTP errors are translated safely, OAuthAccount has a guarded soft-delete method, local verification script uses `TEST_ROOT`, service-unavailable casing is consistent, test YAML nesting is corrected, and API/OpenAPI docs no longer describe sending the PKCE verifier to the browser.
- False-positive note: CodeRabbit continued to suggest Spring Boot 3 `AutoConfigureMockMvc` packages and repeated CORS/JWT/CSRF findings that current code and passing Boot 4 compilation/tests already disprove.
- Final CodeRabbit attempt after those fixes: `/tmp/studypot-coderabbit-review-after-final-fixes.ndjson`; blocked by CodeRabbit `rate_limit` with `waitTime` 11 minutes 19 seconds, so no newer findings were available.
- Verification: `scripts/tests/test_local_dev_verification_contracts.sh` passed on 2026-05-08 after the `TEST_ROOT` fix.
- Verification: `./gradlew check build --no-daemon` passed on 2026-05-08T12:42:46+0900 after final follow-up fixes.
- Verification: `git diff --check` passed after final follow-up fixes.
- PR created: https://github.com/StudyPot/StudyPot_BE/pull/56.
- Post-PR CodeRabbit run: `/tmp/studypot-coderabbit-review-post-pr-final.ndjson`; additional valid items addressed in a follow-up commit: OAuth2 config null handling, Google client secret empty defaults, secure CORS default, localhost-only HTTP redirect URIs, CSRF `_csrf` parameter fallback test, local example HTTP cookie settings, canonical auth paths in docs, server-side PKCE key wording, provider enum persistence parsing, and removal of manual `X-Forwarded-For` trust.
- False-positive note: CodeRabbit suggested adding `@Override` to `AuthTokenCookieIssuer.accessToken`, but `AuthTokenCookiePort` intentionally exposes only refresh-token cookie reading; adding the annotation fails compilation and was reverted.
- False-positive note: CodeRabbit continued to suggest Spring Boot 3 `AutoConfigureMockMvc` imports. Boot 4.0.6 provides `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, and the current imports compile and pass tests.
- False-positive / deferred architecture note: HS256 remains in place because this service currently signs and verifies its own tokens through a single configured secret. Moving to RS256/JWKS is an architecture decision outside the CodeRabbit fix scope and should go through a separate task if multi-service token verification is required.
- PR status note: local and PR Quality checks passed, but `review-gate-pass` remains blocked until all latest-head role gate markers exist. Copilot review was requested but `scripts/task/verify-copilot-review.sh 56 0e80d9c7ce6d0117046ad11615a16d4a6582f721` timed out after 600 seconds without latest-head Copilot review activity.

## Goal
Resolve the latest CodeRabbit findings for the `identity-core` authentication/authorization surface, including compile errors, security hardening, documentation consistency, and focused regression tests.

## Approach
Work in small groups: first capture existing RED compile/test failures, then fix mechanical compile/config/doc issues, then add or update focused tests for behavior changes before implementation. Keep changes scoped to the reviewed files, preserve the approved OAuth cookie login API shape, and rerun CodeRabbit plus full Gradle verification before PR creation.

## Step Plan
- [x] Run focused compile/tests to confirm current failures from wrong imports and stale assertions.
- [x] Fix compile blockers: wrong `AutoConfigureMockMvc` imports, test resource JWT refresh TTL nesting, missing type resolution false positives, and current exec-plan metadata.
- [x] Update docs and historical exec-plan consistency findings without changing locked product scope.
- [x] Add RED tests for identity value object validation, refresh token active-only lookup, cookie-port refresh-token extraction, JWT non-PII claims, CORS credential wildcard validation, OAuth optional bean wiring, and configuration fail-fast behavior.
- [x] Implement minimal production changes to satisfy the RED tests and CodeRabbit findings.
- [x] Update OpenAPI/auth docs for approved cookie behavior: cookie names, optional refresh/logout body schemas, TTL/security attributes, and CR/ADR references.
- [x] Run focused test classes after each behavior cluster.
- [x] Address follow-up actionable CodeRabbit items for `AuthenticatedUser`, auth-session IP metadata, repository soft-delete upsert conflicts, and locked-doc CR/ADR traceability.
- [x] Run `./gradlew check build --no-daemon`, rerun CodeRabbit, and address any still-valid residual findings available before the final CodeRabbit rate limit.
- [ ] Use `scripts/task/create-pr.sh` after verification passes and then follow Copilot/review-gate flow.

## Done Criteria
- [x] Latest still-valid CodeRabbit findings from `/tmp/studypot-coderabbit-review-latest.ndjson` and follow-up successful reruns are addressed or explicitly recorded as false positives with reason.
- [x] Authentication/security tests cover the changed validation and cookie-token behavior.
- [x] OpenAPI and locked docs remain consistent with the approved OAuth cookie login CR/ADR, with PKCE verifier storage documented as server-side.
- [x] `./gradlew check build --no-daemon` passes in this worktree.
- [ ] A follow-up CodeRabbit run raises 0 still-valid issues for the changed scope. Final attempt is currently blocked by CodeRabbit `rate_limit`; retry after the reported wait window.
