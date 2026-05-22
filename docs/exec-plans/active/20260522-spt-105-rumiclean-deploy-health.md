# EXEC_PLAN: [fix] rumiclean 배포 헬스체크 보강

- Task slug: `spt-105-rumiclean-deploy-health`
- Base branch: `develop`
- Feature branch: `codex/spt-105-rumiclean-deploy-health`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-105-rumiclean-deploy-health`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-105-rumiclean-deploy-health`
- Jira issue: `SPT-105`
- Jira URL: https://studypot.atlassian.net/browse/SPT-105
- Jira summary: [infra] rumiclean 배포 헬스체크 경로 보강
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] deploy/rumiclean/docker-compose.yml
- [x] deploy/rumiclean/.env.example
- [x] scripts/tests/test_rumiclean_migration_contracts.sh
- [x] docs/operations/deployment.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `AGENTS.md`: use a Jira-backed `codex/<slug>` worktree, filled EXEC_PLAN, tests, PR, CodeRabbit, review gate, merge, and cleanup.
- `ARCHITECTURE.md`: this is deployment infrastructure only; no API, DB, AI, or product behavior contract changes.
- `docs/index.md`: infrastructure changes use `n/a-harness` and still require `./gradlew check build --no-daemon`.
- `deploy/rumiclean/docker-compose.yml`: `studypot-api` is Caddy-network reachable but has no host loopback port, while the GitHub Deploy workflow verifies `http://127.0.0.1:8080/actuator/health` on the remote host.
- `deploy/rumiclean/.env.example`: needs an explicit host health port knob for operators.
- `scripts/tests/test_rumiclean_migration_contracts.sh`: can guard loopback-only API binding and prevent accidental public `0.0.0.0:8080` exposure.
- `docs/operations/deployment.md`: rumiclean runtime smoke should mention the loopback health port used by GitHub Actions.

## Goal
Make the rumiclean deployment contract compatible with the existing GitHub Actions Deploy health check while keeping MySQL, Redis, and RabbitMQ private.

## Approach
- Add a RED static contract test requiring `studypot-api` to bind only `127.0.0.1:${STUDYPOT_HTTP_PORT:-8080}:8080` on the host.
- Add the loopback-only API port to the rumiclean compose so GitHub Actions can keep using the existing remote host curl check.
- Keep DB, Redis, and RabbitMQ without public host ports.
- Document `STUDYPOT_HTTP_PORT` as an operator override and update `.env.example`.
- Verify statically, via compose config, with the harness, and with the standard Gradle build.
- After merge, apply the compose to rumiclean and run workflow_dispatch after GitHub deploy secrets are switched.

## Progress Notes
- RED: `bash scripts/tests/test_rumiclean_migration_contracts.sh` failed because rumiclean compose did not bind `studypot-api` to host loopback for the GitHub Actions health check.
- GREEN: added `127.0.0.1:${STUDYPOT_HTTP_PORT:-8080}:8080`, documented `STUDYPOT_HTTP_PORT`, and kept DB/Redis/RabbitMQ host ports private.
- Verification passed: `bash scripts/tests/test_rumiclean_migration_contracts.sh`, placeholder `docker compose config`, `bash scripts/tests/run.sh`, `git diff --check`, and `./gradlew check build --no-daemon`.
- reviewdog reported ShellCheck SC2016 on the literal compose-string assertion; changed it to double quotes with escaped `$` and re-ran the same verification set.

## Step Plan
1. Add a failing assertion to `scripts/tests/test_rumiclean_migration_contracts.sh` for loopback-only API host binding and no public `0.0.0.0:8080`.
2. Add `127.0.0.1:${STUDYPOT_HTTP_PORT:-8080}:8080` under `studypot-api.ports`.
3. Add `STUDYPOT_HTTP_PORT=8080` to `.env.example` and document the GitHub Actions health path.
4. Run `bash scripts/tests/test_rumiclean_migration_contracts.sh`, placeholder `docker compose config`, `bash scripts/tests/run.sh`, `git diff --check`, and `./gradlew check build --no-daemon`.
5. Commit, create PR, run CodeRabbit, wait for review gate, and finish/cleanup.
6. Copy the merged compose to rumiclean, recreate `studypot-api`, and verify host loopback plus public HTTPS health.

## Done Criteria
- Static contract test proves the API binds only to host loopback and DB/broker ports remain private.
- Rumiclean compose config remains valid with placeholder env values.
- Standard repository verification passes.
- PR is merged through CodeRabbit and GitHub Actions Review Gate.
- `curl http://127.0.0.1:8080/actuator/health` succeeds on rumiclean.
- After deploy secrets are switched, GitHub Actions Deploy succeeds against rumiclean.
