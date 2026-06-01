# EXEC_PLAN: [feat] AI 팀장 대화 SSE 실시간 응답 추가

- Task slug: `spt-122-ai-chat-sse-stream`
- Base branch: `develop`
- Feature branch: `codex/spt-122-ai-chat-sse-stream`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-122-ai-chat-sse-stream`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-122-ai-chat-sse-stream`
- Jira issue: `SPT-122`
- Jira URL: https://studypot.atlassian.net/browse/SPT-122
- Jira summary: [ai-team-leader] 1:1 AI 팀장 대화 SSE 실시간 응답 수신 추가
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-request-template.md
- [x] docs/specs/adr-template.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] `ai-team-leader`

## Doc Notes
- `ai-contract-v1.md` keeps `TEAM_LEAD_CHAT` DB-first context building inside Spring Boot and stores conversation messages plus `llm_usage` in MySQL.
- `auth-permissions-v1.md` requires active group membership for AI team leader chat and prevents cross-member private context leakage.
- `change-control-v1.md` requires Change Request + ADR because this task adds API paths and realtime AI delivery behavior.
- `SPT-122` excludes WebSocket/STOMP, AI group chat, DB schema changes, external push channels, and model/prompt quality changes.

## Goal
Add a 1:1 AI team leader conversation SSE stream so an authenticated active conversation member can receive message lifecycle events while preserving the existing synchronous `POST /api/v1/ai-conversations/{conversationId}/messages` contract. Expose the existing message-list service as a read API so clients can reconcile missed events after reconnect.

## Approach
1. Record the locked-spec API/AI/permission change with a CR/ADR pair.
2. Add RED tests for stream subscription auth, cross-user rejection, message list recovery, stream event publishing on user save/assistant start/assistant success/failure, send-failure isolation, and cleanup.
3. Add a Spring MVC `SseEmitter` stream service keyed by `conversationId`.
4. Add a service-layer stream publisher port so `AiConversationService` publishes events without depending on web infrastructure.
5. Keep `POST /messages` synchronous and backward compatible; SSE mirrors lifecycle events for clients that subscribed before sending.
6. Publish `user-message-saved`, `assistant-generation-started`, `assistant-message-created`, and `assistant-generation-failed` best-effort. Stream send failures must not roll back user message persistence, `llm_usage`, assistant message persistence, or the original HTTP response.
7. Concurrent sends remain accepted in MVP. Event order is per service execution and clients should correlate by message id; a Redis or DB-backed per-conversation generation lock is deferred unless later product work requires single-flight chat generation.
8. Update API/OpenAPI/AI/auth/QA/coverage/confluence docs.

## Step Plan
1. [x] Add CR/ADR docs for AI conversation SSE and message-list recovery.
2. [x] Add failing controller/service/stream tests.
3. [x] Implement DTO extraction, list endpoint, stream endpoint, stream service, and stream publisher integration.
4. [x] Update locked specs and confluence draft docs.
5. [x] Run focused tests, then `./gradlew check build --no-daemon`.
6. [ ] Create PR, run CodeRabbit review, address one review round if needed, wait for review gate, finish auto-merge/cleanup.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.ai.controller.AiConversationControllerTest' --tests 'com.studypot.aistudyleader.ai.controller.AiConversationStreamServiceTest' --tests 'com.studypot.aistudyleader.ai.service.AiConversationServiceTest' --no-daemon` failed before implementation because `AiConversationStreamConnection` and `AiConversationStreamPublisher` did not exist.
- GREEN focused: same focused AI conversation test command passed after implementation.
- GREEN full: `./gradlew check build --no-daemon` passed.

## Done Criteria
- `GET /api/v1/ai-conversations/{conversationId}/stream` is authenticated, returns `text/event-stream`, and rejects non-members.
- `GET /api/v1/ai-conversations/{conversationId}/messages` returns a cursor page of messages for reconnect recovery and rejects non-members.
- Sending a user message emits `user-message-saved`.
- Provider-backed generation emits `assistant-generation-started` and `assistant-message-created` when successful.
- Provider failure records failed `llm_usage`, keeps the user message, emits `assistant-generation-failed`, and preserves the existing exception behavior.
- SSE send failures and disconnected clients do not roll back message persistence or AI usage logging.
- Completion, timeout, and send errors remove stream registrations.
- Focused tests and `./gradlew check build --no-daemon` pass.
