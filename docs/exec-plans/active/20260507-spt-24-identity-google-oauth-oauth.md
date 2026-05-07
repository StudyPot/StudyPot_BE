# EXEC_PLAN: [identity] Google OAuth 로그인과 oauth_account 구현

- Task slug: `spt-24-identity-google-oauth-oauth`
- Base branch: `develop`
- Feature branch: `codex/spt-24-identity-google-oauth-oauth`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-24-identity-google-oauth-oauth`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-24-identity-google-oauth-oauth`
- Jira issue: `SPT-24`
- Jira URL: https://studypot.atlassian.net/browse/SPT-24
- Jira summary: [identity] Google OAuth 로그인과 oauth_account 구현
- Status: `implemented-local`

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
- [x] docs/specs/domain-erd.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/change-requests/CR-20260506-auth-api-entrypoints.md
- [x] docs/specs/adr/ADR-20260506-auth-api-entrypoints.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] Google OAuth 2.0 for Web Server Applications: https://developers.google.com/identity/protocols/oauth2/web-server
- [x] Google OpenID Connect discovery/userinfo docs: https://developers.google.com/identity/openid-connect/openid-connect

## Related Feature IDs
- [x] identity-core

## Doc Notes
- Jira SPT-24 scope is `users` and `oauth_account`: first login creates user/provider link, relogin updates the existing provider link, and active-row uniqueness must respect soft deletes.
- Jira SPT-25 owns `refresh_token`, current-user lookup, token revocation, and refresh-token reuse protection. This task must not invent refresh-token persistence or rotation behavior ahead of SPT-25.
- Locked API/ADR already decide that the frontend obtains the Google authorization code and calls the backend with JSON. Backend-owned OAuth redirect/callback routes stay out of MVP.
- User decision on 2026-05-07: the real Google Cloud OAuth client can be created by the user; backend implementation proceeds with env/config placeholders and test doubles until real `client_id`, `client_secret`, and redirect URI are available.
- User provided a local Google OAuth client id; store the real value only in ignored `src/main/resources/application-local.yml`, and keep `client-secret` in local env/deployment secret.
- Google docs confirm Web application OAuth credentials require a client ID, client secret, and authorized redirect URI, and the redirect URI must exactly match the configured URI.
- Google OpenID Connect docs identify the discovery document and current token/userinfo endpoint metadata; production code should keep endpoint URIs configurable while defaulting to the documented Google endpoints.
- Copilot review on PR #38 found duplicate nickname length policy in `GoogleOAuthLoginService`; fixed by keeping candidate selection in the service and centralizing length/normalization in `IdentityUser`.

## Goal
Implement the identity-core Google OAuth login core for SPT-24: exchange a Google authorization code, map the verified Google profile, create or update the live `users` row, create or update the live `oauth_account` row, and keep raw provider tokens out of stored domain state and responses.

## Approach
- Add framework-free identity domain records/value objects for canonical email, nickname, Google provider account keys, users, and OAuth account sync state.
- Add an application use case that validates the login command, calls a Google OAuth exchange port, rejects unverified Google email profiles, and upserts the user/provider account through an identity repository port.
- Add a Google OAuth outbound adapter with configurable client id/secret/token URI/userinfo URI. It will exchange the authorization code and fetch userinfo, but it will not persist or return raw Google provider tokens.
- Add a JDBC repository adapter for `users` and `oauth_account`, guarded so the existing no-DataSource test profile can still load the application context.
- Keep REST token/session behavior out of this PR except for contracts already read; SPT-25 will wire refresh-token issuance/current-user API on top of this login core.

## Step Plan
- [x] Run baseline verification in the new worktree.
- [x] RED: write domain/application tests for first login, relogin, validation failures, unverified email rejection, and soft-deleted provider account recreation.
- [x] GREEN: implement identity domain objects, use case, ports, and in-memory test doubles until focused tests pass.
- [x] RED: write outbound Google OAuth adapter tests for token exchange form fields, userinfo bearer call, and secret-safe profile mapping.
- [x] GREEN: implement configurable Google OAuth adapter.
- [x] RED: write JDBC SQL contract tests for live-key/deleted-row filters and insert/update column coverage.
- [x] GREEN: implement JDBC identity repository adapter.
- [x] Run focused tests, then `./gradlew check build --no-daemon`.
- [ ] Commit, create PR through `scripts/task/create-pr.sh`, address CI/Copilot comments with the agreed max-review-loop cap, post role gates, and finish to manual merge notification.

## Done Criteria
- [x] SPT-24 Jira scope is implemented without changing locked API/DB specs.
- [x] Google OAuth login core creates a new user and OAuth account on first login.
- [x] Relogin by the same Google provider user updates the existing OAuth account and user login timestamp without duplicating live rows.
- [x] Blank/invalid command input and unverified Google email profiles are rejected by tests.
- [x] Soft-deleted provider account rows do not block recreating a live provider link.
- [x] Google OAuth client config stays secret-safe; raw provider tokens are not logged, returned, or stored by the SPT-24 core.
- [x] Focused tests and `./gradlew check build --no-daemon` pass.
- [ ] PR review gate, Copilot feedback, role gate evidence, Mattermost manual merge notification, and post-merge cleanup are completed through the harness.

## Verification Log
- [x] Baseline: `./gradlew check build --no-daemon` PASS before SPT-24 implementation.
- [x] RED: `./gradlew test --tests 'com.studypot.aistudyleader.identity.application.GoogleOAuthLoginServiceTest' --no-daemon` failed because identity domain/application classes did not exist.
- [x] GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.identity.application.GoogleOAuthLoginServiceTest' --no-daemon` PASS.
- [x] RED: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.out.google.GoogleOAuthClientTest' --no-daemon` failed because Google OAuth adapter classes did not exist.
- [x] GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.out.google.GoogleOAuthClientTest' --no-daemon` PASS.
- [x] RED: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.out.persistence.IdentityJdbcSqlContractTest' --no-daemon` failed because JDBC SQL contract class did not exist.
- [x] GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.identity.adapter.out.persistence.IdentityJdbcSqlContractTest' --no-daemon` PASS.
- [x] Focused: `./gradlew test --tests 'com.studypot.aistudyleader.identity.application.GoogleOAuthLoginServiceTest' --tests 'com.studypot.aistudyleader.identity.adapter.out.google.GoogleOAuthClientTest' --tests 'com.studypot.aistudyleader.identity.adapter.out.persistence.IdentityJdbcSqlContractTest' --no-daemon` PASS.
- [x] Full verification initially failed because `GoogleOAuthConfiguration` required a `RestClient.Builder` bean that the existing test context does not provide; root cause fixed by using `ObjectProvider<RestClient.Builder>` with `RestClient.builder()` fallback.
- [x] Full verification after fix: `./gradlew check build --no-daemon` PASS.
- [x] Copilot review fix: added `IdentityUserTest` to pin domain-owned nickname normalization/length policy.
- [x] Copilot review fix: `./gradlew test --tests 'com.studypot.aistudyleader.identity.application.GoogleOAuthLoginServiceTest' --tests 'com.studypot.aistudyleader.identity.domain.IdentityUserTest' --no-daemon` PASS.
- [x] Copilot review fix: `./gradlew check build --no-daemon` PASS.
- [x] Local Google config: added `.gitignore` entries for `src/main/resources/application-local.yml` and `.yaml`; created ignored local file with client id and env-backed client secret.
- [x] Local profile check: `./gradlew test --tests 'com.studypot.aistudyleader.AiStudyLeaderApplicationTests' --no-daemon -Dspring.profiles.active=local` PASS.
