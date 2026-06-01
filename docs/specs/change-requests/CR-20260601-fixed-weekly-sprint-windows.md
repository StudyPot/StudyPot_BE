# CR-20260601-fixed-weekly-sprint-windows

## Status
- Approved

## Request
- Derive curriculum sprint windows from `study_group.starts_at` and `study_group.ends_at` when the host starts a study.
- Use a fixed one-week sprint duration for the current MVP implementation.
- Require curriculum generation to return exactly one generated week per planned sprint window.
- Persist `curriculum_week.starts_at`, `curriculum_week.ends_at`, and weekly task `due_at` from the planned sprint windows.
- Defer user-selectable sprint duration to a later Change Request and ADR.

## Reason
- The locked v1 model already stores study periods and curriculum week windows, but implementation could still create week windows from the host-start timestamp and whatever week count the AI returned.
- A three-month study needs deterministic weekly sprint boundaries before AI content is accepted so users can see reliable week numbers and due dates.
- Fixed one-week sprinting matches the current product decision while avoiding premature UI/API/DB configuration scope.

## Affected Feature IDs
- `curriculum-core`
- `weekly-todo`
- `ai-team-leader`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/user-journeys-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/db-contract-v1.md`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/02-requirements.md`
- `docs/confluence/04-erd-data-model.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/09-qa-acceptance.md`

## Impact
- Product: Study periods are split into fixed one-week sprint windows at host start; configurable sprint duration remains deferred.
- API: No endpoint, path, request field, response field, or enum change.
- DB: No table, column, index, enum, or migration change.
- AI: Curriculum generation input and validation now include expected fixed weekly sprint windows.
- Notification: No change.
- Permissions: No change.
- QA: Adds fixed weekly sprint-window acceptance to curriculum start verification.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session for SPT-124.
- Date: 2026-06-01
- Linked ADR: [ADR-20260601-fixed-weekly-sprint-windows](../adr/ADR-20260601-fixed-weekly-sprint-windows.md)
