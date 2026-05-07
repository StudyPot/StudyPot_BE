# ADR-20260504 Remove Discord From MVP And Keep In-App Notification

## Status
- Status: `ACCEPTED`
- Date: `2026-05-04`
- Related CR: [CR-20260504-no-discord-inapp-notification](../change-requests/CR-20260504-no-discord-inapp-notification.md)
- Jira: `SPT-55`

## Context
The onboarding MVP needs alerts for onboarding requests, weekly deadlines, incomplete-reason prompts, retrospective readiness, and AI feedback. The previous v1 docs made Discord the default delivery mechanism, which adds OAuth, bot, channel, token, and failure-handling complexity before the core product loop has been built.

The product decision is to focus MVP on the application experience: users see notifications inside the service, and external delivery can be added later when the core study loop is proven.

## Decision
- Remove Discord-specific DB tables, group columns, API endpoints, Jira implementation scope, and documentation from active MVP contracts.
- Keep a `notification` table and API as an in-app notification contract.
- Make notification recipient/read state explicit.
- Rename `discord-notifications` to `notification`.
- Keep AI team leader as a recurring operator that reviews weekly outcomes and produces next-week adjustments.

## Consequences
- Positive: MVP implementation avoids Discord setup, OAuth scope, bot permissions, and token storage before the core workflow exists.
- Positive: Product reminders remain available through in-app notification.
- Positive: The AI team leader role is clearer: it manages the weekly planning loop, not only the initial curriculum.
- Tradeoff: Users will not receive Discord messages in MVP.
- Tradeoff: Push/email/Discord delivery will require a later CR/ADR and additional delivery-channel tables or preferences.

## Implementation Contract
- MVP notification channel enum: `IN_APP`.
- MVP notification statuses: `PENDING`, `DELIVERED`, `READ`, `FAILED`, `SKIPPED`.
- Notification rows include `recipient_user_id`, `title`, `body`, `payload`, `scheduled_at`, `delivered_at`, and `read_at`.
- AI retrospective output includes `next_week_adjustment` and may use late joiner onboarding only for future week adjustment.
