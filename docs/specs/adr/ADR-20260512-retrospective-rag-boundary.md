# ADR-20260512 Retrospective RAG Boundary

## Status
- Approved

## Context
- The locked v1 plan includes retrospective feedback, AI team leader chat, `llm_usage`, and JSON fields for feedback, adjustment, and request metadata.
- The plan did not explicitly define whether RAG means vector retrieval, SQL retrieval, a separate FastAPI service, or an LLM agent layer.
- Current MVP retrospective context lives primarily in structured tables: onboarding summary, curriculum week, weekly tasks, member week progress, task completions, incomplete reasons, group rules, rule violations, prior retrospectives, prior adjustments, and conversation summaries.
- The implementation sequence should finish backend APIs and persistence boundaries before adding provider-specific LLM modeling.

## Decision
- Use a DB-first context builder as the MVP retrieval boundary for `RETROSPECTIVE_FEEDBACK` and `TEAM_LEAD_CHAT`.
- The context builder collects permission-filtered, deterministic StudyPot data from existing backend repositories before the provider adapter call.
- `retrospective` remains the final stateful feedback and next-week adjustment result.
- `ai_conversation` remains the chat/input interface; retrospective-style conversations may link to `retrospective_id`.
- `llm_usage.request_payload` stores redacted request metadata, including summarized context/source metadata where useful for audit, without storing secrets, OAuth tokens, provider credentials, or excessive private raw notes.
- Keep OpenAPI paths, response schemas, DB tables, columns, enums, and MVP permission actions unchanged for SPT-81.
- Defer vector store, GraphRAG, MCP, FastAPI service split, and broader agent orchestration decisions to SPT-82 or later implementation tasks.

## Consequences
- Positive: SPT-38 can implement retrospective creation/state without depending on LLM or vector infrastructure.
- Positive: SPT-39 and SPT-40 can implement conversation/session/message persistence before model calls.
- Positive: SPT-41, SPT-44, and SPT-46 can add feedback and adjustment mapping against a stable context-builder boundary.
- Positive: Privacy and audit behavior remains inside the Spring Boot backend where membership, group, and owner permissions already live.
- Negative: Unstructured study-material retrieval is intentionally deferred until the product has real document/link sources to index.
- Negative: A later FastAPI or graph/vector retrieval split will require another task and, if it changes contracts, another CR/ADR.
- Migration or compatibility notes: No migration required. This is a pre-implementation contract clarification.

## Affected Feature IDs
- `retrospective-feedback`
- `ai-team-leader`
- `weekly-todo`
- `study-group-rules`
- `notification`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/db-contract-v1.md`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/specs/user-journeys-v1.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/06-ai-team-leader.md`

## Linked Change Request
- [CR-20260512-retrospective-rag-boundary](../change-requests/CR-20260512-retrospective-rag-boundary.md)
