# EXEC_PLAN: [feat] AI 팀장 단일 채팅 DB-first 컨텍스트 자동 보강

- Task slug: `ai-single-room-db-rag`
- Base branch: `develop`
- Feature branch: `codex/ai-single-room-db-rag`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/ai-single-room-db-rag`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/ai-single-room-db-rag`
- Jira issue: `SPT-135`
- Jira URL: https://studypot.atlassian.net/browse/SPT-135
- Jira summary: [feat] AI 팀장 단일 채팅 DB-first 컨텍스트 자동 보강
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/adr/ADR-20260519-ai-llm-rag-architecture.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/architecture/backend-map.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] ai-team-leader
- [x] weekly-todo
- [x] curriculum-core

## Doc Notes
- `docs/confluence/06-ai-team-leader.md` requires the AI team leader to use onboarding/current week/tasks/progress/retrospective context.
- `docs/specs/ai-contract-v1.md` defines DB-backed context collection for retrospective feedback and DB-first provider usage for AI flows.
- `CurriculumJdbcSql.SELECT_CURRENT_WEEK_BY_GROUP` defines current week as the active curriculum's `IN_PROGRESS` week; AI ordinary-chat context should use that same rule when the conversation itself has no `curriculumWeekId`.
- No public API or DB schema change is planned; the change is prompt context retrieval plus provider grounding.

## Goal
Keep ordinary AI team leader chat as one reusable room per active member/group while every message generation loads study group, active curriculum, current week, weekly tasks, member progress, retrospective context, and recent messages from DB-first context. The assistant must be instructed and tested to avoid confirming facts that are absent from DB context.

## Approach
Extend `AiConversationPromptContext` to carry study group and curriculum sections. In `JdbcAiConversationRepository.findPromptContext`, resolve an effective week by using the explicit conversation week when present, otherwise querying the active curriculum's `IN_PROGRESS` week for the group. Feed the effective week into existing task/progress retrieval. Update provider input and audit payload so the LLM call receives and records all DB context coverage. Strengthen the team-leader operating contract so missing/absent context yields a concise clarifying question or refusal to confirm, not invented study plans.

## Step Plan
1. Write RED tests for provider input/audit coverage, no-fabrication contract, current-week fallback for ordinary chat, explicit-week preservation, and single-room reuse QA behavior.
2. Implement prompt context fields, SQL queries, effective-week resolution, provider input/audit, and QA configuration.
3. Run targeted tests and update evidence when RED becomes GREEN.
4. Run `./gradlew check build --no-daemon`.
5. Run HTTP QA on reserved port `18080` for ordinary chat reuse and DB-grounded/missing-context behavior.
6. Create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, address one review loop if needed, pass review gate, and finish with `scripts/task/finish-pr.sh`.

## Done Criteria
- Ordinary `TEAM_LEAD_CHAT` with no `weekId` reuses the same open conversation.
- A message posted in that ordinary conversation receives study group, active curriculum, current week, tasks, and progress context even though the conversation remains unscoped by week.
- If no active curriculum/current week/task/progress exists, provider input uses `NOT_AVAILABLE` sections and the provider contract forbids guessing or confirming absent facts.
- Explicit week-scoped conversations continue to use their explicit week instead of current-week fallback.
- Targeted tests, full Gradle verification, and HTTP QA evidence pass.
- PR review gate and CodeRabbit marker pass before merge/cleanup.
