# EXEC_PLAN: [ai-chat] ai_conversation 세션 구현

- Task slug: `spt-39-ai-conversation-core`
- Base branch: `develop`
- Feature branch: `codex/spt-39-ai-conversation-core`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-39-ai-conversation-core`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-39-ai-conversation-core`
- Jira issue: `SPT-39`
- Jira URL: https://studypot.atlassian.net/browse/SPT-39
- Jira summary: [ai-chat] ai_conversation 세션 구현
- Status: `planned`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md

## Related Feature IDs
- [x] ai-team-leader
- [x] retrospective-feedback

## Doc Notes
- `SPT-39` is the first implementation slice of the larger `SPT-39` to `SPT-46` AI team leader goal.
- API contract exposes `POST /api/v1/groups/{groupId}/ai-conversations` for opening an AI team leader conversation.
- `OpenConversationRequest` requires `conversationType` and supports `TEAM_LEAD_CHAT` or `RETROSPECTIVE`, with optional `weekId` and `retrospectiveId`.
- `ai_conversation` remains the chat/input interface. Retrospective-style conversations may link to `retrospective_id`.
- Permission contract says only active members and owners can chat with the AI team leader. Cross-group resource access must be rejected.
- DB-first context builder, LLM provider calls, message persistence, `llm_usage`, feedback mapping, and next-week adjustment are follow-up slices in `SPT-40` to `SPT-46`.
- FastAPI, vector store, GraphRAG, MCP, and broader agent orchestration are explicitly deferred to `SPT-82` or a later approved task.

## Goal
Implement the `SPT-39` AI conversation session foundation so an authenticated active group member can open a `TEAM_LEAD_CHAT` or `RETROSPECTIVE` AI team leader conversation inside their group, optionally scoped to a curriculum week and/or retrospective that belongs to the same group/member boundary.

This PR does not call an LLM or persist conversation messages. It establishes the session API, domain, repository, permission checks, and tests required for `SPT-40` through `SPT-46`.

## Approach
1. Add an `ai` feature package following existing controller/service/repository/domain boundaries.
2. Model `AiConversation`, `AiConversationType`, `AiConversationStatus`, and an active-member context needed to authorize opening sessions.
3. Add `AiConversationRepository` with JDBC persistence against the existing `ai_conversation` table.
4. Implement `AiConversationService.openConversation` with:
   - authenticated user and group membership lookup,
   - `ACTIVE` group and `ACTIVE` member requirement,
   - `TEAM_LEAD_CHAT` and `RETROSPECTIVE` type validation,
   - optional `weekId` ownership validation,
   - optional `retrospectiveId` ownership validation,
   - insert into `ai_conversation`.
5. Add `AiConversationController` for `POST /api/v1/groups/{groupId}/ai-conversations`.
6. Wire application and persistence configuration conditionally like the existing feature slices.
7. Extend API exception mapping for AI conversation service errors.
8. Add focused domain, service, repository, and controller tests before final verification.

## Step Plan
1. Inspect existing retrospective/curriculum/studygroup implementation patterns.
2. Add failing focused tests for conversation opening, validation, and access rejection.
3. Implement domain records/enums and command/query objects.
4. Implement repository SQL and JDBC adapter.
5. Implement service and controller wiring.
6. Update global error handling and application wiring tests if needed.
7. Run focused tests for the new AI conversation slice.
8. Run `./gradlew check build --no-daemon`.
9. Create PR through `scripts/task/create-pr.sh`, run CodeRabbit review, address actionable feedback once, and finish PR readiness notification.

## Done Criteria
- `POST /api/v1/groups/{groupId}/ai-conversations` returns `201` with `id`, `conversationType`, `status=OPEN`, and `summary`.
- Request body validation rejects missing or invalid `conversationType`.
- Non-member, pending member, left member, inactive/archived group, cross-group week, and cross-member/cross-group retrospective references are rejected.
- A valid `TEAM_LEAD_CHAT` conversation can be opened without `weekId` or `retrospectiveId`.
- A valid `RETROSPECTIVE` conversation can link to a same-member retrospective.
- The new repository stores `group_id`, `member_id`, optional `curriculum_week_id`, optional `retrospective_id`, `conversation_type`, `status`, `summary`, `opened_at`, and audit timestamps in the existing `ai_conversation` table.
- Tests cover happy path, edge cases, input validation, permission checks, and repository persistence.
- `./gradlew check build --no-daemon` passes.
