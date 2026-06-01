# CR-20260601-group-member-profile-api

## Status
- Approved

## Request
- Add `GET /api/v1/groups/{groupId}/members/me/profile` for the authenticated user's own group-scoped member profile.
- Add `PATCH /api/v1/groups/{groupId}/members/me/profile` to update only the current member's group display name.
- Include current member identity, permission/status, onboarding summary, current-week summary, task-completion counts, and retrospective feedback availability in the read response.
- Keep global account profile edits outside this API.
- Keep new profile fields such as introduction, goal, memo, or profile image out of scope until a later DB/API change is approved.
- Add QA coverage for current-member read/update, invalid display name, missing group, non-member access, and LEFT member rejection.

## Reason
- Study-level my-page screens need group-specific profile state that is different from the global `/users/me` account profile.
- The locked v1 model already has `group_member.display_name`, current membership state, onboarding responses, current weeks, task completions, and retrospectives.
- Returning these summaries through one group-scoped endpoint lets the frontend render the study my-page without mixing private global identity fields with study membership state.

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

## Impact
- Product: Adds a study-specific my-page API for the logged-in member.
- API: Adds one read endpoint and one patch endpoint under the existing group resource.
- DB: No table, column, enum, constraint, or migration change. PATCH updates existing `group_member.display_name`.
- AI: None. The retrospective field exposes only feedback availability, not raw AI content.
- Notification: None.
- Permissions: Only current `PENDING_ONBOARDING` or `ACTIVE` members can read/update their own profile. Existing groups with no current membership, `LEFT` membership, deleted membership, or another user return forbidden.
- QA: Adds `QA-GRP-005` for group-scoped my profile read/update and access rejection.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session for SPT-125.
- Date: 2026-06-01
- Linked ADR: [ADR-20260601-group-member-profile-api](../adr/ADR-20260601-group-member-profile-api.md)
