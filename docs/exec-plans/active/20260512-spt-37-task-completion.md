# EXEC_PLAN: [weekly-todo] task_completion 완료/미완료 사유 구현

- Task slug: `spt-37-task-completion`
- Base branch: `develop`
- Feature branch: `codex/spt-37-task-completion`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-37-task-completion`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-37-task-completion`
- Jira issue: `SPT-37`
- Jira URL: https://studypot.atlassian.net/browse/SPT-37
- Jira summary: [weekly-todo] task_completion 완료/미완료 사유 구현
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/exec-plans/active/20260512-spt-35-weekly-task-list.md
- [x] docs/exec-plans/active/20260512-spt-36-member-week-progress.md
- [x] docs/exec-plans/active/20260512-spt-33-group-rules.md

## Related Feature IDs
- [x] weekly-todo

## Doc Notes
- Jira SPT-37 scope is `task_completion`: implement member todo completion and incomplete reason submission.
- `REQ-TODO-002` requires a member to complete todos before the deadline and store completion timestamp/note.
- `REQ-TODO-003` requires incomplete reason and submitted timestamp after the deadline.
- Locked API exposes `POST /api/v1/tasks/{taskId}/completion/me` with `TaskCompletionRequest.status` and response fields `id`, `status`, `completedAt`, `incompleteReason`.
- `TaskCompletionRequest` also includes `completionNote`, `incompleteReason`, and `evidenceUrl`; `completionNote` and `evidenceUrl` are persisted but not returned because locked `TaskCompletionResponse` does not expose them.
- Locked DB already defines `task_completion` with unique `(weekly_task_id, member_id)`, `progress_id`, status, due/completion/reason timestamps, notes, and evidence URL. No DB schema change is needed.
- `task_completion.status` supports `TODO`, `DONE`, `INCOMPLETE`, and `SKIPPED`; SPT-37 accepts actionable endpoint requests for `DONE`, `INCOMPLETE`, and `SKIPPED`.
- `task_completion` must link to the caller's existing `member_week_progress` for the task's curriculum week. SPT-37 does not auto-create progress because SPT-36 owns progress creation and the target flow creates progress before task completion.
- Permission follows `auth-permissions-v1.md`: `PENDING_ONBOARDING` and `LEFT` members cannot complete weekly tasks; only the authenticated active member can mutate their own task completion.
- SPT-33 already lets rule violations reference a `task_completion_id`; SPT-37 creates the task completion rows needed for that linkage but does not auto-record rule violations.
- Final integrated Swagger/API verification found the previously merged onboarding/curriculum/rule slices returning 503 because feature repository/service beans were not created in the real Spring Boot context.
- The root cause is configuration wiring, not endpoint logic: several repository configurations use `@ConditionalOnBean(JdbcTemplate.class)` during user-configuration processing, and `CurriculumService` is also conditional on `CurriculumGenerator`, which blocks read/progress/completion APIs when OpenAI generation is intentionally not configured for local verification.
- The same integrated run exposed a DB contract mismatch: `member_week_progress` has no `deleted_at` column in the locked Flyway schema, so progress lookup/update SQL must not filter that column.
- `GET /api/v1/weeks/{weekId}/progress/me` is still not part of the locked OpenAPI contract; the current implemented progress API is `PUT /api/v1/weeks/{weekId}/progress/me`, which returns the current row after creation/update.
- AI retrospective/feedback, notification worker, external notification channels, frontend UI, and locked spec changes are out of scope.

## Goal
Implement the SPT-37 task-completion slice: an authenticated active group member can call `POST /api/v1/tasks/{taskId}/completion/me` to mark their own weekly task as `DONE`, `INCOMPLETE`, or `SKIPPED`, with a unique `task_completion` row linked to that member's `member_week_progress` and `weekly_task`.

## Approach
- Keep SPT-37 in the existing `curriculum` package because the weekly todo endpoints and `member_week_progress` implementation live there.
- Add `TaskCompletion` and `TaskCompletionStatus` domain types, plus a `CompleteTaskCommand`.
- Resolve authorization through `weekly_task -> curriculum_week -> curriculum -> study_group -> group_member`, using the existing active membership pattern.
- Require existing `member_week_progress` for `(task.curriculumWeekId, memberId)` before inserting/updating `task_completion`.
- Apply conservative state rules:
  - create/update from no row or existing `TODO` to `DONE`, `INCOMPLETE`, or `SKIPPED`;
  - repeat of the same terminal status is idempotent and preserves the first terminal timestamp/note/reason;
  - terminal-to-different-terminal transitions are rejected;
  - `DONE` requires the task not to be overdue when `dueAt` exists and stores `completed_at`; when `dueAt` is null, `DONE` is accepted immediately and still stores `completed_at`;
  - `INCOMPLETE` requires `incompleteReason`, stores `reason_submitted_at`, and is accepted only after `dueAt` when `dueAt` exists; when `dueAt` is null, `INCOMPLETE` is accepted immediately and still stores `reason_submitted_at`;
  - `SKIPPED` stores neither completion nor incomplete timestamps.
- Handle duplicate insert races by re-reading the existing row for `(weekly_task_id, member_id)` and applying the same request once.
- Use TDD: write service, repository, and controller tests first and verify they fail before implementation.
- For the integrated 503 fix, add a Spring Boot wiring regression test first, then minimally align repository conditions with the working auth/study-group pattern and allow `CurriculumService` to exist without an OpenAI generator so non-generation weekly todo APIs remain available.

## Step Plan
1. RED service tests:
   - active member completes a task as `DONE` with `completed_at` and `completion_note`.
   - active member submits `INCOMPLETE` after deadline with `incomplete_reason` and `reason_submitted_at`.
   - active member marks a task `SKIPPED`.
   - duplicate row/update is prevented by reusing/updating existing completion instead of inserting another row.
   - terminal-to-different-terminal transition is rejected.
   - `INCOMPLETE` without reason and `DONE` after due date are rejected.
   - pending/LEFT/non-member access and missing task/progress cases are rejected.
2. RED repository tests:
   - task membership context resolves through task -> week -> curriculum -> group -> member.
   - task lookup filters soft-deleted weekly tasks and maps `due_at`.
   - find/insert/update task completion SQL uses `(weekly_task_id, member_id)` and persists all mutable fields.
   - insert handles duplicate-key race as a non-success path.
3. RED controller tests:
   - `POST /api/v1/tasks/{taskId}/completion/me` requires authentication and CSRF.
   - `DONE`, `INCOMPLETE`, and `SKIPPED` responses match locked `TaskCompletionResponse`.
   - missing `status` and invalid payload combinations return validation problem responses.
   - pending member receives forbidden.
4. GREEN domain/service/repository/controller implementation.
5. Run targeted tests for `CurriculumServiceTest`, `JdbcCurriculumRepositoryTest`, and `CurriculumControllerTest`.
6. Run `./gradlew check build --no-daemon`.
7. Commit with `[feat] 과제 완료 상태 구현`.
8. Create PR with `scripts/task/create-pr.sh`, run `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`, address one actionable review loop if needed, verify review gate, and run `scripts/task/finish-pr.sh <PR_NUMBER>` for manual merge notification.
9. RED integrated wiring test: prove onboarding, curriculum, and group-rule repositories/services are missing from a Spring Boot context with JDBC configured but OpenAI disabled.
10. GREEN configuration fix: make those repositories/services available in that context while keeping curriculum generation itself unavailable until a generator is configured.
11. RED repository SQL test: prove `member_week_progress` SQL must match the real schema and not reference `deleted_at`.
12. GREEN SQL fix: remove the nonexistent `deleted_at` filter from `member_week_progress` lookup/update SQL.
13. Re-run targeted wiring/repository tests, full verification, and the local Swagger/API smoke flow before updating PR #85.

## Done Criteria
- `POST /api/v1/tasks/{taskId}/completion/me` requires authenticated access and CSRF protection.
- Request `status` is required.
- `DONE` stores `completed_at` and optional `completion_note`.
- `INCOMPLETE` requires `incompleteReason` and stores `reason_submitted_at`.
- `SKIPPED` can be stored for the member's task.
- `task_completion` is unique by `(weekly_task_id, member_id)` and duplicate creation is prevented.
- `task_completion.progress_id` links to the authenticated member's `member_week_progress`.
- Active members can update only their own task completion.
- `PENDING_ONBOARDING`, `LEFT`, and non-member users cannot create or update completion records.
- Missing `weekly_task` and missing `member_week_progress` are rejected.
- Terminal task completion state cannot be changed to a different terminal state.
- Repository tests cover SQL, mapping, insert/update, and duplicate race handling.
- Controller tests cover authenticated API behavior, permission failures, and validation failures.
- No rule violation automation, AI, notification worker, external channel, frontend, or locked spec change is implemented.
- `./gradlew check build --no-daemon` passes and task state records the successful verification.
- PR is created against `develop`; CodeRabbit review marker and GitHub Actions Review Gate pass before manual merge notification.
- Real Spring Boot local profile registers onboarding, curriculum, and group-rule repository/service beans when JDBC is configured.
- Weekly todo read/progress/completion APIs remain callable when OpenAI generation is not configured; only start-generation requires the generator.
- `member_week_progress` SQL matches the locked Flyway schema.

## Verification
- RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` failed before implementation because `TaskCompletion`, `TaskCompletionStatus`, task-completion command/repository/controller contracts did not exist.
- GREEN targeted: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon` passed.
- Full: `./gradlew check build --no-daemon` passed.
- CodeRabbit RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CompleteTaskCommandTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --tests com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatusTest --tests com.studypot.aistudyleader.curriculum.service.InvalidTaskCompletionRequestExceptionTest --tests com.studypot.aistudyleader.curriculum.service.TaskCompletionUpdateRejectedExceptionTest --tests com.studypot.aistudyleader.global.error.ApiExceptionHandlerTaskCompletionTest --no-daemon` failed before review fixes on null `status` and incomplete-reason field mapping.
- CodeRabbit GREEN targeted: the same targeted command passed after review fixes.
- CodeRabbit full: `./gradlew check build --no-daemon` passed after review fixes.
- Integrated wiring RED: `./gradlew test --tests com.studypot.aistudyleader.ApplicationFeatureWiringTest --no-daemon` failed before configuration fixes because onboarding/curriculum/group-rule repositories and services were missing from the Spring Boot context.
- Integrated wiring GREEN targeted: `./gradlew test --tests com.studypot.aistudyleader.ApplicationFeatureWiringTest --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --no-daemon` passed after repository/service configuration fixes.
- Progress SQL RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --no-daemon` failed before removing the nonexistent `member_week_progress.deleted_at` filter.
- Progress SQL GREEN targeted: `./gradlew test --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.ApplicationFeatureWiringTest --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --no-daemon` passed.
- Integrated full: `./gradlew check build --no-daemon` passed after configuration and SQL fixes.
- Local Swagger/API flow: `build/manual-flow-spt37-v5/status.log` passed for OpenAPI, Swagger UI, my groups, current week, weekly task list, week progress update, task DONE/INCOMPLETE/SKIPPED, group_rule save/list/deactivate/delete, and rule_violation record/resolve/waive.
- Local Swagger/API note: `GET /api/v1/weeks/{weekId}/progress/me` returned 403 because the locked OpenAPI currently exposes only `PUT /api/v1/weeks/{weekId}/progress/me` for progress mutation/response.
- CodeRabbit addressed targeted: `./gradlew test --tests com.studypot.aistudyleader.ApplicationFeatureWiringTest --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --tests com.studypot.aistudyleader.global.error.ApiExceptionHandlerTaskCompletionTest --no-daemon` passed after review fixes.
- CodeRabbit addressed full: `./gradlew check build --no-daemon` passed after review fixes.
