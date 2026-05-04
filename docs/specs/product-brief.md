# AI Study Leader Product Brief

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Change record: [CR-20260430-onboarding-mysql8-mvp](./change-requests/CR-20260430-onboarding-mysql8-mvp.md)

## Product Summary
AI Study Leader helps a host create a study group, collect member onboarding context, generate an AI curriculum, track weekly todos, and run lightweight retrospective feedback through an AI team leader.

The product replaces the manual study leader role for small groups. The leader does not need to schedule heavy synchronous sessions for MVP; instead, the system captures member skill, task preference, availability, progress, incomplete reasons, and AI feedback.

## Target Users
- Study hosts who want to create a group and start a structured curriculum quickly.
- Study members who need weekly tasks and feedback without a human manager chasing progress.
- Small coding-study or project-study groups that use invite links and asynchronous communication.

## MVP Value
- Fast group creation with topic, detail keywords, max members, and period.
- Invite link flow for bringing members into the group.
- Member-specific onboarding for skill, task preference, availability, and notes.
- Host start action that turns submitted onboarding responses into an AI curriculum.
- Weekly todo completion and incomplete-reason capture.
- AI team leader retrospective feedback and next-week adjustment.
- Discord notification hooks for reminders and status events.

## MVP Golden Path
1. Host creates a group.
2. System stores selected or directly entered detail keywords.
3. System creates host membership in `PENDING_ONBOARDING`.
4. Host shares invite link.
5. Host and members submit onboarding responses.
6. Host starts the study without requiring every invitee to finish onboarding.
7. System creates curriculum and weekly tasks from submitted onboarding summaries.
8. Members complete weekly todos or submit incomplete reasons.
9. AI team leader produces feedback and next-week adjustment.
10. Late joiners complete onboarding and join from the current week.

## Explicitly Deferred
- Heavy live meeting automation.
- Voice transcription.
- Full synchronous meeting assistant.
- Rule version history.
- Rich frontend wireframe implementation.
- Automatic full curriculum regeneration for late joiners.

## Technical Baseline
- Backend: Java 21, Gradle, Spring Boot.
- API: REST `/api/v1`, OpenAPI 3.1.
- DB: MySQL8, UUIDv7 as `BINARY(16)`, flexible structured fields as `JSON`.
- Source control workflow: Jira `SPT` task -> `codex/<slug>` worktree -> PR to `develop`.
