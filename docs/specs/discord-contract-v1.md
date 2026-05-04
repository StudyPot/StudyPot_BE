# AI Study Leader Discord Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.

## Responsibilities
- Store user Discord integration in `discord_integration`.
- Store group Discord delivery targets on `study_group.discord_guild_id` and `study_group.discord_channel_id`.
- Record all delivery attempts in `notification`.
- Keep notification sending asynchronous from core business transactions.

## Notification Types
| Type | Trigger | Related Fields |
| --- | --- | --- |
| `GROUP_INVITE_CREATED` | Host creates or shares invite | `group_id` |
| `ONBOARDING_REQUESTED` | Member joins group | `related_onboarding_response_id` |
| `ONBOARDING_SUBMITTED` | Member submits onboarding | `related_onboarding_response_id` |
| `STUDY_STARTED` | Host starts study | `related_week_id` |
| `TASK_DUE_REMINDER` | Task due soon | `related_week_id`, `related_task_completion_id` |
| `TASK_OVERDUE_CHECK` | Task deadline passed | `related_task_completion_id` |
| `INCOMPLETE_REASON_REQUESTED` | Incomplete modal/event needed | `related_task_completion_id` |
| `RETROSPECTIVE_READY` | AI feedback completed | `related_retrospective_id` |

## Delivery Channels
- `DISCORD_CHANNEL`: group-level channel notifications.
- `DISCORD_DM`: member-specific reminder or feedback notification.

## Idempotency
- Every notification must have `idempotency_key`.
- Duplicate idempotency key must not send twice.
- Failed sends increment `retry_count` and store redacted `error_message`.

## Token Handling
- OAuth/Discord tokens are encrypted in `discord_integration`.
- Token expiry must be checked before delivery.
- Revoked or deleted integration causes notification status `SKIPPED` or `FAILED` according to worker policy.

## Non-Rollback Rule
- Notification failure must not roll back group creation, onboarding submission, host start, task completion, or retrospective creation.
