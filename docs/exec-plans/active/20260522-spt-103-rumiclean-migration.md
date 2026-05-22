# EXEC_PLAN: [infra] StudyPot rumiclean 전체 이관 및 DNS 전환

- Task slug: `spt-103-rumiclean-migration`
- Base branch: `develop`
- Feature branch: `codex/spt-103-rumiclean-migration`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-103-rumiclean-migration`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-103-rumiclean-migration`
- Jira issue: `SPT-103`
- Jira URL: https://studypot.atlassian.net/browse/SPT-103
- Jira summary: [infra] StudyPot rumiclean 전체 이관 및 DNS 전환
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/operations/deployment.md
- [x] docs/architecture/backend-map.md
- [x] docs/specs/adr/ADR-20260519-redis-rabbitmq-realtime-infra.md
- [x] docs/specs/change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md
- [x] docs/operations/local-development.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] n/a-harness
- [x] identity-core
- [x] curriculum-core
- [x] retrospective-feedback
- [x] ai-team-leader
- [x] notification

## Doc Notes
- `AGENTS.md`: SPT Task, generated `codex/<slug>` worktree, filled EXEC_PLAN, tests, PR, CodeRabbit, review gate, auto-merge, and cleanup are required. No direct `src/` edits from base checkout.
- `ARCHITECTURE.md`: StudyPot remains Java 21/Spring Boot/MySQL 8. MVP state and AI/notification audit records remain MySQL-owned.
- `docs/index.md`: harness/infrastructure work uses `n/a-harness`; locked v1 API/DB/AI/notification specs cannot be changed without CR/ADR.
- `docs/operations/deployment.md`: current default deployment is Oracle API-only with `deploy/docker-compose.prod.yml`; Redis/RabbitMQ production activation must record placement, capacity, compose/env pass-through, credentials, actuator health, smoke verification, and rollback.
- `docs/architecture/backend-map.md`: current RabbitMQ listener is same-process inside Spring Boot when enabled; separate `studypot-worker` is a later task.
- `ADR-20260519-redis-rabbitmq-realtime-infra.md`: Redis owns only short-lived rate limit/TTL-lock state; RabbitMQ owns async dispatch/retry/DLQ boundaries; MySQL remains durable source of truth.
- `CR-20260519-redis-rabbitmq-realtime-infra.md`: production activation is allowed as a later deployment task and does not change public API/DB/schema contracts.
- `docs/operations/local-development.md`: Redis and RabbitMQ are disabled by default unless explicitly enabled; RabbitMQ queue/exchange/routing-key defaults are `studypot.notification.events` and `notification.create`.
- Runtime inspection: `studypot.rumiclean.com` resolves to `52.78.243.88`, the same public IP returned by `ssh rumiclean`; Caddy currently has no StudyPot route.
- Runtime inspection: `rumiclean` has 3.7GiB RAM, about 1.5GiB available, Docker running, Caddy on ports 80/443, and existing compose network `compose-cleanb_default`.
- Runtime inspection: `oracle-was` currently runs only `studypot-api`; `oracle-db` has `studypot` schema; Redis/RabbitMQ are not running on Oracle.
- `docs/operations/pr-review-gate.md`: PR completion requires latest-head CodeRabbit PASS/ADDRESSED marker, GitHub Actions Review Gate PASS, no unresolved threads, and safe cleanup.
- `docs/operations/jira-board-sync.md`: SPT Task state must move through Jira-backed start and merge completion; `finish-pr.sh` records done state idempotently.
- `docs/operations/obsidian-error-ledger.md`: real migration failures should be recorded in repo docs first; Obsidian mirror must not block progress.

## Progress Notes
- Created Jira Task `SPT-103` and initialized `codex/spt-103-rumiclean-migration` worktree.
- Added a RED static test for rumiclean migration deployment artifacts; initial run failed because `deploy/rumiclean/docker-compose.yml` did not exist.
- Added `deploy/rumiclean/docker-compose.yml`, `deploy/rumiclean/.env.example`, `deploy/rumiclean/Caddyfile.studypot`, deployment workflow compose-file selection, and rumiclean migration docs.
- `bash scripts/tests/test_rumiclean_migration_contracts.sh` passed.
- `docker compose --env-file .env -f docker-compose.yml config` passed for `deploy/rumiclean/docker-compose.yml` with placeholder non-secret env values.
- `bash scripts/tests/run.sh` passed after adding the rumiclean migration contract.
- `./gradlew check build --no-daemon` passed after the rumiclean migration contract changes.
- CodeRabbit reported one minor issue: rumiclean JDBC URL hard-coded `useSSL=false`. Fixed by introducing `STUDYPOT_MYSQL_JDBC_PARAMS` so same-host compose can keep the default while a future TLS MySQL endpoint can enforce SSL/truststore parameters.
- After the CodeRabbit fix, `bash scripts/tests/test_rumiclean_migration_contracts.sh && bash scripts/tests/run.sh`, placeholder `docker compose config`, and `git diff --check` passed.

## Goal
Migrate the complete StudyPot runtime from Oracle to the `rumiclean` AWS host, restore and verify all StudyPot MySQL data migrations, enable isolated Redis/RabbitMQ services, and make `https://studypot.rumiclean.com` serve the migrated API through Caddy while preserving rollback to the existing Oracle deployment.

## Approach
- Keep the existing `deploy/docker-compose.prod.yml` API-only contract intact so current Oracle deployment remains a safe rollback target until the final cutover.
- Add a separate `deploy/rumiclean/docker-compose.yml` full-stack contract for `studypot-api`, `studypot-mysql`, `studypot-redis`, and `studypot-rabbitmq`.
- Attach only `studypot-api` to the existing `compose-cleanb_default` Caddy network. Keep MySQL, Redis, and RabbitMQ on the StudyPot compose network with no public host ports.
- Add a `deploy/rumiclean/Caddyfile.studypot` snippet that maps `studypot.rumiclean.com` to `studypot-api:8080`.
- Add an example env file and ops documentation that never contains real secrets. Runtime secrets stay only on `rumiclean` in `/home/ec2-user/compose-studypot/.env`.
- Extend GitHub Actions deployment to support a configurable compose file path while defaulting to the existing Oracle compose path.
- Create static harness tests for the rumiclean migration contract before implementation.
- Server execution order is backup-first: capture Oracle `.env`/image metadata, dump Oracle `studypot` MySQL, create rumiclean StudyPot compose/env, restore DB, start dependencies, start API, verify health/migrations/Redis/RabbitMQ, add Caddy route, verify HTTPS DNS, then update GitHub deployment secrets if possible.
- Do not delete Oracle containers, DB, or volumes during this task. Treat them as rollback until the user later approves decommissioning.

## Step Plan
1. Add a RED static test for rumiclean migration artifacts and deployment workflow configurability.
2. Add `deploy/rumiclean/docker-compose.yml`, `deploy/rumiclean/.env.example`, and `deploy/rumiclean/Caddyfile.studypot`.
3. Update `.github/workflows/deploy.yml` so `STUDYPOT_DEPLOY_COMPOSE_FILE` can select the rumiclean compose while the default remains `deploy/docker-compose.prod.yml`.
4. Update `docs/operations/deployment.md` with rumiclean full-stack migration, DNS, backup, smoke, and rollback procedure.
5. Run the new static test, `scripts/tests/run.sh`, compose config validation with placeholder env values, and `./gradlew check build --no-daemon`.
6. Commit, create PR, run CodeRabbit, address at most one actionable review loop, wait for review gate, and auto-merge/cleanup through `finish-pr.sh`.
7. On `rumiclean`, create `/home/ec2-user/compose-studypot`, backup current Caddyfile, and install the merged rumiclean compose/Caddy snippet.
8. On Oracle, create a timestamped MySQL dump for `studypot`, record current image/env metadata without printing secrets, and copy the dump to `rumiclean`.
9. On `rumiclean`, create secret `.env` with copied StudyPot app settings, generated StudyPot MySQL/Redis/RabbitMQ credentials, and `studypot.rumiclean.com` issuer/callback/CORS values.
10. Start `studypot-mysql`, restore the dump, verify row/table counts and Flyway migration success.
11. Start `studypot-redis`, `studypot-rabbitmq`, and `studypot-api`; verify local container health, actuator health, Redis ping, RabbitMQ queue/consumer state, and app logs.
12. Add `studypot.rumiclean.com` to Caddy, reload Caddy, and verify `https://studypot.rumiclean.com/actuator/health` from local machine and from server.
13. If GitHub secret update is available locally, switch deploy host/user/dir/compose secrets to rumiclean and run deploy workflow once; otherwise record the exact external blocker.
14. Record final evidence paths, rollback commands, and old Oracle preservation state in EXEC_PLAN/Jira.

## Done Criteria
- `deploy/rumiclean/docker-compose.yml` defines isolated StudyPot MySQL, Redis, RabbitMQ, and API services with memory limits, health checks, no public DB/broker ports, and Caddy external network access only for the API.
- Deployment workflow supports configurable compose file selection and remains backward-compatible with the current default compose path.
- Static deployment tests and standard `./gradlew check build --no-daemon` pass.
- PR is merged through CodeRabbit and GitHub Actions Review Gate, and SPT-103 local cleanup path is completed unless a real external blocker prevents it.
- Oracle `studypot` DB dump is created and copied to `rumiclean`.
- `rumiclean` StudyPot MySQL restore succeeds and `flyway_schema_history` shows successful migrations.
- `rumiclean` `studypot-api`, `studypot-mysql`, `studypot-redis`, and `studypot-rabbitmq` containers are healthy or pass equivalent smoke checks.
- `https://studypot.rumiclean.com/actuator/health` returns healthy from outside the server.
- Redis and RabbitMQ health/smoke are explicitly verified after API startup.
- Oracle `oracle-was`/`oracle-db` deployment remains available as rollback; no Oracle teardown is performed in this task.
- Final notes include backup file paths, rumiclean compose path, Caddy backup path, current image tag, and rollback steps.
