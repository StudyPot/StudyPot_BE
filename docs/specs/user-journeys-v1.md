# User Journeys v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Change rule: changes require `docs/specs/change-control-v1.md`.

## Journey 1: First Login And Discord Link
1. User opens the web client and chooses Google login.
2. Client completes Google OAuth against backend auth endpoints.
3. Backend creates or updates `users` and `user_oauth_accounts`.
4. User links a Discord account.
5. Backend stores `user_discord_accounts` without placing Discord IDs on core study tables.
6. User can view their current profile and linked Discord state.

Success state:
- `users.status = active`
- at least one active `user_oauth_accounts` row
- optional active `user_discord_accounts` row

Failure handling:
- OAuth failure returns `401` or `409` with `application/problem+json`.
- Discord account already linked to another active user returns `409`.

## Journey 2: Create Study Group
1. Authenticated user creates a group with name, timezone, rules, and schedule defaults.
2. Backend creates `study_groups`.
3. Backend creates an owner `study_group_members` row for the creator.
4. Group owner can update rules and schedule defaults.

Success state:
- Group has one active owner.
- `study_groups.rules` and `study_groups.schedule_defaults` match the JSONB contract in `domain-erd.md`.

Failure handling:
- Duplicate active slug returns `409`.
- Invalid rules payload returns `422`.

## Journey 3: Invite Members
1. Owner or manager creates an invitation.
2. Backend stores `study_group_invitations` with hashed token and target role.
3. Invitee accepts the invitation.
4. Backend creates or activates the user's membership.

Success state:
- Invitation status becomes `accepted`.
- Accepted user has `study_group_members.status = active`.

Failure handling:
- Expired invite returns `410`.
- Revoked invite returns `409`.
- Non-owner/non-manager invite creation returns `403`.

## Journey 4: Connect Discord Notification Channel
1. Owner or manager selects a Discord guild/channel in the client.
2. Backend validates the channel binding and stores `discord_notification_channels`.
3. Notification settings are stored in `notification_settings JSONB`.
4. Future notifications write rows to `discord_notification_logs`.

Success state:
- Channel status is `active`.
- Notification types are limited to v1 supported values.

Failure handling:
- Missing Discord authorization returns `401`.
- Channel already connected to same group returns `409`.
- Revoked bot access marks the channel `revoked` and records failed logs.

## Journey 5: Schedule And Prepare Session
1. Owner or manager creates a study session.
2. Backend assigns group-local `sequence_no`.
3. Backend stores agenda and meeting snapshot.
4. A preparation brief can be generated for the session.
5. AI run is recorded in `ai_prompt_runs`; result is stored in `ai_preparation_briefs`.
6. Discord can send a `prep_brief` notification.

Success state:
- Session status moves from `scheduled` to `ready` when prep material is available.
- AI output follows `PreparationBriefV1` schema.

Failure handling:
- AI failure stores failed `ai_prompt_runs` with redacted error data.
- Session remains usable without AI output.

## Journey 6: Submit Attendance And Notes
1. Members submit or update attendance.
2. Members submit structured pre-note or post-note payloads.
3. Notes can produce action items manually or through AI extraction.

Success state:
- One active attendance row per session/member.
- Notes have `structured_payload`.
- Action items point to session and optional source note.

Failure handling:
- Non-member access returns `403`.
- Note submitted for archived/cancelled session returns `409`.
- Invalid note payload returns `422`.

## Journey 7: Generate Feedback
1. Owner or manager requests feedback after a session.
2. Backend gathers redacted session context.
3. AI run is stored in `ai_prompt_runs`.
4. Group feedback is stored in `ai_feedback_reports` with `target_member_id = null`.
5. Individual feedback is stored with `target_member_id` set.
6. Discord can send `feedback_ready` notification.

Success state:
- Feedback output follows `FeedbackReportV1` schema.
- Generated report version increments per session and target.

Failure handling:
- Missing required notes returns `409` with clear problem detail.
- AI provider failure records failed prompt run and returns retryable problem response.

## Journey 8: Track Progress And Follow-Up
1. Members complete action items or add progress logs.
2. Managers update goals and resources.
3. Future preparation briefs and feedback reports use prior history.

Success state:
- Progress logs remain attached to member, optional goal, and optional session.
- Action item status changes are auditable through `updated_at` and `completed_at`.

Failure handling:
- Completing another member's private action item without permission returns `403`.
- Invalid status transition returns `409`.
