# ADR-20260601 Group Member Profile API

## Status
- Approved

## Context
- StudyPot already has a global authenticated user profile at `/api/v1/users/me`.
- Study-specific my-page screens need membership-level state such as group display name, permission, onboarding submission, current week, task completion summary, and retrospective feedback readiness.
- The existing database already stores the only editable MVP study-profile field, `group_member.display_name`.
- Adding introduction, goal, memo, image, or other profile fields would require a broader DB/API/product change that is not necessary for SPT-125.

## Decision
- Add `GET /api/v1/groups/{groupId}/members/me/profile`.
- Add `PATCH /api/v1/groups/{groupId}/members/me/profile`.
- Limit PATCH to `displayName` with 1 to 80 non-blank characters.
- Return member identity, group id, user id, display name, permission, and status.
- Return onboarding summary, current week summary, current-week task completion counts, and whether a completed retrospective feedback record exists.
- Allow current `PENDING_ONBOARDING` and `ACTIVE` members to read/update their own profile.
- Return not found for a missing group.
- Return forbidden for existing groups when the authenticated user has no current membership, has `LEFT`, has a deleted membership, or attempts to access another user's membership.
- Do not add a DB migration, global account edit behavior, notification behavior, AI generation trigger, or profile fields beyond `displayName`.

## Consequences
- Positive: Frontend clients get a single study my-page payload without combining unrelated global account APIs.
- Positive: The write path stays narrow and reuses the locked `group_member.display_name` column.
- Positive: Current-membership permission is explicit and testable.
- Negative: Product fields such as self-introduction, profile image, and personal study goal remain deferred.
- Migration or compatibility notes: No DB migration is required. Existing clients are unaffected because the endpoints are additive.

## Affected Feature IDs
- `study-group-core`
- `group-onboarding`
- `weekly-todo`
- `retrospective-feedback`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/07-permissions-state.md`
- `docs/confluence/09-qa-acceptance.md`
- `docs/confluence/10-jira-mapping.md`

## Linked Change Request
- [CR-20260601-group-member-profile-api](../change-requests/CR-20260601-group-member-profile-api.md)
