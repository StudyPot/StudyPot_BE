# EXEC_PLAN: [notification] 인앱 알림 로그/읽음 상태 구현

- Task slug: `spt-48-notification`
- Base branch: `develop`
- Feature branch: `codex/spt-48-notification`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-48-notification`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-48-notification`
- Jira issue: `SPT-48`
- Jira URL: https://studypot.atlassian.net/browse/SPT-48
- Jira summary: [notification] 인앱 알림 로그/읽음 상태 구현
- Status: `ready-for-review`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-control-v1.md

## Related Feature IDs
- [x] notification

## Doc Notes
- `notification-contract-v1.md`: MVP channel is `IN_APP`; Discord, email, push, FCM, Kakao, and other external channels are post-MVP and require Change Request + ADR.
- `api-contract-v1.md` and `openapi.yaml`: implement existing locked endpoints only: `GET /api/v1/users/me/notifications`, `POST /api/v1/notifications/{notificationId}/read`, `POST /api/v1/users/me/notifications/read-all`, and `GET /api/v1/groups/{groupId}/notifications`.
- `db-contract-v1.md`: `notification` already exists in the baseline schema with idempotency, recipient, related resource IDs, status, delivered/read timestamps, error, retry, scheduled, and payload fields.
- `auth-permissions-v1.md`: recipients can list/mark only their own notifications; owners can read group notification logs but cannot mutate another member's read state.
- `qa-acceptance-v1.md`: SPT-48 covers read-state and permission portions of `QA-NOTI-001` and `QA-NOTI-003`; duplicate idempotency and failure rollback support should be represented in repository/domain boundaries and completed with event creation in SPT-49.

## Goal
Implement the locked MVP in-app notification read surface for SPT-48 without adding FCM or any external delivery channel: authenticated users can list their own `IN_APP` notifications, mark one or all of their own notifications as read, and owners can inspect group notification logs for audit. The implementation must preserve recipient boundaries, owner-only group audit access, stable newest-first ordering, and the existing `notification` table contract.

## Approach
- Add a `notification` package following existing domain/service/repository/controller patterns.
- Model notification type, channel, status, related resource references, payload, delivery/read timestamps, error, retry, scheduled, and created timestamps from the existing DB schema.
- Keep the write surface limited to read-state changes: mark a recipient-owned delivered/read notification as `READ` idempotently and mark all authenticated user's delivered notifications as read.
- Keep group audit read-only and owner-only, matching the existing LLM usage owner access pattern.
- Query newest notifications first with a bounded limit and optional `unreadOnly` filter. Treat the existing OpenAPI `cursor` as accepted but not advanced in this slice unless the contract already defines concrete cursor semantics.
- Do not add FCM, push tokens, delivery preferences, new tables, new notification channels, or event hook generation in SPT-48. Event creation/retry wiring belongs to SPT-49.

## Step Plan
1. Create notification domain records/enums and service commands/queries/exceptions.
2. Create repository port plus JDBC repository and SQL for own list, group audit list, recipient ownership lookup, mark-one-read, and mark-all-read.
3. Add application/persistence configuration beans and API exception mapping.
4. Add controller endpoints for the locked notification API paths.
5. Add controller tests for authentication, own list, mark one read, mark all read, owner audit, non-owner forbidden, and cross-recipient read denial.
6. Add service tests for recipient and owner permission rules, idempotent read behavior, missing notification, and missing/cross-group audit access.
7. Add repository tests for SQL filters, stable ordering, UUID/JSON/timestamp mapping, and update argument binding.
8. Run `./gradlew check build --no-daemon`, commit, create PR, run CodeRabbit review, fix actionable feedback once, and finish PR readiness.

## Done Criteria
- No FCM/external delivery channel code, schema, or docs are added.
- `GET /api/v1/users/me/notifications` returns only the authenticated user's notifications and supports `unreadOnly`.
- `POST /api/v1/notifications/{notificationId}/read` marks only a recipient-owned notification as read and rejects other users.
- `POST /api/v1/users/me/notifications/read-all` marks the authenticated user's delivered notifications as read without mutating other users' rows.
- `GET /api/v1/groups/{groupId}/notifications` is owner-only and read-only.
- Notification responses include the locked fields: id, notificationType, channel, title, body, status, scheduledAt, deliveredAt, and readAt.
- Tests cover controller success/auth/permission cases, service permission/read-state cases, and repository SQL/mapping/update binding.
- `./gradlew check build --no-daemon` passes.
- PR is created through `scripts/task/create-pr.sh`, CodeRabbit review evidence is posted, review gate is satisfied, and Mattermost manual merge notification is sent.
