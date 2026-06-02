# CR-20260602-ready-to-start-status

## Status
- Approved

## Request
- Add `READY_TO_START` to `study_group.status`.
- Transition an `ONBOARDING` group to `READY_TO_START` after the owner submits onboarding and becomes an active member.
- Require `READY_TO_START` rather than `ONBOARDING` when the owner starts the study and generates the initial curriculum.
- Keep the existing policy that the owner can start before every invitee completes onboarding.
- Keep `READY_TO_START` out of active-study permissions such as boards, weekly todos, retrospectives, AI chat, group notification logs, and LLM usage logs.

## Reason
- The locked v1 state model moves directly from `ONBOARDING` to `ACTIVE`, so the frontend cannot distinguish a group where owner onboarding is complete but the owner has not clicked start yet.
- The product needs an explicit pre-start state for user-facing group progress and start-button readiness.
- Reusing `ACTIVE` for this gap would incorrectly unlock active-study surfaces before curriculum generation.

## Affected Feature IDs
- `study-group-core`
- `group-onboarding`
- `curriculum-core`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/prd-v1.md`
- `docs/specs/user-journeys-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/domain-erd.md`
- `docs/specs/db-contract-v1.md`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/01-mvp-golden-path.md`
- `docs/confluence/02-requirements.md`
- `docs/confluence/04-erd-data-model.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/07-permissions-state.md`
- `docs/confluence/09-qa-acceptance.md`
- `docs/confluence/10-jira-mapping.md`

## Impact
- Product: Adds an explicit owner-onboarding-complete, pre-start group state.
- API: Extends the `StudyGroupStatus` enum with `READY_TO_START`; no endpoint, path, request field, or response field is added or removed.
- DB: Extends the `study_group.status` check constraint with `READY_TO_START` through `V4__study_group_ready_to_start_status.sql`; no table or column is added.
- AI: Curriculum generation starts from `READY_TO_START`; failed generation leaves the group in `READY_TO_START`.
- Notification: No notification type or delivery behavior change.
- Permissions: Current-member group reads and onboarding/join flows can see/use `READY_TO_START`; active-only feature permissions remain `ACTIVE` only.
- QA: Adds coverage for owner onboarding readiness, start-state rejection, partial invitee onboarding, and enum contract updates.

## Compatibility
- Backward compatible: yes
- Migration required: yes, for the DB check constraint before persisting `READY_TO_START`.
- Baseline migration compatibility: do not rewrite already-applied `V1__erd_v0_8_mysql8_schema.sql`; keep the V4 migration as the single executable constraint change.

## Decision
- Approved by: Product owner request in Codex session for SPT-128.
- Date: 2026-06-02
- Linked ADR: [ADR-20260602-ready-to-start-status](../adr/ADR-20260602-ready-to-start-status.md)
