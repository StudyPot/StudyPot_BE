# EXEC_PLAN: [planning] 회고 챗봇/RAG 적용 방향 정리

- Task slug: `spt-81-retrospective-rag-planning`
- Base branch: `develop`
- Feature branch: `codex/spt-81-retrospective-rag-planning`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-81-retrospective-rag-planning`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-81-retrospective-rag-planning`
- Jira issue: `SPT-81`
- Jira URL: https://studypot.atlassian.net/browse/SPT-81
- Jira summary: [planning] 회고 챗봇/RAG 적용 방향 정리
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/product-brief.md
- [x] docs/specs/prd-v1.md
- [x] docs/specs/user-journeys-v1.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] retrospective-feedback
- [x] ai-team-leader
- [x] weekly-todo
- [x] study-group-rules
- [x] notification

## Doc Notes
- SPT-81 is a planning/change-control slice, not a production endpoint implementation slice.
- Existing locked docs already include retrospective feedback, AI conversation, and LLM usage resources, but they do not explicitly define the boundary between deterministic DB context collection, later vector retrieval, and the AI provider adapter.
- The Jira SPT-81 direction is compatible with preserving the current API paths and DB table set: `retrospective` remains the final stateful output, `ai_conversation` remains the input/chat interface, and retrieval context is prepared immediately before LLM calls.
- Because the task changes/clarifies AI context behavior and QA expectations across locked documents, use the Change Request + ADR process rather than editing locked docs as an untracked clarification.
- SPT-82 is the follow-up research task for FastAPI split, GraphRAG, agentic RAG, MCP, BFF, and evaluation tooling. Do not pull those decisions into SPT-81 beyond noting that SPT-81 keeps the MVP backend API boundary stable.

## Goal
Document the SPT-81 retrospective chat/RAG boundary so the next backend API slices can proceed in order: create a Change Request and ADR for deterministic DB-first AI context building, update the affected v1 contracts to state the boundary, and keep SPT-38 through SPT-46 implementation order free of hidden AI/FastAPI scope.

## Approach
Keep this as a docs/change-control task. Add a CR and ADR that approve the boundary without adding endpoints, tables, enum values, or provider-specific infrastructure. Update AI, API, DB, permissions, QA, feature coverage, user journey, and Confluence draft docs so they consistently describe:
- `retrospective` as the final week/member/progress result.
- `ai_conversation` and messages as the chat/input interface that may link to a retrospective.
- a DB-first context builder that collects current week, weekly tasks, progress, task completions, incomplete reasons, group rules/violations, prior retrospectives, prior adjustments, and conversation summaries before provider calls.
- vector/graph retrieval as deferred until non-structured learning materials exist.
- owner-visible LLM/retrieval audit through redacted `llm_usage` metadata, without changing the public OpenAPI response shape in this task.

## Step Plan
1. [x] Create `CR-20260512-retrospective-rag-boundary.md` and matching ADR.
2. [x] Update `change-control-v1.md` so the new approved planning change is discoverable.
3. [x] Update locked source docs to reflect the boundary without changing endpoint paths, table set, enum values, or AI output JSON schema.
4. [x] Update Confluence draft docs to mirror the source contracts.
5. [x] Add focused docs-source validation for the new CR/ADR references if existing tests do not cover them.
6. [x] Run focused docs tests, shell harness tests, then `./gradlew check build --no-daemon`.
7. [ ] Commit and proceed through PR creation and CodeRabbit review gate.

## Progress Notes
- 2026-05-12: Created and linked `CR-20260512-retrospective-rag-boundary` and `ADR-20260512-retrospective-rag-boundary`.
- 2026-05-12: Updated source contracts and Confluence drafts to keep API/DB/OpenAPI shapes unchanged while adding the DB-first context builder boundary.
- 2026-05-12: Posted SPT-81 boundary comments to SPT-38, SPT-39, SPT-40, SPT-41, SPT-42, SPT-44, SPT-45, SPT-46, and SPT-53.
- 2026-05-12: Focused docs-source test passed with `bash scripts/tests/test_docs_source_of_truth.sh`.
- 2026-05-12: Shell harness tests passed with `bash scripts/tests/run.sh`.
- 2026-05-12: Standard verification passed with `./gradlew check build --no-daemon`.
- 2026-05-12: CodeRabbit raised one minor privacy-boundary wording issue in `auth-permissions-v1.md`; clarified conversation summary vs raw message and anonymized aggregate scope.

## Done Criteria
- Change Request and ADR exist and are linked.
- Affected docs consistently state the retrospective/chat/RAG boundary and SPT-38 through SPT-46 implementation ordering.
- No new API path, OpenAPI schema field, DB table/column/enum, or provider-specific service boundary is introduced by SPT-81.
- FastAPI/RAG alternative technology choice remains delegated to SPT-82.
- Focused docs-source tests pass.
- `./gradlew check build --no-daemon` passes before commit/PR.
