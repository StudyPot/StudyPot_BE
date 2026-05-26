# EXEC_PLAN: [chore] OAuth 프론트 리디렉션 env GitHub Actions 관리

- Task slug: `spt-113-actions-managed-oauth-env`
- Base branch: `develop`
- Feature branch: `codex/spt-113-actions-managed-oauth-env`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-113-actions-managed-oauth-env`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-113-actions-managed-oauth-env`
- Jira issue: `SPT-113`
- Jira URL: https://studypot.atlassian.net/browse/SPT-113
- Jira summary: [chore] OAuth 프론트 리디렉션 env GitHub Actions 관리
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/local-development.md
- [x] docs/operations/deployment.md
- [x] .github/workflows/deploy.yml
- [x] scripts/tests/test_deployment_contracts.sh
- [x] scripts/tests/test_rumiclean_migration_contracts.sh
- [x] deploy/rumiclean/.env.example

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `AGENTS.md`: implementation must stay in this generated worktree, include a non-empty EXEC_PLAN, tests, verification, PR, CodeRabbit, review gate, merge, and cleanup.
- `ARCHITECTURE.md`: this is deployment/harness work only; no product/API/DB/AI contract behavior changes are intended.
- `docs/index.md`: harness/infrastructure work uses `n/a-harness` and the standard verification command is `./gradlew check build --no-daemon`.
- `docs/operations/deployment.md`: `.runtime.env` is the GitHub Actions-managed runtime override file; current docs only describe AI runtime values in that file.
- `.github/workflows/deploy.yml`: the runtime upload step previously wrote only AI secrets into `.runtime.env`; OAuth frontend redirects and CORS were not passed through from GitHub Secrets.
- `scripts/tests/test_deployment_contracts.sh` and `scripts/tests/test_rumiclean_migration_contracts.sh`: static deployment contracts should fail until the new GitHub Secrets passthrough is asserted.
- Runtime evidence before this task: `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI`, `STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI`, and `STUDYPOT_CORS_ALLOWED_ORIGINS` were manually present in `/home/ec2-user/compose-studypot/.env`, while `gh secret list` did not include those names.

## Goal
Make the deployed frontend OAuth redirect and credentialed CORS origins GitHub Actions-managed instead of server `.env`-managed, using GitHub repository secrets as the source of truth and `.runtime.env` as the deployment carrier.

## Approach
Add RED static deployment assertions for the three frontend runtime env keys in the Deploy workflow, `.runtime.env` upload block, documentation, and rumiclean deployment contract. Then update `.github/workflows/deploy.yml` to require and upload `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI`, `STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI`, and `STUDYPOT_CORS_ALLOWED_ORIGINS` from GitHub Secrets. Update deployment docs to distinguish server-only `.env` secrets from Actions-managed frontend/AI overrides. Configure the GitHub repository secrets with the current deployed Netlify values, run the standard verification path, create the PR, pass CodeRabbit and the review gate, merge, and verify the deployed container uses the GitHub Actions-managed values.

## Step Plan
1. Add failing static contract checks for frontend OAuth/CORS env passthrough in the deploy workflow and docs.
2. Update `.github/workflows/deploy.yml` so the Deploy job reads the three GitHub Secrets, validates they are non-empty, and writes them to `.runtime.env`.
3. Update deployment docs and rumiclean contracts to document the new source of truth and required secret names.
4. Set the three GitHub repository secrets to the current Netlify deployment values.
5. Run focused static tests, shellcheck for touched shell tests if available, and `./gradlew check build --no-daemon`.
6. Commit, create PR, run CodeRabbit, satisfy review gate, finish/merge, and verify the deployed container plus OAuth/CORS smoke.

## Done Criteria
- GitHub repository secrets contain the frontend OAuth success URI, failure URI, and CORS allowed origins values without printing unrelated secret values.
- Deploy workflow uploads those values into `.runtime.env` and fails early if the required frontend runtime secrets are missing.
- Deployment docs say these values are GitHub Actions-managed, not server `.env`-owned.
- Static deployment tests and `./gradlew check build --no-daemon` pass.
- PR is created, CodeRabbit review marker passes or is addressed with evidence, GitHub Actions Review Gate passes, PR is merged, and cleanup completes.
- Production `studypot-api` is healthy and shows the Netlify success/failure URIs plus Netlify CORS origin after the merge deploy.

## Implementation Notes
- RED: `bash scripts/tests/test_deployment_contracts.sh` failed because `.github/workflows/deploy.yml` did not read `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI` from GitHub Secrets.
- RED: `bash scripts/tests/test_rumiclean_migration_contracts.sh` failed because `deploy/rumiclean/.env.example` still documented the same-domain frontend success URI.
- GREEN: `.github/workflows/deploy.yml` now requires the three frontend runtime secrets and writes them into `.runtime.env` before AI runtime values.
- GREEN: `deploy/rumiclean/.env.example` and `docs/operations/deployment.md` now describe GitHub Actions-managed frontend OAuth/CORS runtime overrides.
- Secret setup: `gh secret set` populated `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI`, `STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI`, and `STUDYPOT_CORS_ALLOWED_ORIGINS` in `StudyPot/StudyPot_BE` without printing secret bodies.

## Verification
- `bash scripts/tests/test_deployment_contracts.sh && bash scripts/tests/test_rumiclean_migration_contracts.sh` - passed.
- `shellcheck --external-sources scripts/tests/test_deployment_contracts.sh scripts/tests/test_rumiclean_migration_contracts.sh` - passed.
- `./gradlew check build --no-daemon` - passed.
- `bash scripts/tests/run.sh` - passed.
