# CR-20260512-retrospective-rag-boundary

## Status
- Approved

## Request
- Clarify the MVP boundary between `retrospective`, `ai_conversation`, DB-first AI context building, and later vector/graph retrieval.
- Keep `retrospective` as the final stateful feedback result keyed by week/member/progress context.
- Keep `ai_conversation` and `ai_conversation_message` as the member chat/input interface that can optionally connect to a retrospective.
- Add a deterministic DB-first context builder step immediately before `RETROSPECTIVE_FEEDBACK` and `TEAM_LEAD_CHAT` provider calls.
- Defer vector store, GraphRAG, MCP, FastAPI service split, and broader agent orchestration decisions to SPT-82 unless a later implementation task explicitly approves them.

## Reason
- The locked v1 docs already define retrospective, AI conversation, and LLM usage resources, but they do not explicitly define how retrieved context is selected before LLM calls.
- SPT-38 through SPT-46 need an implementation order that finishes backend API and persistence foundations before adding model/provider behavior.
- Current StudyPot retrospective data is mostly structured and permission-sensitive, so SQL-backed deterministic context collection should precede general vector retrieval.

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

## Impact
- Product: Clarifies that retrospective feedback is the output, while AI chat is an input/support surface.
- API: No new endpoint, path, request field, response field, or enum is added by this change.
- DB: No table, column, enum, constraint, or migration change is required. Existing JSON fields carry summarized context and redacted metadata.
- AI: Adds the DB-first context builder contract for retrospective/chat LLM calls and defers vector/graph retrieval until unstructured study material exists.
- Notification: No notification type or delivery behavior change. Existing retrospective/AI feedback notification behavior remains.
- Permissions: No role matrix change. Context collection must preserve existing member/owner visibility and avoid exposing another member's private note unless explicitly allowed by aggregation rules.
- QA: Adds expectations that retrospective/chat tests verify deterministic context source selection, redaction, failed generation handling, and cross-member/cross-group privacy.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session to start SPT-81 before AI LLM modeling and separate backend API completion from AI technology research.
- Date: 2026-05-12
- Linked ADR: [ADR-20260512-retrospective-rag-boundary](../adr/ADR-20260512-retrospective-rag-boundary.md)
