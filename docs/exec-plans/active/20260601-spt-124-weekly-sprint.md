# EXEC_PLAN: [study-sprint-week] 1주 단위 스프린트/주차 생성

- Task slug: `spt-124-weekly-sprint`
- Base branch: `develop`
- Feature branch: `codex/spt-124-weekly-sprint`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-124-weekly-sprint`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-124-weekly-sprint`
- Jira issue: `SPT-124`
- Jira URL: https://studypot.atlassian.net/browse/SPT-124
- Jira summary: [post-mvp] 스프린트 기간 단위 설정 기능 검토
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/user-journeys-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/confluence/04-erd-data-model.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/09-qa-acceptance.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] curriculum-core
- [x] weekly-todo
- [x] ai-team-leader

## Doc Notes
- `curriculum-core` already stores `curriculum.total_weeks` and `curriculum_week.week_number`, `starts_at`, and `ends_at`.
- Current implementation creates 7-day windows from the host-start timestamp and trusts the AI-provided week count, so a three-month study can still become one generated week if the provider returns one week.
- Product decision for this task: sprint duration is fixed to one week now; user-selectable sprint duration is deferred as TODO/future change-control scope.
- No public REST endpoint, request/response field, DB table, column, or enum is added in this slice.
- The AI generation contract changes because the provider must create exactly one curriculum week per fixed weekly sprint window; this requires Change Request + ADR and affected docs.

## Goal
When an owner starts a study, derive deterministic one-week sprint windows from the study group's `startsAt` and `endsAt` dates, pass that expected sprint plan into curriculum generation, and persist curriculum weeks/tasks on those windows instead of anchoring weeks to the host-start timestamp.

## Approach
- Add a small domain planner that converts the study date range into fixed one-week windows at UTC day boundaries, capping the final window at the inclusive study end date plus one day.
- Extend `CurriculumGenerationRequest` with the sprint windows so the provider prompt/input and audit payload know the expected week count.
- Validate provider output against the expected week count and sequential week numbers before creating `CurriculumGeneration`.
- Build persisted `curriculum_week.starts_at`, `curriculum_week.ends_at`, and weekly task `due_at` from the deterministic sprint windows.
- Update tests first for planner behavior, service behavior, and provider output validation, then implement the minimal production code.
- Record the AI behavior change through CR/ADR and update locked docs that describe curriculum generation, QA, and feature coverage.

## Step Plan
1. Add failing tests for fixed weekly sprint planning, start-study persistence windows, generation-count mismatch, and provider prompt/output validation.
2. Implement the sprint window value object/planner and request propagation.
3. Update curriculum building to use sprint windows and keep first week `IN_PROGRESS`, later weeks `PENDING`.
4. Update provider instructions/input/request payload and reject non-matching `totalWeeks`/week arrays.
5. Add CR/ADR plus documentation updates for fixed one-week sprint windows and deferred configurable sprint duration.
6. Run targeted tests, then `./gradlew check build --no-daemon`.
7. Commit, create PR, run CodeRabbit, address one review loop if needed, verify review gate, auto-merge, cleanup, and record Jira done.

## Done Criteria
- `CurriculumSprintPlanner` creates deterministic one-week windows from `study_group.starts_at`/`ends_at`.
- `POST /api/v1/groups/{groupId}/start` persists one generated curriculum week per planned sprint window.
- Weekly task `due_at` equals the planned window `endsAt`.
- Provider output is rejected when `totalWeeks` or week array count does not match the planned sprint count.
- Configurable sprint unit remains explicitly deferred in docs/TODO scope, with no API/DB change in this task.
- Relevant tests pass and `./gradlew check build --no-daemon` passes.
- PR has Jira evidence, CodeRabbit PASS/ADDRESSED marker, GitHub Actions Review Gate PASS marker, and is merged to `develop`.
- `SPT-124` is recorded complete by the harness/Jira flow.
