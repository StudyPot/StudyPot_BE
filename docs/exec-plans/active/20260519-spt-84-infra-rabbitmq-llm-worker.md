# EXEC_PLAN: [infra] RabbitMQ 기반 LLM/알림 비동기 worker 도입

- Task slug: `spt-84-infra-rabbitmq-llm-worker`
- Base branch: `develop`
- Feature branch: `codex/spt-84-infra-rabbitmq-llm-worker`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-84-infra-rabbitmq-llm-worker`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-84-infra-rabbitmq-llm-worker`
- Jira issue: `SPT-84`
- Jira URL: https://studypot.atlassian.net/browse/SPT-84
- Jira summary: [infra] RabbitMQ 기반 LLM/알림 비동기 worker 도입
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] Jira SPT-84 issue description
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/architecture/backend-map.md
- [x] docs/operations/local-development.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/exec-plans/active/20260514-spt-78-redis-rate-limit-lock.md

## Related Feature IDs
- [x] n/a-harness
- [x] notification
- [x] ai-team-leader

## Doc Notes
- SPT-84 user decision in Jira: Redis and MQ are both used; RabbitMQ is the first MQ choice; Kafka is deferred; Redis Streams is comparison-only in separate post-MVP tickets.
- SPT-84 goal: create the RabbitMQ worker boundary for LLM generation and notification jobs, enqueue only work that passes Redis protection, and keep durable results/audit in MySQL.
- SPT-84 completion allows the first concrete flow to be either one LLM generation flow or notification creation/retry. This plan uses notification creation/retry first because existing notification idempotency, retry_count, and redacted failure behavior already exist in MySQL and do not require locked API/DB contract changes.
- `docs/specs/notification-contract-v1.md`: MVP notifications are `IN_APP`; failures must not roll back core transactions; every notification has an `idempotency_key`; duplicate keys must not create duplicate recipient notifications; failed attempts increment `retry_count` and store redacted errors.
- `docs/specs/ai-contract-v1.md`: LLM usage remains DB-audited through `llm_usage`. This PR must not move LLM result or audit ownership to RabbitMQ.
- `docs/specs/change-control-v1.md`: no endpoint, schema, enum, AI schema, notification behavior, or permission contract change is allowed without CR + ADR. This plan keeps public API and DB schema unchanged.
- `docs/architecture/backend-map.md`: service code must not depend on infrastructure packages. RabbitMQ code will live under `notification.infrastructure.rabbitmq`; service-level ports and command factories stay under `notification.service`.
- `docs/operations/local-development.md`: Redis remains disabled by default locally. RabbitMQ will also be disabled by default and documented as an opt-in local path.
- `docs/exec-plans/active/20260514-spt-78-redis-rate-limit-lock.md`: Redis owns rate limit and short TTL duplicate lock concerns; RabbitMQ owns async worker dispatch, retry/DLQ boundary, and failure isolation.

## Goal
Add a RabbitMQ-backed notification worker path that can enqueue and process in-app notification creation jobs without changing locked API or DB contracts.

The first concrete SPT-84 flow is notification creation/retry because it already has durable MySQL state, idempotency keys, redacted failure recording, and retry_count behavior. LLM job wiring remains out of this first code slice unless an existing LLM flow can be connected without API/DB/AI contract changes.

## Approach
1. Introduce a service-level queue port for notification jobs.
   - Add a `NotificationJobPublisher` port that accepts existing `CreateNotificationCommand` values.
   - Add a `QueuedNotificationEventPublisher` that implements `NotificationEventPublisher`, builds the same commands as the current direct publisher, and enqueues them after the surrounding transaction commits.
   - Keep the direct `NotificationService` event publisher path as the default when RabbitMQ is disabled.

2. Move notification command construction into one service-level factory.
   - Extract the existing private notification command builders from `NotificationService` into `NotificationCommandFactory`.
   - Use that factory from both direct and queued publishers so idempotency keys, payloads, titles, and related resources remain identical.

3. Add RabbitMQ infrastructure behind an opt-in property.
   - Add `spring-boot-starter-amqp`.
   - Add `NotificationRabbitProperties` with disabled-by-default local/test behavior.
   - Add queue/exchange/binding configuration and a RabbitMQ adapter that publishes `CreateNotificationCommand`.
   - Add a worker listener that consumes the command, calls `NotificationService.createNotification`, records redacted failure through `recordNotificationFailure`, and rejects failed messages without requeue so a broker DLQ can own repeated failures when configured.

4. Preserve existing contracts.
   - Do not add endpoints.
   - Do not alter the Flyway schema.
   - Do not add notification types/channels/statuses.
   - Do not change LLM provider behavior in this PR.
   - Keep MySQL as the durable source for notification success/failure/idempotency.

5. Use TDD.
   - Add failing tests for queued notification event publishing after commit, Rabbit adapter routing, worker success, worker duplicate idempotency behavior, and worker redacted failure recording.
   - Implement only enough code to make those tests pass, then run the standard verification.

## Step Plan
1. Baseline and RED tests
   - [x] Run focused existing notification tests to confirm the starting point.
   - [x] Add tests for `QueuedNotificationEventPublisher`:
     - enqueues onboarding notification jobs with the existing idempotency key shape;
     - enqueues week-started jobs for each active recipient;
     - swallows queue failures so upstream domain transactions are not rolled back.
   - [x] Add tests for `RabbitNotificationJobPublisher`:
     - sends a `CreateNotificationCommand` to the configured exchange/routing key.
   - [x] Add tests for `RabbitNotificationJobWorker`:
     - creates a notification from a consumed command;
     - ignores duplicate idempotency keys through existing service/repository behavior;
     - records a redacted failure and rejects without requeue when creation fails.
   - [x] Run the new tests and confirm they fail for missing classes or missing AMQP dependency.

2. Minimal implementation
   - [x] Add the AMQP dependency.
   - [x] Add `NotificationCommandFactory`, `NotificationJobPublisher`, and `QueuedNotificationEventPublisher`.
   - [x] Refactor `NotificationService` to reuse `NotificationCommandFactory` for direct event publishing.
   - [x] Add RabbitMQ properties, configuration, publisher adapter, and worker listener under `notification.infrastructure.rabbitmq`.
   - [x] Keep RabbitMQ disabled by default and mark the queued publisher `@Primary` only when the RabbitMQ notification worker property is enabled.

3. Documentation and configuration
   - [x] Add RabbitMQ local opt-in properties to `application.yml`.
   - [x] Update `docs/operations/local-development.md` with local RabbitMQ startup and disable-by-default behavior.
   - [x] Update this EXEC_PLAN verification section before commit.

4. Verification and PR flow
   - [x] Run focused notification/RabbitMQ tests.
   - [x] Run architecture tests because package boundaries changed.
   - [x] Run `./gradlew check build --no-daemon`.
   - [ ] Commit with `[feat] RabbitMQ 알림 워커 도입`.
   - [ ] Create PR with `scripts/task/create-pr.sh`.
   - [ ] Run `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`.
   - [ ] Fix one CodeRabbit pass if needed, post evidence, wait for GitHub Actions review gate, then run `scripts/task/finish-pr.sh <PR_NUMBER>`.

## Done Criteria
- EXEC_PLAN is complete and lives only in the generated SPT-84 worktree.
- RabbitMQ notification queue/exchange/binding and worker structure exist behind disabled-by-default configuration.
- At least one existing notification event flow can be routed through RabbitMQ worker when enabled.
- Worker success creates a delivered `IN_APP` notification through the existing `NotificationService`.
- Worker duplicate delivery with the same idempotency key does not create duplicate notifications.
- Worker failure records a redacted `FAILED` notification attempt through existing MySQL-backed notification behavior and rejects the message without requeue.
- Redis and RabbitMQ roles remain separate: Redis rate limit/lock code is not repurposed as the message queue.
- Public API, OpenAPI, DB schema, notification enum, AI schema, and permission contracts remain unchanged.
- Unit tests cover happy path, duplicate/idempotency edge case, queue failure isolation, and input/config validation.
- `./gradlew check build --no-daemon` passes.
- CodeRabbit review gate is latest-head PASS or evidence-backed ADDRESSED before manual merge notification.

## Verification
- `./gradlew test --tests com.studypot.aistudyleader.notification.service.NotificationServiceTest --tests com.studypot.aistudyleader.notification.repository.JdbcNotificationRepositoryTest --no-daemon` - passed.
- `./gradlew test --tests com.studypot.aistudyleader.notification.service.QueuedNotificationEventPublisherTest --no-daemon` - failed first as RED because `NotificationJobPublisher` and `QueuedNotificationEventPublisher` did not exist.
- `./gradlew test --tests com.studypot.aistudyleader.notification.service.QueuedNotificationEventPublisherTest --tests com.studypot.aistudyleader.notification.service.NotificationServiceTest --no-daemon` - passed.
- `./gradlew test --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationWorkerTest --no-daemon` - failed first as RED because Spring AMQP and RabbitMQ infrastructure classes did not exist.
- `./gradlew test --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationWorkerTest --tests com.studypot.aistudyleader.notification.service.QueuedNotificationEventPublisherTest --no-daemon` - passed after implementation.
- `./gradlew test --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationConfigurationTest --no-daemon` - failed first because the test context lacked Rabbit listener auto-configuration, then passed after importing `RabbitAutoConfiguration`.
- `./gradlew test --tests com.studypot.aistudyleader.architecture.LayeredArchitectureTest --tests com.studypot.aistudyleader.ApplicationFeatureWiringTest --no-daemon` - passed.
- `./gradlew test --tests com.studypot.aistudyleader.notification.service.NotificationServiceTest --tests com.studypot.aistudyleader.notification.service.QueuedNotificationEventPublisherTest --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationWorkerTest --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationConfigurationTest --no-daemon` - passed.
- `git diff --check` - passed.
- `./gradlew check build --no-daemon` - passed.
