# EXEC_PLAN: [identity] 현재 사용자와 refresh_token 구현

- Task slug: `spt-25-identity-refresh-token`
- Base branch: `develop`
- Feature branch: `codex/spt-25-identity-refresh-token`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-25-identity-refresh-token`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-25-identity-refresh-token`
- Jira issue: `SPT-25`
- Jira URL: https://studypot.atlassian.net/browse/SPT-25
- Jira summary: [identity] 현재 사용자와 refresh_token 구현
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] docs/architecture/backend-map.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/openapi.yaml
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] Spring Security OAuth 2.0 Resource Server JWT docs: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- [x] Spring Security `NimbusJwtEncoder` API docs: https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/oauth2/jwt/NimbusJwtEncoder.html

## Related Feature IDs
- [x] identity-core

## Doc Notes
- SPT-24 is merged into `develop`; this task builds on the existing Google OAuth login core, `users`, and `oauth_account` persistence.
- Locked API requires `POST /api/v1/auth/oauth/google`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/logout-all`, and `GET /api/v1/users/me`.
- Public endpoints are Google OAuth login and refresh. Logout, logout-all, and current user require bearer access-token authentication.
- OpenAPI declares bearer tokens as JWT. Use Spring Security JWT resource-server/JOSE support rather than custom request filters or hand-built JWT parsing.
- Refresh tokens must be random opaque application tokens. Store only hashes in `refresh_token`; return raw refresh tokens only in issue/rotation responses.
- Token lifetime and signing secret are runtime configuration, not API contract. Use env-backed configuration with conservative defaults for local/test where needed and keep the signing secret out of tracked docs.

## Goal
Implement SPT-25 identity session support: wire Google OAuth login to application access/refresh token issuance, rotate refresh tokens, reject old refresh-token reuse, revoke current/all sessions, authenticate JWT bearer access tokens, and return the current user profile.

## Approach
- Add refresh-token domain/application types and repository port around the locked `refresh_token` table.
- Add an auth session use case that composes `GoogleOAuthLoginService`, access-token issuing, refresh-token generation/hash persistence, user lookup, rotation, logout, and logout-all.
- Add Spring Security JWT configuration using env-backed HMAC secret and Spring Security resource-server support.
- Add inbound web adapters under `identity.adapter.in.web` that implement the locked API shapes and keep DTO mapping out of application/domain code.
- Add JDBC persistence for `refresh_token` without storing raw tokens.
- Keep Google provider token storage out of scope; SPT-24 already avoids provider token persistence and SPT-25 stores only application refresh-token hashes.

## Step Plan
- [x] Run baseline verification in the SPT-25 worktree.
- [x] RED: write application tests for Google OAuth token issue, refresh rotation, old refresh-token rejection, logout current session, logout-all, and current user lookup.
- [x] GREEN: implement refresh-token domain/application service, token hashing/generation ports, and fake repositories until focused application tests pass.
- [x] RED: write web/API tests for the locked endpoint paths, response shape, public/protected auth behavior, and bearer access-token current-user flow.
- [x] GREEN: implement web controller DTOs, Spring Security JWT configuration, and auth exception mapping.
- [x] RED: extend JDBC SQL contract tests for `refresh_token` insert/find/revoke SQL and raw-token non-storage.
- [x] GREEN: implement JDBC refresh-token repository.
- [x] Run focused tests, then `./gradlew check build --no-daemon`.
- [ ] Commit, create PR through `scripts/task/create-pr.sh`, address CI/Copilot comments with the agreed max-review-loop cap, post role gates, and finish to manual merge notification.

## Done Criteria
- [x] Google OAuth login endpoint returns `AuthTokenResponse` with access token, refresh token, token type `Bearer`, positive `expiresIn`, and current user.
- [x] Access token authenticates `GET /api/v1/users/me`; unauthenticated current-user requests return ProblemDetail 401.
- [x] Refresh endpoint rotates refresh tokens and rejects reuse of the old refresh token.
- [x] Logout revokes the submitted current refresh token for the authenticated user.
- [x] Logout-all revokes every active refresh token for the authenticated user.
- [x] DB persistence stores refresh-token hashes only and never stores raw refresh tokens.
- [x] Focused tests and `./gradlew check build --no-daemon` pass.
- [ ] PR review gate, Copilot feedback, role gate evidence, Mattermost manual merge notification, and post-merge cleanup are completed through the harness.

## Verification Log
- [x] Baseline: `./gradlew check build --no-daemon` PASS before SPT-25 implementation.
- [x] RED: `./gradlew test --tests 'com.studypot.aistudyleader.identity.application.AuthSessionServiceTest' --no-daemon` failed because auth session and refresh-token application types did not exist.
- [x] GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.identity.application.AuthSessionServiceTest' --no-daemon` PASS.
- [x] RED: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.in.web.AuthControllerTest' --no-daemon` failed before auth API wiring because `AccessTokenIssuer` had no bean.
- [x] GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.in.web.AuthControllerTest' --no-daemon` PASS.
- [x] RED: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.out.persistence.IdentityJdbcSqlContractTest' --no-daemon` failed because refresh-token JDBC SQL did not exist.
- [x] GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.out.persistence.IdentityJdbcSqlContractTest' --no-daemon` PASS.
- [x] Focused: `./gradlew test --tests 'com.studypot.aistudyleader.identity.application.AuthSessionServiceTest' --tests 'com.studypot.aistudyleader.identity.adapter.in.web.AuthControllerTest' --tests 'com.studypot.aistudyleader.identity.adapter.out.persistence.IdentityJdbcSqlContractTest' --no-daemon` PASS.
- [x] Full verification initially failed because no-DataSource test contexts loaded the new auth controller without an `AuthSessionService`; fixed by lazily resolving the service with `ObjectProvider`.
- [x] Full verification: `./gradlew check build --no-daemon` PASS.
- [x] Local config: created ignored `src/main/resources/application-local.yml` with local JWT secret, provided Google client id, and env-backed Google client secret.
- [x] Local profile check: `./gradlew test --tests 'com.studypot.aistudyleader.AiStudyLeaderApplicationTests' --no-daemon -Dspring.profiles.active=local` PASS.
- [x] Final full verification after local profile check: `./gradlew check build --no-daemon` PASS.
- [x] Pre-commit full verification: `./gradlew check build --no-daemon` PASS.
- [x] Pre-commit whitespace check: `git diff --check` PASS.
