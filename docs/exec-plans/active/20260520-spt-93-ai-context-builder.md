# EXEC_PLAN: [ai] DB-first AI context builder 검증 보강

- Task slug: `spt-93-ai-context-builder`
- Base branch: `develop`
- Feature branch: `codex/spt-93-ai-context-builder`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-93-ai-context-builder`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-93-ai-context-builder`
- Jira issue: `SPT-93`
- Jira URL: https://studypot.atlassian.net/browse/SPT-93
- Jira summary: [ai] DB-first AI context builder 검증 보강
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260519-ai-llm-rag-architecture.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] ai-contract-v1
- [x] retrospective-rag-boundary
- [x] ai-llm-rag-architecture

## Doc Notes
- `docs/specs/ai-contract-v1.md` requires the MVP retrospective/chat retrieval boundary to remain a backend DB-first context builder, with redacted request/source metadata in `llm_usage.request_payload`.
- `RETROSPECTIVE_FEEDBACK` context must include current week, weekly tasks, member progress, task completions, completion notes, incomplete reasons, group rules, rule violations, prior retrospective data, onboarding summary, and retrospective-linked conversation summary before the provider call.
- `TEAM_LEAD_CHAT` context must include conversation metadata, current group/week context, visible weekly tasks, member-owned progress/completion context, and allowed retrospective summary before the provider call.
- `ADR-20260512-retrospective-rag-boundary.md` and `ADR-20260519-ai-llm-rag-architecture.md` explicitly defer FastAPI, vector store, GraphRAG, MCP, and agent orchestration until a later approved task proves the DB-first boundary is insufficient.
- Golden-set guidance says small regression fixtures should assert required context-source keys, permission filtering/redaction, and failure audit behavior before introducing heavier RAG/eval tooling.

## Goal
Lock down the DB-first context builder contract for `RETROSPECTIVE_FEEDBACK` and `TEAM_LEAD_CHAT` with tests and small fixtures, so future AI/RAG work cannot accidentally drop required sources, leak credential-like private fields, or fabricate assistant content on provider failure.

## Approach
- Use TDD: add failing tests first for missing chat task/progress completion details and prompt redaction, observe RED, then implement the smallest production changes.
- Keep the runtime architecture unchanged: no FastAPI split, no vector DB, no RAG framework, no OpenAPI/schema/permission expansion.
- Add small JSON golden fixtures under test resources for retrospective and team-lead-chat context coverage.
- Reuse existing service/repository boundaries and `llm_usage` audit flow; only add shared prompt sanitization where provider-bound input can contain credential-like keys or values.
- Verify failure-path behavior remains unchanged: failed provider/parse calls record failed `llm_usage`, and chat failures do not insert assistant messages.

## Step Plan
1. Add RED tests for `TEAM_LEAD_CHAT` prompt SQL/source coverage, including task completion notes and incomplete reasons.
2. Add RED provider-input tests backed by small golden fixtures for `TEAM_LEAD_CHAT` and `RETROSPECTIVE_FEEDBACK`, asserting credential-like values are redacted before the provider call and from request payload metadata.
3. Implement minimal SQL/mapper additions for chat task/progress prompt context.
4. Implement shared prompt-input sanitization for nested maps/lists/strings before provider calls.
5. Run focused tests for repository/provider/service failure paths, then run `./gradlew check build --no-daemon`.
6. Commit, create PR with `scripts/task/create-pr.sh`, request CodeRabbit review, address one review loop if needed, and run `scripts/task/finish-pr.sh` for manual-merge readiness.

## Done Criteria
- `RETROSPECTIVE_FEEDBACK` provider-bound context coverage is protected by a golden-fixture regression test.
- `TEAM_LEAD_CHAT` provider-bound context coverage includes conversation/week/task/progress/retrospective source keys and member-owned completion detail fields.
- Credential-like values such as provider keys, OAuth/access tokens, authorization headers, cookies, credentials, secrets, and passwords are redacted before provider calls and do not appear in `llm_usage` request metadata.
- Existing failure behavior remains verified: failed LLM calls record failed usage and chat failure does not create assistant content.
- `./gradlew check build --no-daemon` passes in the SPT-93 worktree.
- PR review gate requirements are completed before asking the user to merge.

## Verification
- RED: `./gradlew test --tests com.studypot.aistudyleader.ai.repository.JdbcAiConversationRepositoryTest --tests com.studypot.aistudyleader.ai.service.ProviderBackedAiConversationAssistantResponseGeneratorTest --tests com.studypot.aistudyleader.retrospective.service.ProviderBackedRetrospectiveFeedbackGeneratorTest --no-daemon` failed before production changes with 3 expected failures: missing chat completion detail SQL, unredacted team-lead-chat provider input, and unredacted retrospective provider input.
- GREEN: same focused test command passed after adding chat SQL/mapper coverage and provider-bound prompt sanitization.
- Regression: `./gradlew test --tests com.studypot.aistudyleader.ai.service.AiConversationServiceTest --tests com.studypot.aistudyleader.retrospective.service.RetrospectiveServiceTest --tests com.studypot.aistudyleader.ai.repository.JdbcAiConversationRepositoryTest --tests com.studypot.aistudyleader.retrospective.repository.JdbcRetrospectiveRepositoryTest --no-daemon` passed.
- Full: `./gradlew clean check build --no-daemon` passed after clearing stale `build/classes/java/test/* 2.class` outputs that were not present in source.
- Standard: `./gradlew check build --no-daemon` passed after the clean build.
