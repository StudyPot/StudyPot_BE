# ADR-20260601 Notification SSE Stream

## Status
- Approved

## Context
- The MVP notification contract already stores durable `IN_APP` notifications in MySQL and exposes read/list APIs.
- SPT-121 requires app-open realtime delivery so a user can notice notifications from any group without polling.
- External push channels remain post-MVP, and Redis/RabbitMQ infrastructure boundaries keep MySQL as the notification source of truth.
- The current production shape runs a single Spring Boot API process; horizontal multi-instance fan-out is not part of this task.

## Decision
- Add authenticated `GET /api/v1/users/me/notifications/stream` using Spring MVC SSE.
- Treat SSE as a realtime transport for `IN_APP` notifications, not as a new notification channel.
- Key active SSE connections by authenticated `recipient_user_id`.
- Publish `notification-created` after `NotificationService.createNotification` successfully persists a new delivered notification row.
- Do not emit a new event for idempotency replays that resolve to an existing row.
- Catch stream-send failures so notification row creation and the original business transaction are not rolled back.
- Remove SSE registrations on completion, timeout, send failure, or emitter error.
- Use the existing list API for reconnect recovery and missed-event reconciliation.
- Defer cross-process fan-out, Redis Pub/Sub, Redis Streams, Kafka, Web Push, FCM, email, Discord, Kakao, and worker-container deployment choices to later approved tasks.

## Consequences
- Positive: Frontend can subscribe once per browser session and update notification UI in real time.
- Positive: Durable notification state, idempotency, read state, retry data, and auditability stay in MySQL.
- Positive: RabbitMQ-backed notification creation remains compatible because the final row-creation service path owns SSE publication.
- Negative: If the backend is scaled to multiple API instances later, a user connected to one instance will not receive events created only in another instance without a new fan-out layer.
- Migration or compatibility notes: No DB migration or enum change is required. Existing notification list/read API responses remain compatible.

## Affected Feature IDs
- `notification`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/notification-contract-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/operations/local-development.md`

## Linked Change Request
- [CR-20260601-notification-sse-stream](../change-requests/CR-20260601-notification-sse-stream.md)
