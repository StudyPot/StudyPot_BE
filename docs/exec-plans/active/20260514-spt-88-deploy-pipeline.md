# EXEC_PLAN: [infra] Docker/GitHub Actions 배포 파이프라인 구축

- Task slug: `spt-88-deploy-pipeline`
- Base branch: `develop`
- Feature branch: `codex/spt-88-deploy-pipeline`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-88-deploy-pipeline`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-88-deploy-pipeline`
- Jira issue: `SPT-88`
- Jira URL: https://studypot.atlassian.net/browse/SPT-88
- Jira summary: [infra] Docker/GitHub Actions 배포 파이프라인 구축
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/operations/local-development.md
- [x] src/main/resources/application.yml
- [x] config/application-local.example.yml
- [x] .github/workflows/pr-quality.yml
- [x] .github/workflows/jira-auto-done.yml
- [x] build.gradle

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `AGENTS.md` requires this infrastructure change to stay in a `codex/<slug>` worktree, keep the Jira Task linked, fill this `EXEC_PLAN`, verify with `./gradlew check build --no-daemon`, and use the PR review gate before merge.
- `ARCHITECTURE.md` confirms Java 21 + Spring Boot, REST `/api/v1`, MySQL 8, and the standard verification command.
- `docs/index.md` keeps `LOCKED_FOR_IMPLEMENTATION` product/API/DB/AI/notification/permission/QA specs protected; this task is deploy harness work and should not change product contracts.
- `docs/operations/local-development.md` confirms secrets must stay out of committed resources and that health, Flyway, and MySQL connectivity are the important smoke surfaces.
- `.github/workflows/pr-quality.yml` already runs harness, actionlint, DB schema coverage, and `./gradlew check build --no-daemon`; deploy workflow should be separate from PR quality and should not weaken the existing gate.
- `src/main/resources/application.yml` already exposes Hikari pool size, OpenAPI toggles, JWT, OAuth, CORS, and logging through environment variables. Production compose should set only safe defaults and leave secrets to server/GitHub Secrets.

## Goal
Add a deployable Docker/GitHub Actions path for StudyPot so the backend can be packaged as an image, run on `oracle-was`, connect to `oracle-db` MySQL, pass `/actuator/health`, and later be redeployed by GitHub Actions.

## Approach
Create small, explicit deployment artifacts without changing application code:
- A multi-stage `Dockerfile` builds the Spring Boot jar with Gradle and runs it on a Java 21 runtime image.
- `.dockerignore` keeps local build outputs, IDE files, task state, and secrets out of the build context.
- `deploy/docker-compose.prod.yml` runs only the backend service on `oracle-was`, constrains memory-sensitive JVM/Hikari defaults, and reads secrets from a server-side `.env` file.
- `docs/operations/deployment.md` records the manual-first flow, GitHub Secrets contract, server files, DB connectivity requirements, health checks, and rollback commands.
- `.github/workflows/deploy.yml` builds and pushes a GHCR image, then SSHes to `oracle-was` to pull/recreate the service and verify `/actuator/health`.
- `scripts/tests/test_deployment_contracts.sh` statically verifies the deployment files, secret hygiene, health check, GHCR push, SSH deploy command, and workflow/compose contracts.

## Step Plan
1. Add deployment contract tests first in `scripts/tests/test_deployment_contracts.sh` and register them in `scripts/tests/run.sh`.
2. Add `.dockerignore` and `Dockerfile` for Java 21 Spring Boot image builds.
3. Add `deploy/docker-compose.prod.yml` with no committed secret values, small-server JVM defaults, Hikari pool limits, and `studypot-api` healthcheck.
4. Add `docs/operations/deployment.md` with manual deploy, GitHub Actions deploy, required secrets, DB URL shape, and done criteria.
5. Add `.github/workflows/deploy.yml` for `workflow_dispatch` and `push` to `develop`: Gradle verification, Docker build/push to GHCR, SSH deployment, and health check.
6. Run `bash scripts/tests/test_deployment_contracts.sh`, `bash scripts/tests/run.sh`, and `./gradlew check build --no-daemon`.
7. Build the Docker image locally or through the workflow-compatible command and smoke test the image where feasible.
8. Perform one manual deploy to `oracle-was`, confirm `docker compose ps`, container logs, `/actuator/health`, and `oracle-db` Flyway state.
9. Create the PR with `scripts/task/create-pr.sh`, run the CodeRabbit review gate, address actionable feedback once, and wait for human merge.
10. After the human merge to `develop`, verify the GitHub Actions deploy workflow succeeds, server health remains UP, and run `scripts/task/finish-pr.sh cleanup-merged <PR_NUMBER>`.

## Done Criteria
- `SPT-88` remains linked in task state and this `EXEC_PLAN` has no empty required sections.
- Deployment files exist: `Dockerfile`, `.dockerignore`, `deploy/docker-compose.prod.yml`, `.github/workflows/deploy.yml`, `docs/operations/deployment.md`, `scripts/tests/test_deployment_contracts.sh`.
- No committed file contains the real DB password, JWT secret, OAuth secret, OpenAI key, or other production secret values.
- `bash scripts/tests/test_deployment_contracts.sh` passes.
- `bash scripts/tests/run.sh` passes.
- `./gradlew check build --no-daemon` passes.
- A Docker image builds successfully from the repo.
- Manual deployment to `oracle-was` succeeds and `curl -f http://localhost:8080/actuator/health` returns success from the server.
- The app connects to `oracle-db` MySQL and Flyway migration history exists in the `studypot` schema.
- A PR targeting `develop` passes required PR quality checks and CodeRabbit review gate.
- After the human merge, GitHub Actions deploy workflow succeeds for the merged commit and the server health check passes.
- Post-merge cleanup records the merged Jira/branch/worktree state.

## Verification Evidence
- [x] `bash scripts/tests/test_deployment_contracts.sh` passed after deployment contract files were added.
- [x] `bash scripts/tests/run.sh` passed after registering `test_deployment_contracts.sh`.
- [x] `./gradlew check build --no-daemon` passed.
- [x] `docker compose -f deploy/docker-compose.prod.yml config` passed with placeholder non-secret environment values.
- [x] GHCR deployment contract update passed `bash scripts/tests/test_deployment_contracts.sh`, `bash scripts/tests/run.sh`, `./gradlew check build --no-daemon`, workflow YAML parse, compose config, and `docker buildx build --platform linux/amd64 --load -t ghcr.io/studypot/studypot-api:spt-88-ghcr-contract .`.
- [x] Local Docker image build passed for `studypot-api:spt-88-manual`.
- [x] Initial manual deploy exposed a real platform issue: local Mac image was `linux/arm64` and failed on `oracle-was` `linux/amd64` with `exec format error`.
- [x] Deployment artifacts were corrected to use `linux/amd64`; `studypot-api:spt-88-manual-amd64` built successfully.
- [x] Manual deploy to `oracle-was` succeeded with `studypot-api:spt-88-manual-amd64`.
- [x] `curl -fsS http://127.0.0.1:8080/actuator/health` on `oracle-was` returned `{"groups":["liveness","readiness"],"status":"UP"}`.
- [x] `docker compose ps` on `oracle-was` reported `studypot-api` as `healthy`.
- [x] `oracle-db` query confirmed `studypot.flyway_schema_history` exists with V1 and V2 success rows.
- [x] GitHub deploy secrets registered: `STUDYPOT_DEPLOY_HOST`, `STUDYPOT_DEPLOY_USER`, `STUDYPOT_DEPLOY_DIR`, `STUDYPOT_DEPLOY_KNOWN_HOSTS`, `STUDYPOT_DEPLOY_SSH_KEY`.
- [ ] PR review gate completed.
- [ ] Human merge completed.
- [ ] GitHub Actions `Deploy` workflow completed successfully for the merged commit.
