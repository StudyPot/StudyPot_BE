# EXEC_PLAN: [docs] Redis/RabbitMQ 실시간 인프라 결정 ADR 정리

- Task slug: `spt-87-docs-redis-rabbitmq-adr`
- Base branch: `develop`
- Feature branch: `codex/spt-87-docs-redis-rabbitmq-adr`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-87-docs-redis-rabbitmq-adr`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-87-docs-redis-rabbitmq-adr`
- Jira issue: `SPT-87`
- Jira URL: https://studypot.atlassian.net/browse/SPT-87
- Jira summary: [docs] Redis/RabbitMQ 실시간 인프라 결정 ADR 정리
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/adr-template.md
- [x] docs/specs/change-request-template.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/architecture/backend-map.md
- [x] docs/operations/local-development.md
- [x] docs/operations/deployment.md
- [x] docs/confluence/00-doc-hub.md
- [x] docs/confluence/04-erd-data-model.md
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/exec-plans/active/20260514-spt-78-redis-rate-limit-lock.md
- [x] docs/exec-plans/active/20260519-spt-84-infra-rabbitmq-llm-worker.md
- [x] scripts/tests/test_docs_source_of_truth.sh
- [x] scripts/tests/test_deployment_contracts.sh

## Related Feature IDs
- [x] n/a-harness
- [x] identity-core
- [x] curriculum-core
- [x] retrospective-feedback
- [x] ai-team-leader
- [x] notification

## Doc Notes
- SPT-78 introduced Redis as a short-lived protection layer for rate limit and future duplicate generation locks. Redis must not become the source of truth for `llm_usage`, `notification`, curriculum, retrospective, or conversation records.
- SPT-84 introduced a RabbitMQ-backed notification worker path behind disabled-by-default configuration. The first worker currently runs in the same Spring Boot application process when enabled.
- `docs/operations/deployment.md`: current production deployment runs only the `studypot-api` container on `oracle-was`; MySQL 8 runs on `oracle-db`; Redis and RabbitMQ are not present in `deploy/docker-compose.prod.yml`.
- `docs/operations/local-development.md`: Redis and RabbitMQ are opt-in local paths, disabled by default, with actuator health disabled unless the path is intentionally exercised.
- `docs/specs/change-control-v1.md`: notification delivery behavior and AI/notification infrastructure boundary clarifications should be recorded through Change Request + ADR even when public API and DB schema do not change.

## Goal
Record the Redis/RabbitMQ realtime infrastructure decision as an approved Change Request and ADR, then align architecture and operations docs so future Redis, RabbitMQ, LLM worker, Pub/Sub, Streams, and deployment tasks do not reinterpret the runtime boundary.

## Approach
Keep this as a documentation and contract task.

- Add `CR-20260519-redis-rabbitmq-realtime-infra` and matching ADR.
- State the role split explicitly: MySQL is durable source of truth, Redis is short-lived rate limit/lock state, RabbitMQ is asynchronous dispatch with retry/DLQ ownership, and the Spring Boot API currently owns the same-process worker listener.
- Record the physical deployment boundary: current `oracle-was` remains API-only; Redis/RabbitMQ production activation or a separate worker container requires a later deployment task with capacity, secrets, health, and compose changes.
- Add a Mermaid runtime diagram to the architecture map.
- Link the decision from AI, notification, feature coverage, operations, and Confluence draft docs without adding API paths, DB tables, enums, notification types, or permission changes.
- Add static documentation checks for the new CR/ADR references and deployment boundary language.

## Step Plan
1. Create the CR and ADR.
   - Add `docs/specs/change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md`.
   - Add `docs/specs/adr/ADR-20260519-redis-rabbitmq-realtime-infra.md`.
   - Mark both approved because the user selected SPT-87 after the SPT-84 merge and server architecture discussion.
2. Update locked-source links.
   - Add the new CR/ADR as the current approved change in `docs/specs/change-control-v1.md`.
   - Add narrow references to `docs/specs/ai-contract-v1.md`, `docs/specs/notification-contract-v1.md`, and `docs/specs/feature-coverage-matrix.md`.
3. Update architecture and operations docs.
   - Add a Redis/RabbitMQ/MySQL/API runtime diagram and role notes to `docs/architecture/backend-map.md`.
   - Clarify production placement and activation prerequisites in `docs/operations/deployment.md`.
   - Add a shared role-boundary note to `docs/operations/local-development.md`.
4. Update Confluence draft mirrors.
   - Refresh the doc hub current change.
   - Add transient infrastructure notes to ERD/data-model and AI-team-leader drafts.
5. Add static validation.
   - Extend docs-source tests for the new CR/ADR and source links.
   - Extend deployment contract tests for the production activation boundary.
6. Verify.
   - Run `bash scripts/tests/test_docs_source_of_truth.sh`.
   - Run `bash scripts/tests/test_deployment_contracts.sh`.
   - Run `bash scripts/tests/run.sh`.
   - Run `./gradlew check build --no-daemon`.

## Done Criteria
- CR and ADR exist, are linked, and are referenced from change control.
- Architecture docs show API, same-process RabbitMQ listener, MySQL, Redis, RabbitMQ, GitHub Actions/GHCR, and future worker split boundaries.
- Deployment docs clearly state Redis/RabbitMQ are not added to the current production compose file by this task and require a later deployment task before enabling production toggles.
- AI and notification docs state that Redis/RabbitMQ do not replace MySQL-owned audit, result, idempotency, retry, or read-state records.
- No API, OpenAPI, DB schema, enum, notification type, permission, or product scope change is introduced.
- Static docs tests and the standard `./gradlew check build --no-daemon` verification pass.
- CodeRabbit review gate is latest-head PASS or evidence-backed ADDRESSED before manual merge notification.

## Verification
- `bash scripts/tests/test_docs_source_of_truth.sh` - passed.
- `bash scripts/tests/test_deployment_contracts.sh` - failed first because new test strings used shell backticks inside double quotes; fixed the test quoting and reran successfully.
- `git diff --check` - passed.
- `bash scripts/tests/run.sh` - passed.
- `./gradlew check build --no-daemon` - passed.
