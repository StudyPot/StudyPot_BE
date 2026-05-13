# EXEC_PLAN: [ai-chat] AI 대화 응답 provider/usage 연결 보강

- Task slug: `spt-83-ai-chat-response`
- Base branch: `develop`
- Feature branch: `codex/spt-83-ai-chat-response`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-83-ai-chat-response`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-83-ai-chat-response`
- Jira issue: `SPT-83`
- Jira URL: https://studypot.atlassian.net/browse/SPT-83
- Jira summary: [ai-chat] AI 대화 응답 provider/usage 연결 보강
- Status: `in-review`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md

## Related Feature IDs
- [x] ai-team-leader
- [x] retrospective-feedback
- [x] weekly-todo

## Doc Notes
- v1 specs are LOCKED_FOR_IMPLEMENTATION; this task must not add a new API path, table, enum, field, or AI architecture boundary.
- `POST /api/v1/ai-conversations/{conversationId}/messages` is already specified to store both the user message and assistant message while returning the existing single-message response schema.
- `TEAM_LEAD_CHAT` context must be DB-first and limited to the authenticated member's visible group/week/task/progress/retrospective context.
- Provider failures must keep the user message and failed `llm_usage` record, and must not insert fake assistant content.
- LLM request payloads must stay redacted and must not include provider secrets, OAuth tokens, raw private notes outside the permission contract, or unrelated member data.
- Jira transition during bootstrap intermittently returned 503 after SPT-83 was created; task-state records the real issue key and this plan keeps the Jira link explicit.

## Goal
Complete the missing AI team-leader chat execution path: when an active member sends a message to an open AI conversation, persist the user message, build allowed DB-first context, call the configured LLM provider, record successful or failed `llm_usage`, store the assistant response on success, update the conversation summary, and preserve existing access controls.

## Approach
- Add tests first around success, provider failure, and permission boundaries for `AiConversationService`.
- Keep the public response type unchanged. On success, return the stored assistant message; when no provider/usage recorder is configured, preserve the current user-message-only fallback for local wiring compatibility.
- Introduce a small AI conversation generator abstraction and a provider-backed implementation using existing `LlmProviderClient`, `LlmStructuredRequest`, `LlmStructuredResponse`, and `LlmCallFailure` patterns.
- Extend repository reads only as needed for safe DB-first prompt context and conversation summary updates.
- Annotate the send flow so provider-generation exceptions do not roll back the already-stored user message and failed usage record.

## Step Plan
1. Add failing service/provider/repository tests for successful assistant generation, failed provider recording, summary update, and blocked unauthorized/cross-member context.
2. Add AI conversation prompt/result records and a provider-backed generator that parses structured provider JSON.
3. Extend message domain and repository SQL for assistant messages, prompt context reads, and summary updates.
4. Wire optional generator/usage recorder into `AiConversationService` and update controller/OpenAPI wording.
5. Run targeted tests, then `./gradlew check build --no-daemon`.
6. Commit, create PR with a Korean `[feat]` title, request CodeRabbit, address one review cycle if needed, and finish PR readiness notification.

## Done Criteria
- Active member + open `TEAM_LEAD_CHAT` or `RETROSPECTIVE` conversation stores a USER message, calls the provider with allowed DB-first context, stores an ASSISTANT message, records successful `TEAM_LEAD_CHAT` `llm_usage`, and updates `ai_conversation.summary`.
- Provider failure, timeout, or invalid structured output stores the USER message and failed `llm_usage`, does not store assistant content, and returns a service error instead of inventing an AI answer.
- LEFT/inactive member, closed conversation, cross-member retrospective, cross-group week, and non-member access remain blocked.
- Tests use fake provider/recorder coverage and verify request payload privacy boundaries.
- `./gradlew check build --no-daemon` passes before PR creation.
