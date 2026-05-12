# EXEC_PLAN: [weekly-todo] weekly_task 템플릿/목록 구현

- Task slug: `spt-35-weekly-task-list`
- Base branch: `develop`
- Feature branch: `codex/spt-35-weekly-task-list`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-35-weekly-task-list`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-35-weekly-task-list`
- Jira issue: `SPT-35`
- Jira URL: https://studypot.atlassian.net/browse/SPT-35
- Jira summary: [weekly-todo] weekly_task 템플릿/목록 구현
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/prd-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/domain-erd.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] weekly-todo

## Doc Notes
- `REQ-TODO-001`: curriculum weeks contain weekly tasks with type, order, title, required flag, and due timestamp.
- `api-contract-v1.md` locks `GET /api/v1/groups/{groupId}/weeks/current` and `GET /api/v1/weeks/{weekId}/tasks` for `weekly-todo`.
- `openapi.yaml` defines `CurriculumWeekResponse` and `WeeklyTaskResponse`; SPT-35 will not add new API fields outside the locked contract.
- `db-contract-v1.md` and `domain-erd.md` already define `weekly_task`, task type enum, and `display_order` uniqueness/order expectation.
- `auth-permissions-v1.md` requires only active members/owners to read curriculum/current week; pending members cannot participate in weekly tasks.
- `QA-TODO-001` covers weekly task listing for active members.
- Existing `CurriculumGeneration` persists `weekly_task` rows and marks the first generated week `IN_PROGRESS`; current-week lookup will use the active curriculum's `IN_PROGRESS` week.

## Goal
Expose the SPT-35 weekly-task read slice: an active group member can resolve the active curriculum's current week and list that week's `weekly_task` rows ordered by `display_order`, with task type, required flag, due timestamp, title, and description preserved.

## Approach
Keep the implementation inside the existing `curriculum` package because `weekly_task` currently belongs to the generated curriculum aggregate. Add read-only service methods and repository queries for:
- current week by group from the active curriculum and `IN_PROGRESS` week status;
- weekly task list by week id, guarded by group membership resolved through the week.

Do not implement SPT-36 progress creation/update or SPT-37 task completion state changes in this branch. Do not change locked specs or DB schema.

## Step Plan
1. Write failing service tests for current-week resolution, ordered task listing, pending member denial, non-member denial, and missing current week/not-found cases.
2. Write failing repository SQL/mapping tests for current-week lookup, week membership context, and `weekly_task` ordering by `display_order`.
3. Write failing controller API tests for authentication, current-week response, weekly-task list response, and forbidden pending member access.
4. Implement the minimal domain/service/repository/controller changes to pass those tests.
5. Run targeted domain/service/repository/controller tests.
6. Run `./gradlew check build --no-daemon`.
7. Commit with `[feat] ...`, create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, address actionable feedback once, verify review gate, then `finish-pr`.

## Done Criteria
- `GET /api/v1/groups/{groupId}/weeks/current` returns the active curriculum's current week for an authenticated active group member.
- `GET /api/v1/weeks/{weekId}/tasks` returns that week's tasks sorted by `display_order`.
- `WeeklyTaskResponse` includes id, curriculumWeekId, displayOrder, taskType, title, description, required, and dueAt.
- Task type enum values `READING`, `PRACTICE`, `ASSIGNMENT`, `PROJECT`, and `CUSTOM` remain supported.
- Pending/non-member access is rejected according to the locked permission contract.
- Repository tests cover the SQL order/filter contract and row mapping.
- Controller tests cover authenticated API behavior.
- `./gradlew check build --no-daemon` passes and task state records the verification.

## Verification
- RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` failed before implementation because SPT-35 query/service/repository/controller contracts did not exist.
- GREEN targeted: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` passed.
- Full: `./gradlew check build --no-daemon` passed.
