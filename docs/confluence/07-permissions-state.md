# 07 권한 / 상태 전이

## Status Values
- `study_group.status`: `DRAFT`, `ONBOARDING`, `ACTIVE`, `COMPLETED`, `ARCHIVED`
- `group_member.status`: `PENDING_ONBOARDING`, `ACTIVE`, `LEFT`
- `group_board_post.status`: `PUBLISHED`, `DELETED`
- `group_board_comment.status`: `PUBLISHED`, `DELETED`
- `task_completion.status`: `TODO`, `DONE`, `INCOMPLETE`, `SKIPPED`
- `retrospective.status`: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`

## Permission Summary
- Google OAuth login and refresh token rotation are public auth endpoints.
- Current-session logout and logout-all require bearer authentication.
- Owner creates/updates group and starts study.
- Pending member can submit onboarding.
- Pending member can read/update their own group-scoped profile display name.
- Active member can view curriculum, complete todos, request retrospective, and chat with AI.
- Active member can read group boards/posts/comments and create posts/comments in their own group.
- Board authors can edit/delete their own posts/comments.
- Owner can delete any board post/comment and pin/unpin posts, but cannot rewrite another member's content unless they are also the author.
- Current group members can read/update only their own group member profile; `LEFT` members are not current members.
- Active conversation members can list and stream only their own AI conversation messages.
- Members can read and mark their own in-app notifications.
- Owner can read group notification and LLM usage logs.

## Source
- `docs/specs/auth-permissions-v1.md`
