# EXEC_PLAN: [fix] AI팀장 응답 내부 진단 필드 숨김

- Task slug: `hide-ai-team-leader-internal-fields`
- Base branch: `develop`
- Feature branch: `codex/hide-ai-team-leader-internal-fields`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/hide-ai-team-leader-internal-fields`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/hide-ai-team-leader-internal-fields`
- Jira issue: `SPT-132`
- Jira URL: https://studypot.atlassian.net/browse/SPT-132
- Jira summary: AI팀장 응답 내부 진단 필드 숨김
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/architecture/backend-map.md
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] ai-team-leader

## Doc Notes
- 2026-06-04: `TEAM_LEAD_CHAT` is allowed to use DB-first context and return an assistant message plus optional summary/next-week adjustment candidate.
- 2026-06-04: This task does not add API fields, DB columns, or AI output schema fields; it only tightens the provider prompt/input contract and parsing safety so internal diagnostic labels do not appear in user-facing assistant text.
- 2026-06-04: Existing `teamLeaderOperatingContract` currently contains internal camelCase labels `observedDbEvidence`, `inferenceFromContext`, and `recommendedNextAction`. The reported leak concerns at least `observedDbEvidence` and `recommendedNextAction`.
- 2026-06-04: RED `./gradlew test --tests com.studypot.aistudyleader.ai.service.ProviderBackedAiConversationAssistantResponseGeneratorTest --no-daemon` failed as expected because the provider input still contained internal labels and generated messages could return those labels unchanged.
- 2026-06-04: GREEN focused verification passed after changing the provider operating contract to user-facing wording and stripping internal diagnostic labels from generated assistant messages.
- 2026-06-04: Full verification `./gradlew check build --no-daemon` passed.

## Goal
Prevent `observedDbEvidence` and `recommendedNextAction` from appearing in AI team-leader messages shown to users while preserving DB-grounded evidence, inference, and concrete next-action behavior.

## Approach
Use TDD around `ProviderBackedAiConversationAssistantResponseGenerator`. First add a failing regression test proving generated user-facing `message` is sanitized when the provider echoes internal labels. Then remove internal camelCase labels from the operating contract and add a final message sanitization guard before the assistant response is persisted/returned. Keep `conversationSummary`, metadata, request payload, API schema, and DB contract unchanged.

## Step Plan
1. Add RED tests for leaked internal labels in provider output and provider input contract.
2. Implement minimal sanitization and prompt/input wording changes inside the AI conversation response generator.
3. Run the focused AI response generator test.
4. Run the full `./gradlew check build --no-daemon` verification before commit.
5. Create PR through `scripts/task/create-pr.sh`, run CodeRabbit review, satisfy review gates, finish PR, and cleanup through harness scripts.

## Done Criteria
- `observedDbEvidence` and `recommendedNextAction` are absent from user-facing AI team-leader `message` values even if provider output includes them.
- Provider input no longer instructs the model with those internal camelCase labels.
- Existing DB-first AI team-leader behavior and response metadata remain compatible.
- Focused tests and `./gradlew check build --no-daemon` pass.
- PR is created, reviewed, gated, merged, and cleaned up through the StudyPot harness.
