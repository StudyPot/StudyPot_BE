# CR-20260504 No Discord In-App Notification

## Status
- Status: `APPROVED`
- Date: `2026-05-04`
- Owner: Product owner.
- Jira: `SPT-55`
- Supersedes: [CR-20260430-onboarding-mysql8-mvp](./CR-20260430-onboarding-mysql8-mvp.md) for Discord and notification scope only.

## Problem
The current v1 baseline keeps Discord integration as the notification delivery path. The product direction is now clearer: Discord is not part of MVP, but timely reminders and feedback alerts are required so members do not miss onboarding, weekly todo deadlines, incomplete-reason prompts, and AI feedback.

## Decision
- Remove Discord integration from MVP.
- Keep notification as a first-class in-app feature.
- Store notifications for authenticated users with recipient, read state, related resource links, idempotency key, and payload.
- Keep external delivery channels as post-MVP extension points, not implementation requirements.
- Clarify that the AI team leader operates weekly: it generates the initial curriculum, reviews weekly progress and incomplete reasons, and proposes next-week adjustments.

## Affected Feature IDs
- `identity-core`: Google OAuth and refresh-token lifecycle only for MVP.
- `notification`: replaces `discord-notifications`.
- `ai-team-leader`: explicitly includes weekly adjustment support.
- `retrospective-feedback`: next-week adjustment is part of the weekly operating loop.

## Affected Documents
- `ARCHITECTURE.md`
- `docs/index.md`
- `docs/architecture/backend-map.md`
- `docs/specs/product-brief.md`
- `docs/specs/prd-v1.md`
- `docs/specs/user-journeys-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/domain-erd.md`
- `docs/specs/db-contract-v1.md`
- `docs/specs/db-schema-v1.sql`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/notification-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/*`

## Migration Notes
- Drop `discord_integration`.
- Remove `study_group.discord_guild_id` and `study_group.discord_channel_id`.
- Remove `/api/v1/users/me/discord-link`.
- Replace `discord-notifications` feature references with `notification`.
- Change `notification.channel` MVP enum to `IN_APP`.
- Add `notification.recipient_user_id`, `title`, `body`, `delivered_at`, and `read_at`.

## Compatibility
No production data exists yet. This is a pre-implementation contract correction.
