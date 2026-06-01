# AI Study Leader Notification Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Change record: [CR-20260504-no-discord-inapp-notification](./change-requests/CR-20260504-no-discord-inapp-notification.md)
- Runtime infra record: [CR-20260519-redis-rabbitmq-realtime-infra](./change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md) and [ADR-20260519-redis-rabbitmq-realtime-infra](./adr/ADR-20260519-redis-rabbitmq-realtime-infra.md)
- Realtime stream record: [CR-20260601-notification-sse-stream](./change-requests/CR-20260601-notification-sse-stream.md) and [ADR-20260601-notification-sse-stream](./adr/ADR-20260601-notification-sse-stream.md)

## Responsibilities
- Create in-app notifications for study events that require member attention.
- Store recipient, read state, related resource links, idempotency key, and payload in `notification`.
- Keep notification creation asynchronous from core business transactions where practical.
- Never roll back group creation, onboarding submission, host start, task completion, or retrospective creation because notification creation failed.

## Notification Types
| Type | Trigger | Related Fields |
| --- | --- | --- |
| `GROUP_INVITE_CREATED` | Host creates or shares invite | `group_id` |
| `ONBOARDING_REQUESTED` | Member joins group or host needs onboarding | `related_onboarding_response_id` |
| `ONBOARDING_SUBMITTED` | Member submits onboarding | `related_onboarding_response_id` |
| `STUDY_STARTED` | Host starts study | `related_week_id` |
| `WEEK_STARTED` | Current curriculum week begins | `related_week_id` |
| `TASK_DUE_REMINDER` | Task due soon | `related_week_id`, `related_task_completion_id` |
| `TASK_OVERDUE_CHECK` | Task deadline passed | `related_task_completion_id` |
| `INCOMPLETE_REASON_REQUESTED` | Incomplete modal/event needed | `related_task_completion_id` |
| `RETROSPECTIVE_READY` | AI feedback completed | `related_retrospective_id` |
| `NEXT_WEEK_ADJUSTED` | AI team leader proposed next-week adjustment | `related_retrospective_id`, `related_week_id` |

## Delivery Channels
- MVP channel is `IN_APP`.
- Authenticated browsers may subscribe to `GET /api/v1/users/me/notifications/stream` for SSE delivery of newly created `IN_APP` notification rows.
- SSE emits `connected` when the stream is established and `notification-created` with the same `NotificationResponse` payload shape used by list/read APIs after a new delivered row is created.
- SSE delivery is scoped by `recipient_user_id`, so a user's active stream receives their notifications regardless of which group produced the event and never receives another user's notifications.
- SSE is a best-effort app-open transport. Clients must use `GET /api/v1/users/me/notifications` after reconnect to reconcile missed events.
- External channels such as Discord, email, push, or Kakao are post-MVP and require a new Change Request and ADR.

## Runtime Infrastructure Boundary
- MySQL remains the durable source for notification status, recipient, related resources, idempotency key, retry count, redacted failure, payload, and read state.
- RabbitMQ may dispatch in-app notification creation or retry jobs, but it does not define a new delivery channel and must not replace MySQL-owned notification records.
- The current SSE stream is in-process with the Spring Boot API and publishes only from the final row-creation service path; cross-process fan-out requires a later approved Redis Pub/Sub, Redis Streams, Kafka, or broker-backed task.
- Redis may support short-lived rate limit or duplicate-lock protection around expensive upstream flows, but Redis does not store final notification state.
- The current RabbitMQ listener runs in the Spring Boot application process when explicitly enabled; a separate notification worker container requires a later approved deployment task.

## Status Model
| Status | Meaning |
| --- | --- |
| `PENDING` | Notification is scheduled or waiting to be materialized. |
| `DELIVERED` | In-app notification is available to the recipient. |
| `READ` | Recipient marked the notification as read. |
| `FAILED` | Notification creation/delivery failed and can be retried or inspected. |
| `SKIPPED` | Notification was intentionally skipped by policy. |

## Idempotency
- Every notification must have `idempotency_key`.
- Duplicate idempotency key must not create duplicate recipient notifications.
- Failed attempts increment `retry_count` and store redacted `error_message`.

## Read Rules
- `recipient_user_id` is required for every MVP notification.
- Recipients can list and mark only their own notifications as read.
- Owners can read group-level notification logs for audit, but not mutate another member's read state.
