# EXEC_PLAN: [feat] 과제 완료 API 프론트 계약 보강

- Task slug: `spt-126-task-completion-contract`
- Base branch: `develop`
- Feature branch: `codex/spt-126-task-completion-contract`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-126-task-completion-contract`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-126-task-completion-contract`
- Jira issue: `SPT-126`
- Jira URL: https://studypot.atlassian.net/browse/SPT-126
- Jira summary: [weekly-todo] 과제 완료 API 프론트 계약 점검 및 보강
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] weekly-todo

## Doc Notes
- Jira SPT-126 requires checking the existing `POST /api/v1/tasks/{taskId}/completion/me` contract, not adding a duplicate API.
- The locked API docs currently define request fields `status`, `completionNote`, `incompleteReason`, and `evidenceUrl`.
- The current controller response intentionally omits `completionNote` and `evidenceUrl` to match the older locked OpenAPI, but SPT-126 explicitly requires frontend display fields: `taskId`, `status`, `completedAt`, `reasonSubmittedAt`, `completionNote`, `incompleteReason`, and `evidenceUrl`.
- Because response shape changes require Change Request + ADR after v1 lock, this task must add a focused CR/ADR and update API/OpenAPI/QA docs.
- Existing domain/repository already persist `completionNote`, `incompleteReason`, `reasonSubmittedAt`, and `evidenceUrl`; expected production change is controller/schema/docs/tests, not a DB migration.
- 2026-06-01: Added `CR-20260601-task-completion-response-contract` and `ADR-20260601-task-completion-response-contract` for the additive response contract.
- 2026-06-01: RED `./gradlew test --tests 'com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest' --tests 'com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest' --no-daemon` failed as expected on missing task completion response fields.
- 2026-06-01: GREEN focused tests passed after returning `taskId`, `reasonSubmittedAt`, `completionNote`, and `evidenceUrl`.
- 2026-06-01: PASS `scripts/tests/test_swagger_docs_contracts.sh`.
- 2026-06-01: PASS OpenAPI parse with `paths=30`, `schemas=41`.
- 2026-06-01: PASS `./gradlew check build --no-daemon`.
- 2026-06-01: PASS `git diff --check`.

## Goal
Make the existing task completion API usable by the frontend completion button by returning the persisted task completion display fields and pinning the DONE/INCOMPLETE/SKIPPED validation and permission behavior in tests and docs.

## Approach
Use TDD around the existing controller/service surface. First add failing tests that assert the richer response contract and cross-group access rejection. Then update `TaskCompletionResponse`, OpenAPI/API/QA docs, and a focused Change Request + ADR. Keep status transition semantics conservative: terminal same-status requests remain idempotent, terminal status changes remain rejected, and no new API or DB schema is introduced.

## Step Plan
1. Add RED controller/service tests for the frontend response fields and missing acceptance coverage.
2. Implement the minimal response DTO/schema/doc changes needed for the tests.
3. Add Change Request + ADR for the additive response shape change.
4. Run focused Gradle tests, then full `./gradlew check build --no-daemon`.
5. Commit with the required Korean subject, create PR, request CodeRabbit review, satisfy review gate, finish merge, and clean up.

## Done Criteria
- Existing `POST /api/v1/tasks/{taskId}/completion/me` returns `id`, `taskId`, `status`, `completedAt`, `reasonSubmittedAt`, `completionNote`, `incompleteReason`, and `evidenceUrl`.
- DONE/INCOMPLETE/SKIPPED request rules and already-terminal conflict/idempotency behavior are covered by tests.
- Permission and cross-group task access rejection are covered by tests.
- API contract, OpenAPI, QA acceptance, CR/ADR, and feature coverage docs match the implementation.
- `./gradlew check build --no-daemon` passes.
- PR review gate, CodeRabbit marker, auto-merge, local cleanup, and Jira completion finish successfully.
