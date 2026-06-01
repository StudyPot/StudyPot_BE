# CR-20260601-ai-conversation-sse-stream

## Status
- Approved

## Request
- Add authenticated `GET /api/v1/ai-conversations/{conversationId}/stream` for 1:1 AI team leader conversation SSE subscription.
- Emit message lifecycle events for clients that subscribe before sending a message:
  - `user-message-saved`
  - `assistant-generation-started`
  - `assistant-message-created`
  - `assistant-generation-failed`
- Expose existing conversation message listing as authenticated `GET /api/v1/ai-conversations/{conversationId}/messages` for reconnect recovery.
- Keep the existing synchronous `POST /api/v1/ai-conversations/{conversationId}/messages` request/response contract backward compatible.
- Keep access limited to active members who can access the conversation.

## Reason
- The locked v1 API stores AI team leader user/assistant messages, but the user experience is request/response-only.
- SPT-122 requires a chat-like realtime reception path while keeping the current 1:1 AI team leader model and DB-first context boundary.
- SSE gives app-open realtime updates without introducing WebSocket/STOMP, group chat, external push channels, or schema changes.

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

## Impact
- Product: Clients can render AI response progress and final assistant messages in real time while keeping the existing send endpoint.
- API: Adds one SSE stream endpoint and one read-only message-list endpoint under the existing AI conversation resource.
- DB: No table, column, enum, constraint, or migration change.
- AI: No provider/model/prompt quality change. DB-first context building and `llm_usage` audit remain unchanged.
- Notification: None.
- Permissions: Uses the existing active conversation member boundary. Cross-user conversation streams and message lists are rejected.
- QA: Adds SSE subscribe, cross-user rejection, message-list recovery, success/failure event publishing, stream send-failure isolation, and cleanup coverage.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session for SPT-122.
- Date: 2026-06-01
- Linked ADR: [ADR-20260601-ai-conversation-sse-stream](../adr/ADR-20260601-ai-conversation-sse-stream.md)
