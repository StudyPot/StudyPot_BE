# ADR-20260601 Study Group Board API

## Status
- Approved

## Context
- Existing StudyPot collaboration surfaces are task/progress APIs, retrospective feedback, notification logs, and 1:1 AI team leader conversation.
- Groups need a public-within-group board surface for notices, questions, resources, and retrospectives.
- Board content should not be stored in Redis, SSE stream buffers, or AI conversation tables because it is durable group content, not realtime delivery state or private AI context.
- The v1 contract is locked, so adding tables, endpoints, and permission actions requires CR/ADR approval.

## Decision
- Add `study-group-board` as a feature ID.
- Add `group_board`, `group_board_post`, and `group_board_comment` tables.
- Create default board rows lazily when an active member calls `GET /api/v1/groups/{groupId}/boards`.
- Expose post list/create/read/update/delete and comment list/create/update/delete APIs.
- Use cursor pagination for post and comment lists.
- Require active group membership for every board read/write API.
- Allow authors to update/delete their own posts and comments.
- Allow OWNER users to delete posts/comments and change post pinned state.
- Do not allow a non-author OWNER to rewrite another member's title/content.
- Return forbidden for cross-group resource access when the resource exists in another group.
- Defer upload, mentions, reactions, search, tags, realtime board events, board notifications, and AI summaries.

## Consequences
- Positive: Groups gain a durable shared communication layer without changing private AI conversation boundaries.
- Positive: The access model stays explicit and testable: active membership, author ownership, and OWNER moderation.
- Positive: Board state remains recoverable from MySQL and can later feed notification/realtime features through separate approved changes.
- Negative: Default boards are currently fixed and custom board management is deferred.
- Negative: Board content is not yet indexed for search or wired to notifications.
- Migration or compatibility notes: Additive Flyway migration creates new tables only; existing clients are unaffected.

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

## Linked Change Request
- [CR-20260601-study-group-board-api](../change-requests/CR-20260601-study-group-board-api.md)
