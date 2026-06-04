# EXEC_PLAN: [fix] AI 팀장 일반 채팅 회고 DB 컨텍스트 보강

- Task slug: `ai-chat-retrospective-context`
- Base branch: `develop`
- Feature branch: `codex/ai-chat-retrospective-context`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/ai-chat-retrospective-context`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/ai-chat-retrospective-context`
- Jira issue: `SPT-136`
- Jira URL: https://studypot.atlassian.net/browse/SPT-136
- Jira summary: [fix] AI 팀장 일반 채팅 회고 DB 컨텍스트 보강
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/confluence/04-erd-data-model.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] ai-team-leader
- [x] retrospective-feedback
- [x] weekly-todo

## Doc Notes
- `docs/confluence/06-ai-team-leader.md` requires retrospective DB-first context before LLM provider calls.
- `docs/confluence/04-erd-data-model.md` lists `retrospective.ai_feedback` and `retrospective.next_week_adjustment` as durable AI team leader outputs and expects retrospective lookup by progress/week/member.
- `docs/specs/db-schema-v1.sql` already has `retrospective.curriculum_week_id`, `member_id`, `status`, `ai_feedback`, and `next_week_adjustment`; this task does not change DB schema.
- SPT-135 / PR #220 already merged single-room study/curriculum/current-week/tasks/progress grounding; final audit found ordinary null-week chat still skipped retrospective context because it only queried by explicit `retrospectiveId`.

## Goal
Make ordinary `TEAM_LEAD_CHAT` prompt context load the authenticated member's retrospective context for the resolved current week when the conversation has no explicit `retrospectiveId`, while keeping explicit retrospective conversations bound to their given retrospective id. DB-absent retrospective facts must remain `NOT_AVAILABLE`.

## Approach
Add a repository query that selects the latest non-deleted retrospective by `curriculum_week_id` and `member_id`. Resolve week first, then load retrospective by explicit `retrospectiveId` when present, otherwise by the resolved week/member when available. Add regression tests for ordinary chat retrospective availability and absence, plus provider/QA fixture coverage that retrospective context reaches the LLM input.

## Step Plan
1. Add RED repository tests proving ordinary null-week team lead chat loads retrospective context by resolved current week and leaves it `NOT_AVAILABLE` when no retrospective row exists.
2. Add provider/QA fixture assertions so ordinary chat provider input carries retrospective context instead of only tasks/progress.
3. Implement week/member retrospective SQL and repository helper without schema/API changes.
4. Run targeted AI repository/provider/controller tests.
5. Run `./gradlew check build --no-daemon`, create PR, pass CodeRabbit/review gate, and finish merge/cleanup.

## Done Criteria
- Ordinary `TEAM_LEAD_CHAT` with no `retrospectiveId` and a resolved current week queries retrospective by current week/member and includes available feedback/adjustment context.
- If the resolved week is missing or no retrospective row exists, retrospective context is exactly `NOT_AVAILABLE` and no absent retrospective facts are confirmed.
- Explicit retrospective conversations continue to query by explicit retrospective id/member.
- Provider input and HTTP QA fixture include retrospective context for ordinary single-room chat.
- Targeted tests, full Gradle verification, PR review gate, CodeRabbit marker, merge, Jira done, and cleanup complete.
