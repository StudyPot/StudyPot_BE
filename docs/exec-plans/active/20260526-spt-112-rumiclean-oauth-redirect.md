# EXEC_PLAN: [fix] rumiclean Google OAuth 성공 리디렉션 연결

- Task slug: `spt-112-rumiclean-oauth-redirect`
- Base branch: `develop`
- Feature branch: `codex/spt-112-rumiclean-oauth-redirect`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-112-rumiclean-oauth-redirect`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-112-rumiclean-oauth-redirect`
- Jira issue: `SPT-112`
- Jira URL: https://studypot.atlassian.net/browse/SPT-112
- Jira summary: [fix] rumiclean Google OAuth 성공 리디렉션 연결
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/adr/ADR-20260508-oauth2-cookie-login.md
- [x] docs/specs/change-requests/CR-20260508-oauth2-cookie-login.md
- [x] docs/operations/local-development.md
- [x] docs/operations/deployment.md
- [x] deploy/rumiclean/.env.example
- [x] deploy/rumiclean/Caddyfile.studypot
- [x] scripts/tests/test_deployment_contracts.sh
- [x] scripts/tests/test_rumiclean_migration_contracts.sh

## Related Feature IDs
- [x] identity-core

## Doc Notes
- 2026-05-26 runtime evidence:
  - `https://studypot.rumiclean.com/api/oauth2/authorization/google` returned `401` with problem `instance=/error`, not a Google OAuth redirect.
  - Direct server curl to `http://127.0.0.1:8080/api/oauth2/authorization/google` returned the same `401`.
  - `studypot-api` runtime env had `STUDYPOT_GOOGLE_CLIENT_ID` and `STUDYPOT_GOOGLE_CLIENT_SECRET` set, but `STUDYPOT_OAUTH_GOOGLE_CLIENT_ID` and `STUDYPOT_OAUTH_GOOGLE_CLIENT_SECRET` were empty.
  - Source currently binds `studypot.oauth.google.client-id` only in `config/application-local.example.yml`; production `src/main/resources/application.yml` lacks the equivalent mapping from `STUDYPOT_GOOGLE_CLIENT_ID`.
  - `studypot.rumiclean.com/auth/success` currently hits the backend and returns `401` because deployed Caddy routes every StudyPot path to `studypot-api`. No StudyPot frontend container/upstream is running on `rumiclean` yet.
- Locked OAuth contract already permits backend-owned redirect login and says success redirects to configured frontend success URI without token query parameters. This task stays within that approved CR/ADR and does not change API shape.
- 2026-05-26 verification:
  - RED: `./gradlew test --tests com.studypot.aistudyleader.auth.controller.GoogleOAuth2ProductionEnvBindingTest --no-daemon` failed because production-style Google client env names did not activate the OAuth login redirect path.
  - GREEN focused: `./gradlew test --tests com.studypot.aistudyleader.auth.infrastructure.security.GoogleOAuth2ProductionConfigTest --tests com.studypot.aistudyleader.auth.controller.GoogleOAuth2LoginFlowTest --no-daemon` passed.
  - Static deployment contracts passed: `bash scripts/tests/test_deployment_contracts.sh`; `bash scripts/tests/test_rumiclean_migration_contracts.sh`.
  - Full verification passed: `./gradlew check build --no-daemon`.

## Goal
Make the deployed rumiclean backend OAuth entrypoint use the existing `STUDYPOT_GOOGLE_CLIENT_ID` and `STUDYPOT_GOOGLE_CLIENT_SECRET` env values so `/api/oauth2/authorization/google` redirects to Google, and clarify the deployment contract for connecting the configured frontend success/failure URI to the real frontend handler.

## Approach
Use a reproduction test that loads the app with production-style `STUDYPOT_GOOGLE_CLIENT_ID` / `STUDYPOT_GOOGLE_CLIENT_SECRET` properties instead of direct `studypot.oauth.google.*` properties. The RED failure should show the Google authorization endpoint is not a 3xx redirect. Then add the missing production property mapping in `src/main/resources/application.yml`, strengthen static deployment tests so the mapping cannot drift again, and document that `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI` must point to an actual frontend origin/route, not an API-only Caddy route.

## Step Plan
1. Add a RED Spring MVC test proving production-style `STUDYPOT_GOOGLE_CLIENT_ID` and `STUDYPOT_GOOGLE_CLIENT_SECRET` enable `/api/oauth2/authorization/google`.
2. Run the focused test and confirm it fails with a non-redirect status before production code changes.
3. Add `client-id` and `client-secret` placeholders under `studypot.oauth.google` in `src/main/resources/application.yml`.
4. Add static deployment contract assertions that production config maps the existing compose env names into the Spring OAuth property namespace.
5. Update deployment docs with the rumiclean frontend success/failure routing requirement and the current API-only Caddy caveat.
6. Run focused tests:
   - `./gradlew test --tests com.studypot.aistudyleader.auth.controller.GoogleOAuth2LoginFlowTest --no-daemon`
   - `bash scripts/tests/test_deployment_contracts.sh`
   - `bash scripts/tests/test_rumiclean_migration_contracts.sh`
7. Run full verification: `./gradlew check build --no-daemon`.
8. Create PR with `scripts/task/create-pr.sh`, run CodeRabbit agent review once, address actionable feedback once, verify PR readiness, finish/merge, then verify deployed `/api/oauth2/authorization/google` returns a Google redirect.

## Done Criteria
- `src/main/resources/application.yml` maps `STUDYPOT_GOOGLE_CLIENT_ID` and `STUDYPOT_GOOGLE_CLIENT_SECRET` into `studypot.oauth.google.client-id/client-secret`.
- A production-style test proves the backend OAuth authorization entrypoint redirects to Google using only those env-style names.
- Deployment contract tests prevent the env/property binding from drifting again.
- Docs tell operators that the configured frontend success/failure URI must resolve to the frontend handler; if `studypot.rumiclean.com` remains API-only, `/auth/success` will not be a working frontend handler.
- `./gradlew check build --no-daemon` passes and task state records the successful verification.
- PR review gate, CodeRabbit marker, auto-merge, deploy, and post-merge cleanup complete unless an external blocker is documented.
