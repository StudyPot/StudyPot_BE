# EXEC_PLAN: [fix] AI 팀장 대화 품질과 회고 진행률 오류 보정

- Task slug: `spt-120-ai-leader-progress-fix`
- Base branch: `develop`
- Feature branch: `codex/spt-120-ai-leader-progress-fix`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-120-ai-leader-progress-fix`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-120-ai-leader-progress-fix`
- Jira issue: `SPT-120`
- Jira URL: https://studypot.atlassian.net/browse/SPT-120
- Jira summary: [fix] AI 팀장 대화 품질과 회고 진행률 오류 보정
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-week-progress-read-endpoint.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/user-journeys-v1.md
- [x] docs/specs/domain-erd.md
- [x] docs/confluence/06-ai-team-leader.md

## Related Feature IDs
- [x] ai-team-leader
- [x] retrospective-feedback
- [x] weekly-todo

## Doc Notes
- `TEAM_LEAD_CHAT` is already a DB-first Spring Boot context-builder path. It passes conversation, recent messages, current user message, week, tasks, progress, and retrospective data into the provider, but the current provider instructions are generic and do not force a team-leader answer style that names observed evidence, interpretation, and next action.
- `POST /api/v1/tasks/{taskId}/completion/me` currently looks up `member_week_progress` and throws `member week progress was not found.` when the row has not been initialized. This is the likely visible failure when the UI completes a task before calling `PUT /weeks/{weekId}/progress/me`.
- `POST /api/v1/weeks/{weekId}/retrospectives/me` intentionally requires weekly progress as the canonical retrospective source. Creating a retrospective from no progress would weaken `REQ-RETRO-001`; instead, task completion should ensure the progress row exists before downstream retrospective.
- `GET /api/v1/weeks/{weekId}/progress/me` is read-only by ADR and must continue returning not-found without creating a row.
- Current sprint modeling is per `curriculum_week`: `curriculum.total_weeks`, `curriculum_week.week_number`, `starts_at`, `ends_at`, and `sprint_goal`. A new user-selected sprint-size API or DB field would be a locked contract change and is out of this fix.

## Goal
Fix the reported backend pain points without changing locked API/DB/AI schemas: make AI team-leader chat prompt like a StudyPot operator using DB context, and stop task completion from surfacing `member week progress was not found.` when progress has not been initialized.

## Approach
Use TDD. First add a failing provider prompt-contract test that requires the `TEAM_LEAD_CHAT` request to carry a team-leader operating contract: cite DB-first evidence, explain interpretation, state uncertainty, and propose the next action. Then add a failing curriculum service test for completing a task when `member_week_progress` is missing. Implement the smallest changes: strengthen the provider instruction/input contract and make task completion create an `IN_PROGRESS` progress row through the same domain/repository path already used by `PUT /progress/me`, preserving the read-only GET behavior and retrospective canonical progress requirement.

## Step Plan
1. Add RED test in `ProviderBackedAiConversationAssistantResponseGeneratorTest` that inspects `provider.request.instructions()` and `provider.request.input()` for the team-leader answer contract.
2. Run the focused AI provider test and confirm it fails because the prompt lacks the new operating contract.
3. Add RED test in `CurriculumServiceTest` that calls `completeMyTask` with no existing progress and expects a new progress row plus a task completion using that progress id.
4. Run the focused curriculum service test and confirm it fails with `member week progress was not found.`
5. Update `ProviderBackedAiConversationAssistantResponseGenerator` instructions/input only; do not change the JSON response schema.
6. Extract/reuse progress creation inside `CurriculumService.completeMyTask` so missing progress is initialized as `IN_PROGRESS` before creating the task completion.
7. Run focused tests for AI provider, curriculum service, and retrospective service to prove the fix does not create retrospective rows without progress.
8. Run `./gradlew check build --no-daemon`.
9. Create PR with `scripts/task/create-pr.sh`, run `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`, address one CodeRabbit pass if needed, verify PR readiness, and finish/cleanup through `scripts/task/finish-pr.sh` when gates pass.

## Done Criteria
- `TEAM_LEAD_CHAT` provider requests include an explicit StudyPot team-leader operating contract backed by DB-first context.
- Completing a task no longer throws `member week progress was not found.` solely because the progress row has not been initialized.
- `GET /weeks/{weekId}/progress/me` remains read-only and can still return not-found when no row exists.
- Retrospective creation still uses canonical `member_week_progress` and is not changed into a schema/API workaround.
- Current sprint behavior is reported as week-based `curriculum_week`/`sprint_goal`/`totalWeeks`; user-selected sprint unit changes are deferred to a separate change-control task.
- Focused tests and `./gradlew check build --no-daemon` pass.
- PR/review-gate flow completes according to AGENTS.md, or a real external blocker is recorded.

## Execution Notes
- RED: `./gradlew test --tests com.studypot.aistudyleader.ai.service.ProviderBackedAiConversationAssistantResponseGeneratorTest.requestCarriesTeamLeaderOperatingContractForDbGroundedCoaching --no-daemon` failed because the provider instructions lacked the DB-grounded team-leader operating contract.
- RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest.completeMyTaskCreatesWeekProgressWhenProgressDoesNotExist --no-daemon` failed with `member week progress was not found.`
- GREEN: both focused tests passed after adding the prompt contract and missing-progress initialization for task completion.
- Focused regression: `./gradlew test --tests com.studypot.aistudyleader.ai.service.ProviderBackedAiConversationAssistantResponseGeneratorTest --tests com.studypot.aistudyleader.ai.service.AiConversationServiceTest --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.retrospective.service.RetrospectiveServiceTest --no-daemon` passed.
- Standard verification: `./gradlew check build --no-daemon` passed on 2026-05-28.
- Caution: running two Gradle `test` commands concurrently in the same worktree produced a transient test-result file race. Verification was rerun serially afterward.
