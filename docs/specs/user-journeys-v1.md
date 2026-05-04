# AI Study Leader User Journeys v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.

## Journey 1: Host Creates Group
1. Authenticated host submits group name, topic, selected/detail keywords, maximum members, and study period.
2. Backend creates `study_group` with `status = ONBOARDING`.
3. Backend creates owner `group_member` with `status = PENDING_ONBOARDING`.
4. Backend returns group summary and invite link/code.

Acceptance:
- `study_group.detail_keywords` stores only final selected/direct keywords.
- `study_group.onboarding_started_at` is set.
- Host must still submit onboarding before becoming `ACTIVE`.

## Journey 2: Member Joins by Invite
1. Authenticated user opens invite link.
2. Backend validates group status and max member count.
3. Backend creates `group_member` with `status = PENDING_ONBOARDING`.
4. Backend returns onboarding form metadata based on group detail keywords.

Acceptance:
- Duplicate active membership is rejected.
- Joined member cannot access weekly todos until onboarding is submitted or host starts current-week assignment policy allows it.

## Journey 3: Member Submits Onboarding
1. Member enters detail-keyword skill levels.
2. Member enters task preference scores.
3. Member enters optional note.
4. Member enters recurring availability slots.
5. Backend stores `group_onboarding_response` as `SUBMITTED` and creates `member_availability_slot` rows.
6. Backend changes `group_member.status` to `ACTIVE` if the group is already active or ready to activate.

Acceptance:
- Skill and preference values are 1 to 5.
- Availability uses `day_of_week`, `start_time`, `end_time`, and `timezone`.
- Each member has at most one active onboarding response per group.

## Journey 4: Host Starts Study
1. Host clicks start.
2. Backend verifies host is owner and group is `ONBOARDING`.
3. Backend summarizes submitted onboarding responses.
4. Backend creates curriculum, weeks, and weekly tasks.
5. Backend sets `study_group.status = ACTIVE` and `started_at`.

Acceptance:
- Start does not require every invitee to finish onboarding.
- Initial curriculum uses only responses submitted at start time.
- Late joiner onboarding is reflected in future feedback/adjustments, not automatic full regeneration.

## Journey 5: Weekly Todo Execution
1. Member sees current week and tasks.
2. Member completes task or leaves it open.
3. Before deadline, member can click complete todo.
4. After deadline, incomplete tasks require an incomplete reason modal.
5. Backend stores task and week progress.

Acceptance:
- `task_completion.status` is one of `TODO`, `DONE`, `INCOMPLETE`, `SKIPPED`.
- Incomplete reason is required for overdue incomplete status.
- Completion timestamps and notes are retained.

## Journey 6: Retrospective and AI Team Leader Chat
1. Week ends or user opens AI team leader conversation.
2. Backend creates retrospective context from onboarding, tasks, completion notes, and incomplete reasons.
3. AI produces feedback and next-week adjustment proposal.
4. Chat messages are stored for the member and group.

Acceptance:
- Feedback is linked to `member_week_progress`, `curriculum_week`, and `group_member`.
- AI responses are backed by `llm_usage`.
- Next-week adjustment can affect future weekly task recommendations.
