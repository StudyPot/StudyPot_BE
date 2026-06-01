# CR-20260601-study-group-board-api

## Status
- Approved

## Request
- Add durable group-scoped board, post, and comment APIs under `/api/v1/groups/{groupId}`.
- Add default group boards: `NOTICE`, `QUESTION`, `RESOURCE`, and `RETROSPECTIVE`.
- Create MySQL tables for `group_board`, `group_board_post`, and `group_board_comment`.
- Allow only active group members to read and write board content.
- Allow authors to edit/delete their own posts and comments.
- Allow group OWNER users to delete posts/comments and change post pinned state.
- Keep upload, mentions, reactions, search, tags, realtime board events, board notifications, and AI summaries out of scope.

## Reason
- Study groups need shared asynchronous communication surfaces beyond 1:1 AI conversation and personal task progress.
- The API must preserve private/member-scoped AI and progress data boundaries while enabling public-within-group board context.
- Storing posts and comments in MySQL keeps board state durable and auditable without coupling it to SSE or notification delivery.

## Affected Feature IDs
- `study-group-board`
- `study-group-core`

## Affected Documents
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/db-contract-v1.md`
- `docs/specs/db-schema-v1.sql`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/04-erd-data-model.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/07-permissions-state.md`
- `docs/confluence/09-qa-acceptance.md`
- `docs/confluence/10-jira-mapping.md`

## Impact
- Product: Adds group-visible boards for notices, questions, resources, and retrospectives.
- API: Adds board list, post CRUD, comment list/create/update/delete endpoints.
- DB: Adds three MySQL tables and their indexes/FKs through a new Flyway migration.
- AI: None. Board content is not added to AI context in this change.
- Notification: None. Board-created notifications are deferred.
- Permissions: Active group membership is required for board access. Author and OWNER write boundaries are enforced.
- QA: Adds `QA-BOARD-001` to `QA-BOARD-006`.

## Compatibility
- Backward compatible: yes
- Migration required: yes, additive Flyway migration for group board tables.

## Decision
- Approved by: Product owner direction in Codex session for SPT-123.
- Date: 2026-06-01
- Linked ADR: [ADR-20260601-study-group-board-api](../adr/ADR-20260601-study-group-board-api.md)
