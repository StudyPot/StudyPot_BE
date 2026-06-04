# EXEC_PLAN: [fix] 그룹홈 학습활동 및 TODO 상태 API 보강

- Task slug: `spt-131-learning-activity-todo-actions`
- Base branch: `develop`
- Feature branch: `codex/spt-131-learning-activity-todo-actions`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-131-learning-activity-todo-actions`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-131-learning-activity-todo-actions`
- Jira issue: `SPT-131`
- Jira URL: https://studypot.atlassian.net/browse/SPT-131
- Jira summary: [study-group/weekly-todo] 그룹홈 학습활동 및 TODO 상태 API 보강
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/09-qa-acceptance.md
- [x] docs/exec-plans/active/20260601-spt-126-task-completion-contract.md
- [x] docs/exec-plans/active/20260601-spt-125-group-member-profile.md

## Related Feature IDs
- [x] study-group-core
- [x] weekly-todo

## Doc Notes
- `AGENTS.md`, `ARCHITECTURE.md`, `docs/index.md`: SPT Jira Task, dedicated `codex/<slug>` worktree, `EXEC_PLAN`, tests, `./gradlew check build --no-daemon`, PR/review-gate/finish flow are mandatory.
- `docs/specs/api-contract-v1.md`: locked v1 already has separate current-week, weekly-task list, week progress, and generic task completion APIs; group member profile only returns a summary and no task list.
- `docs/specs/openapi.yaml`: `TaskCompletionStatus` is `TODO|DONE|INCOMPLETE|SKIPPED`; existing `POST /tasks/{taskId}/completion/me` remains the canonical generic mutation.
- `docs/specs/qa-acceptance-v1.md`: current week task completion and rendering for done/incomplete/skipped/pending-member/cross-group are required scenarios.
- `docs/confluence/05-api-spec.md`, `docs/confluence/09-qa-acceptance.md`: mirror the repo API and QA contracts for frontend-facing reference.
- `SPT-125` plan: group-scoped profile contains current-week and completion summary, but not the current learning activity task list.
- `SPT-126` plan: task completion contract exists, so this task should add frontend-friendly additive wrappers instead of replacing the existing completion endpoint.

## Goal
Close the two frontend integration gaps without breaking locked v1 contracts:

1. Add a group-scoped current learning activity API for group home that returns the current week, my progress status, task list, and my task completion statuses in one response.
2. Add explicit TODO action endpoints for done, incomplete, and skip that delegate to the existing task completion state machine and return the same completion result shape.

## Approach
- Keep existing `GET /groups/{groupId}/weeks/current`, `GET /weeks/{weekId}/tasks`, `GET/PUT /weeks/{weekId}/progress/me`, and `POST /tasks/{taskId}/completion/me` behavior unchanged.
- Add a read model in the curriculum slice so the group-home endpoint can assemble current week + progress + task completions without creating missing progress rows.
- Add completion wrapper endpoints under the existing task completion resource, with request bodies that match the button action:
  - `POST /api/v1/tasks/{taskId}/completion/me/done`
  - `POST /api/v1/tasks/{taskId}/completion/me/incomplete`
  - `POST /api/v1/tasks/{taskId}/completion/me/skip`
- Reuse `CurriculumService.completeMyTask` for all mutations so permission, due-date, idempotency, and transition rules remain centralized.
- Update API/OpenAPI/Confluence/QA docs only for additive endpoints and schemas.

## Step Plan
1. Add focused controller/service tests that fail for the missing learning activity read API and explicit TODO action APIs.
2. Add small domain/service records for learning activity response assembly and repository access to current-week task completions.
3. Implement controller endpoints with validation for done/incomplete/skip request shapes.
4. Update `docs/specs/api-contract-v1.md`, `docs/specs/openapi.yaml`, `docs/specs/qa-acceptance-v1.md`, and Confluence mirror docs.
5. Run targeted tests, then `./gradlew check build --no-daemon`.
6. Commit, create PR through `scripts/task/create-pr.sh`, run CodeRabbit review helper, satisfy review gate, and finish via `scripts/task/finish-pr.sh`.

## Done Criteria
- `GET /api/v1/groups/{groupId}/learning-activity/me` returns current week, nullable existing progress, progress status defaulting to `NOT_STARTED`, task completion summary, and per-task completion snapshot with `TODO` for missing completion rows.
- `POST /api/v1/tasks/{taskId}/completion/me/done` stores `DONE` with optional completion note/evidence URL.
- `POST /api/v1/tasks/{taskId}/completion/me/incomplete` stores `INCOMPLETE` and rejects a blank or missing incomplete reason.
- `POST /api/v1/tasks/{taskId}/completion/me/skip` stores `SKIPPED` with no completion/incomplete payload.
- Pending members and cross-group task access remain rejected.
- Existing generic completion API remains covered and unchanged.
- Documentation/OpenAPI parse and match the implemented endpoints.
- `./gradlew check build --no-daemon` passes before PR creation.
