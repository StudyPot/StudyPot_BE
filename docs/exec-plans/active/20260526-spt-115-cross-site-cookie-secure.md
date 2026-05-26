# EXEC_PLAN: [fix] Cross-site 쿠키 Secure 배포 설정

- Task slug: `spt-115-cross-site-cookie-secure`
- Base branch: `develop`
- Feature branch: `codex/spt-115-cross-site-cookie-secure`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-115-cross-site-cookie-secure`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-115-cross-site-cookie-secure`
- Jira issue: `SPT-115`
- Jira URL: https://studypot.atlassian.net/browse/SPT-115
- Jira summary: [fix] Cross-site 쿠키 Secure 배포 설정
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/operations/deployment.md
- [x] .github/workflows/deploy.yml
- [x] deploy/rumiclean/.env.example
- [x] scripts/tests/test_deployment_contracts.sh
- [x] scripts/tests/test_rumiclean_migration_contracts.sh
- [x] src/main/resources/application.yml
- [x] src/main/java/com/studypot/server/auth/config/AuthProperties.java
- [x] src/main/java/com/studypot/server/auth/support/AuthTokenCookieIssuer.java
- [x] src/test/java/com/studypot/server/auth/oauth/GoogleOAuth2LoginHandlerTest.java

## Related Feature IDs
- [ ] n/a-harness
- [x] auth-oauth2-cookie-session-boundary

## Doc Notes
- `AGENTS.md`: implementation must use a Jira SPT Task, generated worktree, EXEC_PLAN, tests, review gate, CodeRabbit review, and finish-pr cleanup.
- `ARCHITECTURE.md`: authentication issues belong to the Spring Security/OAuth boundary; token cookie behavior is controlled by configuration rather than frontend code.
- `docs/index.md`: deployment docs are under `docs/operations/deployment.md`.
- `docs/operations/deployment.md`: Netlify frontend and rumiclean API are different sites; credentialed browser requests need `SameSite=None; Secure` plus frontend `credentials: "include"` / Axios `withCredentials: true`.
- `.github/workflows/deploy.yml`: SPT-114 added `STUDYPOT_AUTH_COOKIE_SAME_SITE` to `.runtime.env`, but `STUDYPOT_AUTH_COOKIE_SECURE` is still not GitHub Actions-managed.
- `deploy/rumiclean/.env.example`: manual fallback already uses `STUDYPOT_AUTH_COOKIE_SECURE=true`, but runtime deploy still allowed stale server `.env` to override it.
- Runtime evidence after SPT-114 deploy: `.runtime.env` had `STUDYPOT_AUTH_COOKIE_SAME_SITE=None`, while the running `studypot-api` container still had `STUDYPOT_AUTH_COOKIE_SECURE=false`. Browsers reject `SameSite=None` cookies unless the cookie is also `Secure`.

## Goal
Make the deployed rumiclean backend issue cross-site auth token cookies that browsers will accept from `https://studypot.netlify.app` credentialed requests by managing `STUDYPOT_AUTH_COOKIE_SECURE=true` through GitHub Secrets and the GitHub Actions `.runtime.env` deployment path.

## Approach
This is a deployment-contract fix, not an application-code fix. Add failing contract assertions that require `STUDYPOT_AUTH_COOKIE_SECURE` to be sourced from GitHub Secrets, validated as non-empty, written to `.runtime.env`, documented as a required secret, and present as `true` in the rumiclean manual fallback example. Then update the workflow, docs, and example env to satisfy the contract and set the real GitHub Secret to `true`.

## Step Plan
1. Add deployment-contract tests for `STUDYPOT_AUTH_COOKIE_SECURE`.
2. Run the focused deployment contract tests and confirm they fail for the current missing workflow/runtime pass-through.
3. Update `.github/workflows/deploy.yml` to read, validate, and upload `STUDYPOT_AUTH_COOKIE_SECURE`.
4. Update `deploy/rumiclean/.env.example` and `docs/operations/deployment.md` so manual fallback and required-secret docs match the cross-site cookie policy.
5. Set the GitHub repository secret `STUDYPOT_AUTH_COOKIE_SECURE=true`.
6. Run focused contract checks, shellcheck, harness tests, and `./gradlew check build --no-daemon`.
7. Commit, create PR, run CodeRabbit agent review, satisfy review gate, merge through `finish-pr.sh`.
8. Watch the develop deploy and verify server `.runtime.env`, container env, CORS preflight, and health.

## Done Criteria
- Deployment workflow uploads both `STUDYPOT_AUTH_COOKIE_SAME_SITE=None` and `STUDYPOT_AUTH_COOKIE_SECURE=true` through `.runtime.env`.
- `STUDYPOT_AUTH_COOKIE_SECURE` is a documented required GitHub Secret.
- rumiclean fallback env documents `STUDYPOT_AUTH_COOKIE_SECURE=true`.
- Focused contract tests, shellcheck, full harness, and Gradle verification pass.
- PR review gate and CodeRabbit marker pass, PR is merged to `develop`, and cleanup is recorded.
- The deployed `studypot-api` container reports `STUDYPOT_AUTH_COOKIE_SAME_SITE=None`, `STUDYPOT_AUTH_COOKIE_SECURE=true`, Netlify CORS credentials are allowed, and actuator health is UP.

## Implementation Notes
- Added contract assertions that require `STUDYPOT_AUTH_COOKIE_SECURE` to be sourced from GitHub Secrets, validated, and written to `.runtime.env`.
- Confirmed RED failure before implementation:
  - `bash scripts/tests/test_deployment_contracts.sh` failed on missing `STUDYPOT_AUTH_COOKIE_SECURE: ${{ secrets.STUDYPOT_AUTH_COOKIE_SECURE }}`.
  - `bash scripts/tests/test_rumiclean_migration_contracts.sh` failed on the same missing workflow secret.
- Updated `.github/workflows/deploy.yml` so normal deployments upload `STUDYPOT_AUTH_COOKIE_SECURE` alongside `STUDYPOT_AUTH_COOKIE_SAME_SITE`.
- Moved rumiclean manual fallback `STUDYPOT_AUTH_COOKIE_SECURE=true` into the GitHub Actions-managed runtime override block in `deploy/rumiclean/.env.example`.
- Updated deployment docs to list `STUDYPOT_AUTH_COOKIE_SECURE` as a required GitHub Secret and to document the `SameSite=None` plus `Secure=true` browser requirement.
- Set GitHub Secret `STUDYPOT_AUTH_COOKIE_SECURE=true` for `StudyPot/StudyPot_BE`.

## Verification
- `bash scripts/tests/test_deployment_contracts.sh` -> PASS
- `bash scripts/tests/test_rumiclean_migration_contracts.sh` -> PASS
- `bash scripts/tests/test_deployment_contracts.sh && bash scripts/tests/test_rumiclean_migration_contracts.sh` -> PASS
- `shellcheck --external-sources scripts/tests/test_deployment_contracts.sh scripts/tests/test_rumiclean_migration_contracts.sh` -> PASS
- `bash scripts/tests/run.sh` -> PASS
- `./gradlew check build --no-daemon` -> PASS (`BUILD SUCCESSFUL in 51s`)
