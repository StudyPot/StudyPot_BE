# EXEC_PLAN: [fix] Netlify 교차 출처 쿠키 전송 설정

- Task slug: `spt-114-cross-site-cookie-samesite`
- Base branch: `develop`
- Feature branch: `codex/spt-114-cross-site-cookie-samesite`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-114-cross-site-cookie-samesite`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-114-cross-site-cookie-samesite`
- Jira issue: `SPT-114`
- Jira URL: https://studypot.atlassian.net/browse/SPT-114
- Jira summary: [fix] Netlify 교차 출처 쿠키 전송 설정
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/operations/deployment.md
- [x] .github/workflows/deploy.yml
- [x] scripts/tests/test_deployment_contracts.sh
- [x] scripts/tests/test_rumiclean_migration_contracts.sh
- [x] deploy/rumiclean/.env.example
- [x] src/main/resources/application.yml
- [x] src/main/java/com/studypot/aistudyleader/auth/infrastructure/security/AuthTokenCookieIssuer.java
- [x] src/main/java/com/studypot/aistudyleader/auth/infrastructure/security/AuthProperties.java
- [x] src/test/java/com/studypot/aistudyleader/auth/infrastructure/security/GoogleOAuth2LoginHandlerTest.java

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `AGENTS.md`: implementation must stay in this generated worktree and continue through tests, PR, review gate, merge, deployment, and cleanup unless blocked.
- `ARCHITECTURE.md`: this is deployment/auth runtime configuration work; no locked API/DB/product contract change is intended.
- `docs/index.md`: harness/infrastructure work uses `n/a-harness` and standard verification is `./gradlew check build --no-daemon`.
- `AuthTokenCookieIssuer`: token cookies already use configured `properties.cookie().sameSite()` and `properties.cookie().secure()`, so the runtime bug is configuration, not cookie construction code.
- `application.yml`: default `STUDYPOT_AUTH_COOKIE_SAME_SITE` is `Lax`, which is safe for same-site/local defaults but does not allow Netlify-to-rumiclean XHR/fetch cookies.
- `deploy/rumiclean/.env.example`: rumiclean production fallback still documents `STUDYPOT_AUTH_COOKIE_SAME_SITE=Lax`.
- `.github/workflows/deploy.yml`: `.runtime.env` currently carries frontend OAuth/CORS and AI values, but not the cookie SameSite policy.
- Browser/runtime diagnosis: `https://studypot.netlify.app` and `https://studypot.rumiclean.com` are different sites, so credentialed cross-site requests require token cookies with `SameSite=None; Secure`, plus frontend `credentials: "include"` / Axios `withCredentials: true`.

## Goal
Make production token cookies usable from the Netlify frontend's credentialed cross-site requests by managing `STUDYPOT_AUTH_COOKIE_SAME_SITE=None` through GitHub Secrets and the Deploy workflow.

## Approach
Add RED deployment contract assertions that fail until the Deploy workflow reads, validates, and uploads `STUDYPOT_AUTH_COOKIE_SAME_SITE` to `.runtime.env`, and until rumiclean production examples document `None`. Then update the workflow/docs/env example, set the GitHub repository secret to `None`, run verification, PR/review/merge through the harness, and verify production `Set-Cookie` contains `SameSite=None; Secure`.

## Step Plan
1. Add failing static deployment assertions for `STUDYPOT_AUTH_COOKIE_SAME_SITE` in workflow, docs, and rumiclean env example.
2. Update `.github/workflows/deploy.yml` to require the GitHub Secret and write it into `.runtime.env`.
3. Update deployment docs and rumiclean `.env.example` to document Netlify cross-site cookie requirements.
4. Set GitHub repository secret `STUDYPOT_AUTH_COOKIE_SAME_SITE=None`.
5. Run focused static tests, shellcheck for touched tests, and `./gradlew check build --no-daemon`.
6. Commit, create PR, run CodeRabbit, pass review gate, finish/merge, watch Deploy, remove stale SameSite from server `.env`, and verify live cookies/CORS.

## Done Criteria
- GitHub repository secret `STUDYPOT_AUTH_COOKIE_SAME_SITE` is set to `None`.
- Deploy workflow writes `STUDYPOT_AUTH_COOKIE_SAME_SITE` into `.runtime.env` and fails early if the secret is missing.
- Server `.env` no longer owns a duplicate `STUDYPOT_AUTH_COOKIE_SAME_SITE` value after deploy cleanup.
- Static tests, shellcheck, and `./gradlew check build --no-daemon` pass.
- PR is merged through CodeRabbit and GitHub Actions review gates.
- Production `studypot-api` is healthy and token `Set-Cookie` headers include `SameSite=None; Secure`.

## Implementation Notes
- RED: `bash scripts/tests/test_deployment_contracts.sh` failed because `.github/workflows/deploy.yml` did not read `STUDYPOT_AUTH_COOKIE_SAME_SITE` from GitHub Secrets.
- RED: `bash scripts/tests/test_rumiclean_migration_contracts.sh` failed because `deploy/rumiclean/.env.example` still documented `STUDYPOT_AUTH_COOKIE_SAME_SITE=Lax`.
- GREEN: Deploy workflow now requires `STUDYPOT_AUTH_COOKIE_SAME_SITE` and writes it to `.runtime.env`.
- GREEN: rumiclean `.env.example` and deployment docs now document `SameSite=None` for Netlify-to-rumiclean credentialed requests.
- Secret setup: `gh secret set STUDYPOT_AUTH_COOKIE_SAME_SITE` populated the GitHub repository secret with `None`.

## Verification
- `bash scripts/tests/test_deployment_contracts.sh && bash scripts/tests/test_rumiclean_migration_contracts.sh` - passed.
- `shellcheck --external-sources scripts/tests/test_deployment_contracts.sh scripts/tests/test_rumiclean_migration_contracts.sh` - passed.
- `bash scripts/tests/run.sh` - passed.
- `./gradlew check build --no-daemon` - passed.
