# EXEC_PLAN: [bug] RabbitMQ 알림 worker consumer 등록 수정

- Task slug: `spt-90-rabbitmq-worker-consumer`
- Base branch: `develop`
- Feature branch: `codex/spt-90-rabbitmq-worker-consumer`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-90-rabbitmq-worker-consumer`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-90-rabbitmq-worker-consumer`
- Jira issue: `SPT-90`
- Jira URL: https://studypot.atlassian.net/browse/SPT-90
- Jira summary: [bug] RabbitMQ 알림 worker consumer 등록 수정
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] Jira SPT-90 issue description
- [x] docs/operations/local-development.md
- [x] docs/exec-plans/active/20260519-spt-84-infra-rabbitmq-llm-worker.md
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/architecture/backend-map.md

## Related Feature IDs
- [x] n/a-harness
- [x] notification

## Doc Notes
- Local runtime evidence before the fix: RabbitMQ broker is up, `/actuator/health` reports `rabbit: UP`, a RabbitMQ connection exists, and the `studypot.notification.events` queue/exchange are declared, but queue `consumers` is `0`.
- JVM class histogram evidence before the fix: `RabbitNotificationConfiguration`, `RabbitNotificationJobPublisher`, and `NotificationService` exist, but `RabbitNotificationJobWorker` is missing.
- `docs/exec-plans/active/20260519-spt-84-infra-rabbitmq-llm-worker.md`: SPT-84 intended enabled RabbitMQ mode to register a worker listener and route notification jobs through RabbitMQ.
- `docs/operations/local-development.md`: local RabbitMQ worker is opt-in through `STUDYPOT_NOTIFICATION_RABBITMQ_ENABLED=true`; default disabled behavior must remain unchanged.
- `docs/specs/notification-contract-v1.md`: RabbitMQ is async dispatch only; MySQL remains the durable source for notification status, idempotency, retry count, redacted failure, and read state.

## Goal
Fix the RabbitMQ notification worker registration bug so enabling `studypot.notification.rabbitmq.enabled=true` registers the `RabbitNotificationJobWorker` and its `@RabbitListener` consumer when `NotificationService` is created by normal application configuration.

## Approach
Use a reproduction test that matches the real application ordering: RabbitMQ configuration is processed before a separate configuration declares `NotificationService`. The current `@ConditionalOnBean(NotificationService.class)` on the worker bean can evaluate before the service bean definition exists, so the worker is skipped even though `NotificationService` is later present.

Keep the fix narrow:
- Do not change queue, exchange, routing-key, payload, API, DB schema, or notification contract.
- Keep RabbitMQ disabled by default.
- Remove or replace only the fragile worker-bean condition so the constructor dependency creates the worker when the enabled RabbitMQ path and notification service are available.
- Keep configuration tests for disabled mode, direct service registration, and service-declared-by-configuration mode.

## Step Plan
1. Add RED reproduction test.
   - Extend `RabbitNotificationConfigurationTest` with a nested configuration that declares `NotificationService` through a bean method.
   - Run only `RabbitNotificationConfigurationTest` and confirm the new test fails because `RabbitNotificationJobWorker` is missing.
2. Implement the minimal fix.
   - Update `RabbitNotificationConfiguration.rabbitNotificationJobWorker` to avoid the early false `@ConditionalOnBean(NotificationService.class)` decision.
   - Keep the constructor dependency on `NotificationService`.
3. Verify focused behavior.
   - Run `RabbitNotificationConfigurationTest`.
   - Run existing RabbitMQ worker tests.
4. Verify broader behavior.
   - Run architecture/wiring tests.
   - Run `git diff --check`.
   - Run `./gradlew check build --no-daemon`.
5. Optional local runtime smoke if the local DB/RabbitMQ are available.
   - Start the fixed app on a non-8080 port with RabbitMQ enabled.
   - Confirm `studypot.notification.events` has `consumers >= 1`.

## Done Criteria
- New regression test fails before the fix and passes after the fix.
- RabbitMQ enabled mode registers `RabbitNotificationJobWorker` when `NotificationService` is created through normal configuration ordering.
- RabbitMQ disabled mode still does not register RabbitMQ properties, publisher, or worker.
- Existing queue/exchange/routing-key defaults remain unchanged.
- No public API, OpenAPI, DB schema, notification type/channel/status, permission, or product scope change is introduced.
- Focused RabbitMQ tests and `./gradlew check build --no-daemon` pass.
- CodeRabbit review gate is latest-head PASS or evidence-backed ADDRESSED before manual merge notification.

## Verification Log
- 2026-05-19 KST: RED 확인 - `./gradlew test --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationConfigurationTest --no-daemon` failed because `RabbitNotificationJobWorker` was missing when `NotificationService` was declared by configuration.
- 2026-05-19 KST: 수정 후 `./gradlew test --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationConfigurationTest --no-daemon` passed.
- 2026-05-19 KST: `./gradlew test --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationWorkerTest --tests com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationConfigurationTest --no-daemon` was rerun alone and passed. The first parallel Gradle attempt failed from concurrent writes to `build/test-results/test/binary`, not from the product code.
- 2026-05-19 KST: `./gradlew test --tests com.studypot.aistudyleader.architecture.LayeredArchitectureTest --tests com.studypot.aistudyleader.ApplicationFeatureWiringTest --no-daemon` passed.
- 2026-05-19 KST: Local RabbitMQ runtime smoke on port `18080` with `STUDYPOT_NOTIFICATION_RABBITMQ_ENABLED=true` and local RabbitMQ confirmed `studypot.notification.events` consumers became `1`.
- 2026-05-19 KST: `git diff --check` passed.
- 2026-05-19 KST: `./gradlew check build --no-daemon` passed.
