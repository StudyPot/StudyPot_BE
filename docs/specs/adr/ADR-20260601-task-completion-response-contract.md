# ADR-20260601 Task Completion Response Contract

## Status
- Approved

## Context
- The MVP weekly todo flow already has `POST /api/v1/tasks/{taskId}/completion/me` for completing, skipping, or marking a task incomplete.
- The `task_completion` table and domain model already persist `completion_note`, `incomplete_reason`, `reason_submitted_at`, and `evidence_url`.
- The previous response body exposed only the completion record id, status, completed timestamp, and incomplete reason.
- SPT-126 needs a frontend-ready mutation response for completion buttons, incomplete reason UI, evidence URL display, and task list/progress refresh.

## Decision
- Keep the existing task completion endpoint and request shape.
- Return `id`, `taskId`, `status`, `completedAt`, `reasonSubmittedAt`, `completionNote`, `incompleteReason`, and `evidenceUrl` from `POST /api/v1/tasks/{taskId}/completion/me`.
- Keep `DONE` requests before the due date with optional `completionNote` and `evidenceUrl`; reject `incompleteReason` for `DONE`.
- Keep repeated `DONE` requests idempotent by preserving the first completion timestamp, note, and evidence URL.
- Keep `INCOMPLETE` requests limited to overdue tasks with required `incompleteReason`; reject `completionNote` and `evidenceUrl` for `INCOMPLETE`.
- Keep `SKIPPED` requests free of completion, incomplete, or evidence fields.
- Keep task completion scoped to the authenticated active member of the task's study group. Cross-group tasks remain forbidden.
- Do not add a DB migration, duplicate endpoint, realtime notification, AI retrospective trigger, or task creation/update API.

## Consequences
- Positive: Frontend clients can update task completion UI from the mutation response without another read call.
- Positive: The change is additive for existing clients that only read the older fields.
- Positive: The implementation reuses existing persisted columns and validation rules.
- Negative: Clients still need the weekly task list or progress read endpoints for aggregate progress; this task does not add a full task-with-completion list response.
- Migration or compatibility notes: No DB migration is required. JSON response consumers that ignore unknown fields remain compatible.

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

## Linked Change Request
- [CR-20260601-task-completion-response-contract](../change-requests/CR-20260601-task-completion-response-contract.md)
