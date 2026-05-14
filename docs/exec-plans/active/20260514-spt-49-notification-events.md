# EXEC_PLAN: [notification] 인앱 알림 생성/재시도/읽음 처리 구현

- Task slug: `spt-49-notification-events`
- Base branch: `develop`
- Feature branch: `codex/spt-49-notification-events`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-49-notification-events`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-49-notification-events`
- Jira issue: `SPT-49`
- Jira URL: https://studypot.atlassian.net/browse/SPT-49
- Jira summary: [notification] 인앱 알림 생성/재시도/읽음 처리 구현
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/user-journeys-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260504-no-discord-inapp-notification.md
- [x] docs/specs/adr/ADR-20260504-no-discord-inapp-notification.md
- [x] docs/exec-plans/active/20260513-spt-48-notification.md

## Related Feature IDs
- [x] notification

## Doc Notes
- `notification-contract-v1.md`: MVP channel is strictly `IN_APP`; external channels such as FCM, Discord, email, push, or Kakao are post-MVP and require a new Change Request + ADR.
- `notification-contract-v1.md`: every notification needs an `idempotency_key`; duplicate keys must not create duplicate recipient rows; failures increment `retry_count` and store redacted `error_message`.
- `api-contract-v1.md` and `openapi.yaml`: public API surface is already implemented by SPT-48. SPT-49 must not add new public endpoints.
- `db-schema-v1.sql`: the existing `notification` table already contains recipient, related resource IDs, idempotency, status, delivered/read timestamps, error, retry, scheduled, and payload fields.
- `auth-permissions-v1.md`: recipients can list/mark only their own notifications, and owners can read group notification logs but cannot mutate another member's read state.
- `qa-acceptance-v1.md`: SPT-49 completes `QA-NOTI-001`, `QA-NOTI-002`, and `QA-NOTI-004` while preserving SPT-48 read-state behavior for `QA-NOTI-003`.
- `user-journeys-v1.md`: group create/join creates onboarding notifications, host start creates study/week notifications, and retrospective completion creates feedback/adjustment notifications.

## Goal
Complete the MVP in-app notification generation and retry boundary on top of the SPT-48 read-state implementation. Core domain events should be able to create `IN_APP` notification rows with stable idempotency keys, duplicate requests must return the existing row, failures must be recordable with redacted error text and retry count, and notification failures must not roll back the original group/onboarding/curriculum/retrospective action.

## Approach
- Add an internal `NotificationEventPublisher` port implemented by `NotificationService`.
- Keep all new behavior inside the existing `notification` table and enum contract; do not add public API paths, FCM, external channels, Redis, RabbitMQ, or schema changes.
- Add repository methods to insert delivered/failed notifications idempotently, retry failed rows, and resolve active group recipients for group-level events.
- Publish event notifications after the surrounding transaction commits when Spring transaction synchronization is active; otherwise publish immediately in tests/non-transactional callers.
- Hook only existing domain points that already have enough committed context: group create/join onboarding request, curriculum start week-start notification, and retrospective feedback/next-week adjustment readiness.
- Leave due-soon/overdue/incomplete-reason notifications as supported internal publisher methods for later scheduler/worker integration because no scheduler contract exists in v1.

## Step Plan
1. [x] Extend notification domain/service/repository contracts for create, failure record, retry, active-recipient lookup, and event publishing.
2. [x] Wire `NotificationEventPublisher` into study group, curriculum, and retrospective services with safe failure swallowing.
3. [x] Add focused domain/service/repository tests for idempotency, failure redaction/retry, after-commit publishing, and core-event non-rollback behavior.
4. [x] Run targeted tests for notification, study group, curriculum, and retrospective services.
5. [x] Run `./gradlew check build --no-daemon`.
6. [ ] Commit, create PR with `scripts/task/create-pr.sh`, request CodeRabbit review, handle actionable review feedback once, and run `finish-pr.sh` readiness flow.

## Verification
- [x] 2026-05-14 09:12 KST: `./gradlew test --no-daemon` passed.
- [x] 2026-05-14 09:13 KST: `./gradlew check build --no-daemon` passed.

## Done Criteria
- SPT-49 can create delivered `IN_APP` notification rows for onboarding request, week start, retrospective ready, and next-week adjustment events.
- The notification service exposes internal support for task due reminder, task overdue check, and incomplete reason requested notifications without adding a scheduler or public endpoint.
- Duplicate `idempotency_key` does not create duplicate rows.
- Failed notification attempts can be recorded with redacted `error_message` and incremented `retry_count`.
- Failed notifications can be retried to `DELIVERED` without changing read-state API behavior.
- Notification creation failure is isolated from the originating domain operation.
- No external delivery channels, FCM tokens, Redis, RabbitMQ, new tables, new enum values, or public API paths are added.
- Relevant tests and `./gradlew check build --no-daemon` pass.
