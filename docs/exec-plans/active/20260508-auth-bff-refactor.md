# EXEC_PLAN: [refactor] auth BFF 인증 흐름 정리

- Task slug: `auth-bff-refactor`
- Base branch: `develop`
- Feature branch: `codex/auth-bff-refactor`
- Worktree: `/Users/hyunwoo/Developer/Projects/StudyPot`
- Port: ``
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/auth-bff-refactor`
- Jira issue: ``
- Jira URL: ``
- Jira summary: ``
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/architecture/backend-map.md
- [x] docs/operations/local-development.md
- [x] docs/specs/auth-permissions-v1.md

## Related Feature IDs
- [x] auth-core

## Doc Notes
- User decision: rename the existing `identity` bounded context package to `auth` because the responsibility is authentication, token, and session handling.
- User decision: move toward a BFF-style auth boundary where the frontend depends on cookie-backed session endpoints instead of handling Google authorization-code exchange payloads.
- This refactor keeps the REST auth paths, database tables, Google OAuth2 callback, token issuance, and cookie names unchanged unless explicitly changed in code.
- Local YAML tab indentation was fixed so Spring Boot can parse the base and local example configuration.

## Goal
Rename the authentication bounded context from `identity` to `auth` and align the auth controller with a cookie-backed BFF session flow.

## Approach
1. Move production and test packages from `com.studypot.aistudyleader.identity` to `com.studypot.aistudyleader.auth`.
2. Rename major identity-prefixed types to auth-prefixed names where the old name would confuse future package navigation.
3. Remove the JSON request/response token exposure from direct Google code-exchange controller tests and use cookie-backed session refresh/logout behavior.
4. Update architecture documentation and architecture tests to make `auth` the bounded context name and reject legacy `identity` packages.
5. Preserve existing OAuth2 redirect/callback infrastructure and protected API behavior.
6. Verify the full Gradle check/build command before committing.

## Step Plan
- [x] Confirm current architecture/package rules.
- [x] Move `identity` production package to `auth`.
- [x] Move `identity` test package to `auth`.
- [x] Rename visible identity-prefixed Java types to auth-prefixed names.
- [x] Update BFF-style auth controller tests for cookie-only refresh/logout.
- [x] Update architecture docs and tests.
- [x] Fix YAML indentation issues in application configuration.
- [x] Run `./gradlew check build --no-daemon`.

## Done Criteria
- No production or test package remains under `com.studypot.aistudyleader.identity`.
- Authentication code lives under `com.studypot.aistudyleader.auth`.
- Architecture tests reject legacy `identity` packages.
- Refresh/logout behavior depends on HttpOnly refresh-token cookies instead of JSON refresh-token bodies.
- YAML configuration files parse without tab indentation failures.
- `./gradlew check build --no-daemon` passes.

## Verification
- PASS: `git diff --check`.
- PASS: YAML parse check for `src/main/resources/application.yml`, `config/application-local.example.yml`, and `config/application-local.yml`.
- PASS: `./gradlew check build --no-daemon`.
