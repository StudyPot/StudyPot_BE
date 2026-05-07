# 08 알림

MVP 알림은 Discord가 아니라 서비스 내부 `IN_APP` 알림이다. 외부 채널은 post-MVP 확장이다.

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
- `read_at` stores member acknowledgement.
- `idempotency_key` prevents duplicate notification rows.
- Failure does not roll back core domain events.

## Source
- `docs/specs/notification-contract-v1.md`
