# 06 AI 팀장 명세

## AI Purposes
- `DETAIL_KEYWORD_SUGGEST`
- `CURRICULUM_GENERATE`
- `RETROSPECTIVE_FEEDBACK`
- `TEAM_LEAD_CHAT`

## Rules
- Detail keyword suggestions are exposed through `POST /api/v1/groups/detail-keyword-suggestions`, return `keywords` only, and are not stored unless selected in group creation.
- Curriculum uses onboarding responses submitted at host start.
- Retrospective uses a DB-first context builder before the LLM provider call.
- Retrospective context includes onboarding summary, current week, weekly tasks, member progress, task completions, incomplete reasons, relevant group rules/violations, prior feedback/adjustment, and conversation summary.
- `retrospective` remains the final feedback/adjustment result; `ai_conversation` is the chat/input interface and may link to a retrospective.
- `GET /api/v1/ai-conversations/{conversationId}/messages` lists messages for active conversation members and reconnect recovery.
- `GET /api/v1/ai-conversations/{conversationId}/stream` sends 1:1 SSE lifecycle events: `user-message-saved`, `assistant-generation-started`, `assistant-message-created`, and `assistant-generation-failed`.
- `POST /api/v1/ai-conversations/{conversationId}/messages` remains synchronous; SSE mirrors lifecycle events for subscribed clients.
- AI team leader proposes next-week adjustments every week for difficulty, task split, support material, and coaching notes.
- Late joiner onboarding can affect future adjustments, not automatic full curriculum regeneration.
- All AI calls create `llm_usage`.
- `llm_usage.request_payload` stores redacted request/source metadata for audit. Raw secrets, OAuth tokens, provider credentials, and disallowed private raw notes are never stored in AI request logs.
- Redis is short-lived protection state for rate limits and TTL duplicate locks. RabbitMQ is async dispatch state for later worker jobs. MySQL remains the durable source for AI outputs and `llm_usage`.
- Vector store, GraphRAG, MCP, FastAPI service split, and broader agent orchestration are deferred to SPT-82 or later approved tasks. SPT-82's proposed default keeps MVP AI inside Spring Boot with DB-first context building and lists transition criteria for later FastAPI/RAG adoption.

## Source
- `docs/specs/ai-contract-v1.md`
- `docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md`
- `docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md`
- `docs/specs/change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md`
- `docs/specs/adr/ADR-20260519-redis-rabbitmq-realtime-infra.md`
- `docs/specs/adr/ADR-20260519-ai-llm-rag-architecture.md`
- `docs/specs/change-requests/CR-20260520-detail-keyword-suggestion-api.md`
- `docs/specs/adr/ADR-20260520-detail-keyword-suggestion-api.md`
- `docs/specs/change-requests/CR-20260601-ai-conversation-sse-stream.md`
- `docs/specs/adr/ADR-20260601-ai-conversation-sse-stream.md`
