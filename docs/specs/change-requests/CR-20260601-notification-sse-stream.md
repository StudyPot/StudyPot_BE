# CR-20260601-notification-sse-stream

## Status
- Approved

## Request
- Add authenticated `GET /api/v1/users/me/notifications/stream` for user-level notification SSE subscription.
- Emit `notification-created` events after a durable `IN_APP` notification row is created for the authenticated recipient.
- Keep `GET /api/v1/users/me/notifications` as the reconnect recovery path for missed events.
- Keep delivery scoped by `recipient_user_id`, so notifications from any group reach the recipient's active stream while other users' notifications do not.
- Use connection timeout and cleanup callbacks to release server resources when the browser disconnects.

## Reason
- The locked v1 plan supports durable in-app notification listing and read state, but it requires polling to notice new notifications.
- Product direction for SPT-121 requires users to see their own notifications in real time while they are using another group screen.
- External push channels are still outside MVP, so the realtime path must preserve the existing `IN_APP` notification contract.

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

## Impact
- Product: Users can see newly delivered in-app notifications without polling while the app is open.
- API: Adds one authenticated SSE endpoint under the existing user notification resource.
- DB: No table, column, enum, constraint, or migration change. MySQL remains the durable source of truth.
- AI: None.
- Notification: Adds an in-process realtime transport for delivered `IN_APP` notification rows; it does not add a new notification channel or type.
- Permissions: Same recipient boundary as my-notification listing. The authenticated user can subscribe only to their own notification stream.
- QA: Adds SSE subscribe, recipient-only delivery, cross-group recipient delivery, send-failure isolation, and disconnect cleanup coverage.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session for SPT-121.
- Date: 2026-06-01
- Linked ADR: [ADR-20260601-notification-sse-stream](../adr/ADR-20260601-notification-sse-stream.md)
