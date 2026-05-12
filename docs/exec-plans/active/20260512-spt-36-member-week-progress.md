# EXEC_PLAN: [weekly-todo] member_week_progress 주차 상태 구현

- Task slug: `spt-36-member-week-progress`
- Base branch: `develop`
- Feature branch: `codex/spt-36-member-week-progress`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-36-member-week-progress`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-36-member-week-progress`
- Jira issue: `SPT-36`
- Jira URL: https://studypot.atlassian.net/browse/SPT-36
- Jira summary: [weekly-todo] member_week_progress 주차 상태 구현
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] Jira SPT-36 issue detail
- [x] docs/specs/prd-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/domain-erd.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/exec-plans/active/20260512-spt-34-group-status-limits.md
- [x] docs/exec-plans/active/20260512-spt-35-weekly-task-list.md

## Related Feature IDs
- [x] weekly-todo

## Doc Notes
- Jira SPT-36 scope is only `member_week_progress`: create and update each member's weekly progress record, use statuses `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `INCOMPLETE`, `FEEDBACK_READY`, align the record with the curriculum week window, and give late joiners progress from the current week.
- Locked OpenAPI exposes `PUT /api/v1/weeks/{weekId}/progress/me` with `UpdateWeekProgressRequest` and `MemberWeekProgressResponse`; it does not define a separate progress GET endpoint. SPT-36 will return the current/upserted progress from this locked PUT endpoint and avoid adding a new endpoint without a Change Request.
- `member_week_progress` already exists in the locked DB schema with unique `(curriculum_week_id, member_id)`, timestamps, `completion_note`, `incomplete_reason`, and `reason_submitted_at`; no schema change is needed.
- `auth-permissions-v1.md` requires only `ACTIVE` members/owners to participate in weekly work. `PENDING_ONBOARDING` and `LEFT` members cannot create weekly progress.
- SPT-34 enabled active-group late join + onboarding submit but intentionally left current-week progress creation to SPT-36.
- SPT-35 exposed current week and ordered weekly task reads and explicitly deferred progress creation/update.
- `task_completion`, DONE/SKIPPED task state, incomplete reason submission for individual tasks, and rule_violation linkage remain SPT-37/out of SPT-36 scope.
- The locked v1 specs are not changed in this task.

## Goal
Implement the SPT-36 member-week-progress slice: an authenticated active group member can call `PUT /api/v1/weeks/{weekId}/progress/me` to create their own `member_week_progress` for that curriculum week if it does not exist, receive the existing record if it already exists, and update allowed progress fields/status without creating duplicates. Late joiners who became `ACTIVE` after group start use the same current-week path and receive progress from the requested/current week onward.

## Approach
- Keep SPT-36 inside the existing `curriculum` package because `member_week_progress` belongs to the curriculum/todo aggregate and the existing weekly todo endpoints already live there.
- Add a small domain model for `MemberWeekProgress` and `MemberWeekProgressStatus`, plus a service command for `UpdateWeekProgressRequest`.
- Reuse `findReadContextByWeekId` to resolve the current member id through the week and enforce active membership. Owners are allowed only when their member status is active because weekly participation is member-based.
- Repository will first look up progress by `(weekId, memberId)`. If missing, insert one with a generated id, requested status, `due_at` copied from `curriculum_week.ends_at`, and unique-key protection. If a concurrent insert wins the race, fetch that existing row, apply the same request, and persist the update once.
- Status transition rules for SPT-36 stay progress-level and conservative: supported statuses are the locked enum values; `IN_PROGRESS` sets `started_at` if missing, `COMPLETED` sets `completed_at`, `INCOMPLETE` requires `incompleteReason` and sets `reason_submitted_at`, `NOT_STARTED` cannot carry completion/incomplete fields, and `FEEDBACK_READY` can only be set after completion/incomplete progress data exists.
- Use TDD: write service/repository/controller tests that fail because the command/domain/repository/controller endpoint does not exist, then implement the minimum code to pass.

## Step Plan
1. RED service tests:
   - creates `member_week_progress` for an active member when no row exists.
   - returns/updates existing progress without duplicate creation.
   - rejects pending, left, and non-member users.
   - rejects invalid status payload combinations such as `INCOMPLETE` without reason and `NOT_STARTED` with completion note/reason.
   - rejects unknown week with not-found.
2. RED repository tests:
   - SQL resolves week membership context and current member id through `curriculum_week -> curriculum -> study_group -> group_member`.
   - find-by-week-member maps all progress columns.
   - insert uses `member_week_progress_uidx` natural key inputs, week `ends_at` as due date, and generated id.
   - update persists status, started/completed/reason timestamps, completion note, and incomplete reason.
3. RED controller tests:
   - `PUT /api/v1/weeks/{weekId}/progress/me` requires authentication and CSRF.
   - active member can create/update progress and receives locked `MemberWeekProgressResponse` fields.
   - pending member receives forbidden.
   - invalid request body receives a validation problem response.
4. GREEN domain/service/repository/controller implementation.
5. Run targeted tests for `CurriculumServiceTest`, `JdbcCurriculumRepositoryTest`, and `CurriculumControllerTest`.
6. Run `./gradlew check build --no-daemon`.
7. Commit with `[feat] 주차 진행 상태 구현`.
8. Create PR with `scripts/task/create-pr.sh`, run `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`, address one actionable review loop if needed, verify review gate, and run `scripts/task/finish-pr.sh <PR_NUMBER>` for manual merge notification.

## Done Criteria
- `PUT /api/v1/weeks/{weekId}/progress/me` requires authenticated access and CSRF protection.
- `UpdateWeekProgressRequest.status` is required.
- An `ACTIVE` member can create their own `member_week_progress` for a curriculum week.
- Existing progress for the same `(curriculum_week_id, member_id)` is updated/returned instead of duplicated.
- Late joiners who have submitted onboarding and become `ACTIVE` can create progress for the current week using the same endpoint.
- `PENDING_ONBOARDING`, `LEFT`, and non-member users cannot create or update progress.
- Supported progress statuses are `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `INCOMPLETE`, and `FEEDBACK_READY`.
- `IN_PROGRESS` records `started_at` when first started.
- `COMPLETED` records `completed_at` and optional `completion_note`.
- `INCOMPLETE` requires `incompleteReason` and records `reason_submitted_at`.
- Repository tests cover SQL, row mapping, insert/upsert guard, and update behavior.
- Controller tests cover authenticated API behavior, permission failures, and validation failures.
- No `task_completion`, rule violation, AI, notification, or frontend scope is implemented in SPT-36.
- `./gradlew check build --no-daemon` passes and task state records the successful verification.
- PR is created against `develop`; CodeRabbit review marker and GitHub Actions Review Gate pass before manual merge notification.

## Verification
- RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` failed before implementation because `MemberWeekProgress`, `MemberWeekProgressStatus`, `UpdateWeekProgressCommand`, and repository/controller contracts did not exist.
- GREEN targeted: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` passed.
- Full: `./gradlew check build --no-daemon` passed.
- CodeRabbit fix RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` failed with expected review-fix coverage failures before the fix: soft-delete SQL guards, concurrent insert update application, first completion timestamp/note preservation, invalid backward transition, and missing status validation.
- CodeRabbit fix GREEN targeted: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` passed.
- CodeRabbit fix full: `./gradlew check build --no-daemon` passed.
