# EXEC_PLAN: [feat] AI 팀장 자연 대화 및 DB RAG 검증

- Task slug: `ai-team-lead-human-rag`
- Base branch: `develop`
- Feature branch: `codex/ai-team-lead-human-rag`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/ai-team-lead-human-rag`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/ai-team-lead-human-rag`
- Jira issue: `SPT-134`
- Jira URL: https://studypot.atlassian.net/browse/SPT-134
- Jira summary: [feat] AI 팀장 자연 대화 및 DB RAG 검증
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/adr/ADR-20260519-ai-llm-rag-architecture.md
- [x] docs/architecture/backend-map.md
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] ai-team-leader

## Doc Notes
- v1 AI contract is locked; this task must not add vector store, GraphRAG, FastAPI service split, API schema, or DB schema changes.
- `TEAM_LEAD_CHAT` is authorized to use Spring Boot internal DB-first context building before provider calls.
- Existing implementation path is controller -> `AiConversationService` -> `AiConversationRepository.findPromptContext(...)` -> `ProviderBackedAiConversationAssistantResponseGenerator`.
- DB-first context includes conversation, recent messages, week, tasks, member progress, and allowed retrospective summary.

## Goal
`TEAM_LEAD_CHAT` should answer like a real StudyPot team leader: natural Korean coaching, specific DB-grounded reasoning, explicit uncertainty when context is missing, and a concrete next action or follow-up question. Verify DB-first RAG/context behavior through tests and HTTP-level QA without changing locked v1 product/API/DB contracts.

## Approach
1. Add failing provider tests for human conversational contract, DB-first context coverage metadata, and missing-context follow-up behavior.
2. Make the smallest provider prompt/request-payload change in `ProviderBackedAiConversationAssistantResponseGenerator`.
3. Add test-only conditional QA wiring only if needed to exercise the real HTTP controller with `bootTestRun` and `curl`.
4. Run targeted tests, adjacent regression tests, full Gradle verification, and ulw-loop HTTP/tmux evidence capture.

## Step Plan
1. RED: run targeted provider tests and capture expected failures under `.omo/ulw-loop/evidence`.
2. GREEN: update the provider prompt operating contract and request payload metadata.
3. Regression: run AI service, JDBC repository, and controller tests.
4. Manual QA: start the test-runtime HTTP server on port `18080`, call the AI conversation message endpoint and validation/list endpoints with `curl -i`, capture artifacts, then kill the server and record cleanup.
5. Full verification: run `./gradlew check build --no-daemon`.
6. Review: request ultrawork reviewer audit before final completion.

## Done Criteria
- Targeted RED->GREEN evidence exists for new provider behavior.
- `./gradlew check build --no-daemon` exits 0.
- HTTP/tmux QA artifacts exist for happy path, edge/validation, and adjacent message-list regression.
- ulw-loop criteria are recorded with cleanup receipts.
- `EXEC_PLAN` and relevant evidence describe that DB RAG is the current DB-first context builder and vector RAG remains deferred by locked docs.
