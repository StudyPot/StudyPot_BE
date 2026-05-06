# 07 권한 / 상태 전이

## Status Values
- `study_group.status`: `DRAFT`, `ONBOARDING`, `ACTIVE`, `COMPLETED`, `ARCHIVED`
- `group_member.status`: `PENDING_ONBOARDING`, `ACTIVE`, `LEFT`
- `task_completion.status`: `TODO`, `DONE`, `INCOMPLETE`, `SKIPPED`
- `retrospective.status`: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`

## Permission Summary
- Google OAuth login and refresh token rotation are public auth endpoints.
- Current-session logout and logout-all require bearer authentication.
- Owner creates/updates group and starts study.
- Pending member can submit onboarding.
- Active member can view curriculum, complete todos, request retrospective, and chat with AI.
- Members can read and mark their own in-app notifications.
- Owner can read group notification and LLM usage logs.

## Source
- `docs/specs/auth-permissions-v1.md`
