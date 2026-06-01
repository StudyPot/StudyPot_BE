# CR-20260601-task-completion-response-contract

## Status
- Approved

## Request
- Add frontend display fields to the existing `POST /api/v1/tasks/{taskId}/completion/me` response:
  - `taskId`
  - `reasonSubmittedAt`
  - `completionNote`
  - `evidenceUrl`
- Keep the existing request fields `status`, `completionNote`, `incompleteReason`, and `evidenceUrl`.
- Keep the existing endpoint and do not add a duplicate task completion API.
- Document the `DONE`, `INCOMPLETE`, and `SKIPPED` input policies and the repeated `DONE` idempotency policy.
- Add QA coverage for done, incomplete, skipped, pending-member rejection, cross-group rejection, and frontend response fields.

## Reason
- The locked v1 API stores task completion note, evidence URL, incomplete reason, completion timestamp, and reason-submission timestamp, but the public response exposed only part of that state.
- SPT-126 requires the frontend completion button and task list/progress views to render the latest task completion state without a second API call.
- The existing DB and service model already persist the required fields, so an additive response contract is enough.

## Affected Feature IDs
- `weekly-todo`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/09-qa-acceptance.md`
- `docs/confluence/10-jira-mapping.md`

## Impact
- Product: The frontend can render completion note, evidence URL, incomplete reason, and reason-submission time from the mutation response.
- API: Adds optional response fields to the existing task completion response and documents state-specific validation behavior.
- DB: No table, column, enum, constraint, or migration change.
- AI: None.
- Notification: None.
- Permissions: Keeps the existing active group member boundary and explicitly verifies cross-group task rejection.
- QA: Adds frontend response-field and cross-group rejection acceptance coverage for `weekly-todo`.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session for SPT-126.
- Date: 2026-06-01
- Linked ADR: [ADR-20260601-task-completion-response-contract](../adr/ADR-20260601-task-completion-response-contract.md)
