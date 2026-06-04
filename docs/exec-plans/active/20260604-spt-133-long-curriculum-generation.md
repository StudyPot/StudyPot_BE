# EXEC_PLAN: [curriculum] 1년 초과 커리큘럼 생성 실패 보정

- Task slug: `spt-133-long-curriculum-generation`
- Base branch: `develop`
- Feature branch: `codex/spt-133-long-curriculum-generation`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-133-long-curriculum-generation`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-133-long-curriculum-generation`
- Jira issue: `SPT-133`
- Jira URL: https://studypot.atlassian.net/browse/SPT-133
- Jira summary: [curriculum] 1년 초과 커리큘럼 생성 실패 보정
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
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/change-requests/CR-20260601-fixed-weekly-sprint-windows.md
- [x] docs/specs/adr/ADR-20260601-fixed-weekly-sprint-windows.md

## Related Feature IDs
- [x] curriculum-core
- [x] weekly-todo
- [x] ai-team-leader

## Doc Notes
- v1 API/DB shape is locked; this task must not add a request field, response field, table, or migration.
- Approved SPT-124 contract says host start derives fixed one-week sprint windows from `study_group.starts_at` through inclusive `study_group.ends_at`.
- AI contract requires generated `totalWeeks`, week array size, and sequential week numbers to match the backend-computed sprint plan.
- There is no documented one-year maximum for study group periods; adding one would be a product/API contract change and is out of scope.
- Initial investigation found no group creation/domain validation that rejects periods over one year. The failure surface was the curriculum-generation provider call/output validation when 53+ weeks must be returned as one JSON response under the previous 4096 token output budget and 30s application read-timeout default.

## Goal
Allow a study period longer than one year to proceed through deterministic sprint planning and curriculum generation without a vague generation failure caused by the default output budget being too small for 53+ generated weeks.

## Approach
1. Add a focused RED test proving a 53-week curriculum generation request receives enough provider output budget metadata to support long-period output.
2. Keep the existing fixed weekly sprint contract: no new period cap, no API/DB shape changes, no configurable sprint unit.
3. Increase the default curriculum-generation output token budget and deployment/example defaults consistently, while preserving environment-variable override behavior.
4. Add boundary tests for one-year-plus sprint windows so future changes cannot accidentally introduce a 52-week ceiling.
5. Run targeted tests first, then the standard `./gradlew check build --no-daemon`.

## Step Plan
1. Write failing tests around 53-week sprint planning and OpenAI curriculum output budget defaults.
2. Implement the smallest configuration/default changes needed for those tests to pass.
3. Re-run targeted tests for curriculum planning/provider properties.
4. Run full verification.
5. Commit with `[fix] ...`, create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, satisfy review gate, finish PR, and cleanup.

## Verification Log
- RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiCurriculumPropertiesTest --tests com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiLlmProviderTest --tests com.studypot.aistudyleader.curriculum.domain.CurriculumSprintPlannerTest --no-daemon` failed on 4096 output budget expectations after tests required 16384.
- RED: `./gradlew test --tests com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiCurriculumPropertiesTest --no-daemon` failed on 30s read timeout after tests required 120s.
- GREEN: `./gradlew test --tests com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiCurriculumPropertiesTest --tests com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiLlmProviderTest --tests com.studypot.aistudyleader.curriculum.domain.CurriculumSprintPlannerTest --no-daemon` passed after increasing curriculum output budget and read timeout defaults.
- GREEN: `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.service.ProviderBackedCurriculumGeneratorTest --no-daemon` passed for surrounding generation/service contracts.
- GREEN: `./gradlew check build --no-daemon` passed locally.

## Done Criteria
- `CurriculumSprintPlanner` has explicit coverage for periods over one year and returns 53+ windows without throwing.
- OpenAI curriculum-generation default output budget is large enough for long curriculum JSON and is reflected in app config/deploy examples.
- No v1 API or DB contract shape changes are introduced.
- Targeted tests pass.
- `./gradlew check build --no-daemon` passes.
- PR is created, CodeRabbit review marker is present for latest head, GitHub Actions review gate passes, PR is merged, and local cleanup is complete.
