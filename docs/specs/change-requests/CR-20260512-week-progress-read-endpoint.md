# CR-20260512-week-progress-read-endpoint

## Status
- Approved

## Request
- Add `GET /api/v1/weeks/{weekId}/progress/me` to the `weekly-todo` API.
- Return the authenticated active group member's existing `member_week_progress` as `MemberWeekProgressResponse`.
- Keep `PUT /api/v1/weeks/{weekId}/progress/me` as the create/update endpoint.

## Reason
- The locked v1 API exposes `PUT /api/v1/weeks/{weekId}/progress/me`, which returns progress only as a mutation response.
- The weekly execution completion goal requires members to read their own week progress independently from mutation.
- `GET /api/v1/groups/{groupId}/weeks/current` currently returns week metadata and does not expose `member_week_progress`.

## Affected Feature IDs
- `weekly-todo`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/confluence/05-api-spec.md`

## Impact
- Product: Members can inspect their own progress state without changing it.
- API: Adds one authenticated group-member GET endpoint under the existing progress resource path.
- DB: No table, column, enum, or constraint changes.
- AI: None.
- Notification: None.
- Permissions: Same weekly-todo read boundary as weekly tasks: only active group members can read their own progress.
- QA: Adds read-progress happy path, missing progress, and inactive/non-member access coverage.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner request in Codex session, selecting option 2 for separate progress lookup.
- Date: 2026-05-12
- Linked ADR: [ADR-20260512-week-progress-read-endpoint](../adr/ADR-20260512-week-progress-read-endpoint.md)
