# AI Study Leader User Journeys v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Retrospective/chat context boundary is authorized by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md) and [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md).
- Fixed one-week sprint windows are authorized by [CR-20260601-fixed-weekly-sprint-windows](./change-requests/CR-20260601-fixed-weekly-sprint-windows.md) and [ADR-20260601-fixed-weekly-sprint-windows](./adr/ADR-20260601-fixed-weekly-sprint-windows.md).
- Ready-to-start group status is authorized by [CR-20260602-ready-to-start-status](./change-requests/CR-20260602-ready-to-start-status.md) and [ADR-20260602-ready-to-start-status](./adr/ADR-20260602-ready-to-start-status.md).

## Journey 1: Host Creates Group
1. Authenticated host submits group name, topic, selected/detail keywords, maximum members, and study period.
2. Backend creates `study_group` with `status = ONBOARDING`.
3. Backend creates owner `group_member` with `status = PENDING_ONBOARDING`.
4. Backend returns group summary and invite link/code.
5. Backend creates in-app onboarding notification targets for the host and later invitees.

Acceptance:
- `study_group.detail_keywords` stores only final selected/direct keywords.
- `study_group.onboarding_started_at` is set.
- Host must still submit onboarding before becoming `ACTIVE`.

## Journey 2: Member Joins by Invite
1. Authenticated user opens invite link.
2. Backend validates group status and max member count.
3. Backend creates `group_member` with `status = PENDING_ONBOARDING`.
4. Backend returns onboarding form metadata based on group detail keywords.
5. Backend creates an in-app notification asking the member to complete onboarding.

Acceptance:
- Duplicate active membership is rejected.
- Joined member cannot access weekly todos until onboarding is submitted or host starts current-week assignment policy allows it.

## Journey 3: Member Submits Onboarding
1. Member enters one overall skill level for the study topic.
2. Member enters optional note.
3. Member enters recurring availability slots.
4. Backend stores `group_onboarding_response` as `SUBMITTED` and creates `member_availability_slot` rows.
5. Backend changes `group_member.status` to `ACTIVE` if the group is already active or ready to activate.
6. If the submitting member is the active owner and the group is still `ONBOARDING`, backend changes `study_group.status` to `READY_TO_START`.

Acceptance:
- Skill level is 1 to 5.
- Availability uses `day_of_week`, `start_time`, `end_time`, and `timezone`.
- Each member has at most one active onboarding response per group.

## Journey 4: Host Starts Study
1. Host clicks start.
2. Backend verifies host is owner and group is `READY_TO_START`.
3. Backend summarizes submitted onboarding responses.
4. Backend derives fixed one-week sprint windows from the group study period.
5. Backend creates curriculum, weeks, and weekly tasks that match the sprint windows.
6. Backend sets `study_group.status = ACTIVE` and `started_at`.
7. Backend creates in-app notifications for study start and the first weekly tasks.

Acceptance:
- Start does not require every invitee to finish onboarding.
- Initial curriculum uses only responses submitted at start time.
- Initial curriculum has exactly one week per fixed one-week sprint window.
- Late joiner onboarding is reflected in future feedback/adjustments, not automatic full regeneration.

## Journey 5: Weekly Todo Execution
1. Member sees current week and tasks.
2. Member completes task or leaves it open.
3. Before deadline, member can click complete todo.
4. After deadline, incomplete tasks require an incomplete reason modal.
5. Backend stores task and week progress.
6. Backend creates in-app notifications for due-soon, overdue, and incomplete-reason-required states.

Acceptance:
- `task_completion.status` is one of `TODO`, `DONE`, `INCOMPLETE`, `SKIPPED`.
- Incomplete reason is required for overdue incomplete status.
- Completion timestamps and notes are retained.

## Journey 6: Retrospective and AI Team Leader Chat
1. Week ends or user opens AI team leader conversation.
2. Backend creates retrospective context from onboarding summary, current week, tasks, progress, completion notes, incomplete reasons, relevant rules/violations, prior feedback/adjustment, and conversation summary.
3. AI produces feedback and next-week adjustment proposal for the next weekly operating loop.
4. Chat messages are stored for the member and group.
5. Backend creates an in-app notification when AI feedback or next-week adjustment is ready.

Acceptance:
- Feedback is linked to `member_week_progress`, `curriculum_week`, and `group_member`.
- AI responses are backed by `llm_usage`.
- Context building is DB-first for MVP; vector/graph retrieval is deferred until unstructured study materials exist.
- Next-week adjustment can affect future weekly task recommendations.

## Journey 7: In-App Notification
1. Member opens the notification list.
2. Backend returns notifications for the authenticated user, ordered by newest first.
3. Member marks one or more notifications as read.
4. Backend stores `read_at` and returns updated unread state.

Acceptance:
- Notification uses `channel = IN_APP` for MVP.
- Members can only read or update their own notification rows.
- Discord, push, and email delivery are post-MVP extensions.
