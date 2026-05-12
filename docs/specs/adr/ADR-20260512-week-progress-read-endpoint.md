# ADR-20260512 Week Progress Read Endpoint

## Status
- Approved

## Context
- The locked API contract includes `PUT /api/v1/weeks/{weekId}/progress/me` for progress create/update.
- The same path does not currently expose a `GET` operation.
- The current week endpoint returns week metadata only, so it cannot satisfy an independent member progress lookup.
- Adding an endpoint changes the locked API shape and therefore requires a Change Request and ADR.

## Decision
- Add `GET /api/v1/weeks/{weekId}/progress/me`.
- The endpoint returns the authenticated active group member's own existing `member_week_progress` using `MemberWeekProgressResponse`.
- The endpoint is read-only. It must not create progress when no row exists.
- If the target week exists but the member has no progress row yet, return the existing not-found problem response path.
- Reuse the same membership and cross-group checks as the weekly-todo read/update flow.
- Keep `PUT /api/v1/weeks/{weekId}/progress/me` as the only create/update operation.

## Consequences
- Positive: Swagger and clients can perform progress 조회 separately from progress 갱신.
- Positive: No DB migration is required because the endpoint reads the existing `member_week_progress` table.
- Positive: The existing PUT mutation contract remains backward compatible.
- Negative: Clients must handle not found when progress has not been created yet.
- Migration or compatibility notes: No migration required. Existing clients continue to use PUT unchanged.

## Affected Feature IDs
- `weekly-todo`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/confluence/05-api-spec.md`

## Linked Change Request
- [CR-20260512-week-progress-read-endpoint](../change-requests/CR-20260512-week-progress-read-endpoint.md)
