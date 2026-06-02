# ADR-20260602 Ready To Start Status

## Status
- Approved

## Context
- Study groups currently move from `ONBOARDING` directly to `ACTIVE`.
- Owner onboarding submission activates the owner membership, but the group remains `ONBOARDING` until the owner starts curriculum generation.
- The UI needs a durable state for "owner onboarding is complete, study start has not been clicked yet".
- `ACTIVE` cannot represent this state because active-study surfaces depend on a generated curriculum.
- The v1 package is locked, so changing a persisted enum and state transition requires CR/ADR approval.

## Decision
- Add `READY_TO_START` to `study_group.status`.
- Keep new group creation as `ONBOARDING`.
- After onboarding submission, attempt to transition the group from `ONBOARDING` to `READY_TO_START` only when the submitting member is the active owner.
- Let invited members continue joining and submitting onboarding while the group is `READY_TO_START`.
- Require `READY_TO_START` for `POST /api/v1/groups/{groupId}/start`.
- Persist successful host start as `READY_TO_START -> ACTIVE` with `started_at`.
- Apply the DB check-constraint change only in `V4__study_group_ready_to_start_status.sql`; keep the already-applied V1 baseline migration unchanged.
- Keep current active-only permissions and queries limited to `ACTIVE`.
- Do not require every invitee to complete onboarding before host start.

## Consequences
- Positive: The frontend can display an explicit pre-start ready state instead of overloading `ONBOARDING`.
- Positive: Active-study permissions stay tied to generated curriculum availability.
- Positive: The existing partial-onboarding host-start policy remains intact.
- Negative: Existing databases need the `study_group.status` check constraint updated before `READY_TO_START` can be saved.
- Negative: Clients that hard-code the old enum list need to accept the new value.
- Migration or compatibility notes: No data backfill is required. Existing `ONBOARDING` rows remain valid and become `READY_TO_START` when owner onboarding is submitted after deployment.

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

## Linked Change Request
- [CR-20260602-ready-to-start-status](../change-requests/CR-20260602-ready-to-start-status.md)
