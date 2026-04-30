# 08 Discord / 알림

## Notification Types
- `GROUP_INVITE_CREATED`
- `ONBOARDING_REQUESTED`
- `ONBOARDING_SUBMITTED`
- `STUDY_STARTED`
- `TASK_DUE_REMINDER`
- `TASK_OVERDUE_CHECK`
- `INCOMPLETE_REASON_REQUESTED`
- `RETROSPECTIVE_READY`

## Delivery
- `DISCORD_CHANNEL` for group-level events.
- `DISCORD_DM` for member-specific reminders.
- `idempotency_key` prevents duplicate sends.
- Failure does not roll back core domain events.

## Source
- `docs/specs/discord-contract-v1.md`
