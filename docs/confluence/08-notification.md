# 08 알림

MVP 알림은 Discord가 아니라 서비스 내부 `IN_APP` 알림이다. 외부 채널은 post-MVP 확장이다.

승인된 `CR-20260601-notification-sse-stream`에 따라 앱이 열려 있는 동안에는 인증된 사용자가 자기 알림 SSE 스트림을 구독할 수 있다.

## Notification Types
- `GROUP_INVITE_CREATED`
- `ONBOARDING_REQUESTED`
- `ONBOARDING_SUBMITTED`
- `STUDY_STARTED`
- `WEEK_STARTED`
- `TASK_DUE_REMINDER`
- `TASK_OVERDUE_CHECK`
- `INCOMPLETE_REASON_REQUESTED`
- `RETROSPECTIVE_READY`
- `NEXT_WEEK_ADJUSTED`

## Delivery
- `IN_APP` for MVP.
- `recipient_user_id` targets the member.
- `GET /api/v1/users/me/notifications/stream` sends `connected` and `notification-created` SSE events for the authenticated recipient.
- Notifications from any group are delivered to the recipient's user-level stream; another user's notifications are not delivered.
- SSE is best-effort realtime transport. Reconnect recovery uses `GET /api/v1/users/me/notifications`.
- `read_at` stores member acknowledgement.
- `idempotency_key` prevents duplicate notification rows.
- Failure does not roll back core domain events.

## Source
- `docs/specs/notification-contract-v1.md`
