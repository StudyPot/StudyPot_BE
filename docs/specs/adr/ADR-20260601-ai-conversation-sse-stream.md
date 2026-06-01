# ADR-20260601 AI Conversation SSE Stream

## Status
- Approved

## Context
- The MVP AI team leader conversation stores user messages, assistant messages, optional conversation summaries, and `llm_usage` in MySQL.
- The current public send endpoint is synchronous: user message persistence and assistant generation happen during `POST /messages`.
- SPT-122 needs a realtime, chat-like receive path for 1:1 AI team leader conversations without changing the DB-first context builder or adding group chat.
- The existing service already supports cursor-based message listing, but that read path is not exposed through the controller.

## Decision
- Add authenticated `GET /api/v1/ai-conversations/{conversationId}/stream` using Spring MVC SSE.
- Add authenticated `GET /api/v1/ai-conversations/{conversationId}/messages` for cursor-based reconnect recovery.
- Keep `POST /api/v1/ai-conversations/{conversationId}/messages` synchronous and backward compatible.
- Key active SSE connections by `conversationId` after service-layer access validation.
- Emit `connected` when the stream opens.
- Emit `user-message-saved` after the user message row is persisted.
- Emit `assistant-generation-started` immediately before provider-backed assistant generation begins.
- Emit `assistant-message-created` after the assistant message row is persisted.
- Emit `assistant-generation-failed` after failed `llm_usage` is recorded and before the existing generation exception is rethrown.
- Treat SSE publication as best-effort. Stream send failures must not roll back user message persistence, assistant message persistence, conversation summary updates, `llm_usage`, or the synchronous HTTP response.
- Clean up stream registrations on completion, timeout, send failure, or emitter error.
- Keep concurrent sends accepted for MVP. The server records independent user messages and assistant generations; a single-flight Redis/DB lock is deferred to a later approved task if product policy changes.
- Defer WebSocket/STOMP, AI group chat/public room context, DB schema changes, external push channels, model/provider changes, and worker/service split choices to later approved tasks.

## Consequences
- Positive: Existing HTTP clients keep working without adopting SSE.
- Positive: Subscribed frontend clients can render user-message acknowledgement, generation progress, final assistant messages, and failure states.
- Positive: Reconnect recovery is possible through the message list endpoint backed by existing service logic.
- Positive: MySQL remains the durable source of truth for messages, summaries, and AI audit records.
- Negative: SSE is in-process; if the API is horizontally scaled later, cross-instance fan-out requires Redis Pub/Sub, Redis Streams, broker fan-out, or another approved realtime layer.
- Negative: Concurrent sends may interleave events. Clients should correlate events by message id and continue to use message-list reconciliation.
- Migration or compatibility notes: No DB migration, enum change, prompt shape change, or provider change is required.

## Affected Feature IDs
- `ai-team-leader`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/06-ai-team-leader.md`
- `docs/confluence/07-permissions-state.md`
- `docs/confluence/09-qa-acceptance.md`
- `docs/confluence/10-jira-mapping.md`

## Linked Change Request
- [CR-20260601-ai-conversation-sse-stream](../change-requests/CR-20260601-ai-conversation-sse-stream.md)
