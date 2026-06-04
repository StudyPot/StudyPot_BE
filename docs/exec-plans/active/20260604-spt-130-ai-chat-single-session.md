# EXEC_PLAN: [fix] AI 팀장 일반 대화 단일 세션 재사용

- Task slug: `spt-130-ai-chat-single-session`
- Base branch: `develop`
- Feature branch: `codex/spt-130-ai-chat-single-session`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-130-ai-chat-single-session`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-130-ai-chat-single-session`
- Jira issue: `SPT-130`
- Jira URL: https://studypot.atlassian.net/browse/SPT-130
- Jira summary: [ai] AI 팀장 일반 대화 단일 세션 재사용
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260601-ai-conversation-sse-stream.md
- [x] docs/specs/adr/ADR-20260601-ai-conversation-sse-stream.md
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] ai-team-leader

## Doc Notes
- `POST /api/v1/groups/{groupId}/ai-conversations` is the existing public "open AI team leader conversation" entrypoint.
- `GET /api/v1/ai-conversations/{conversationId}/messages` is the approved reconnect recovery path for a known conversation.
- Current v1 contracts do not require a new endpoint or DB schema change for this fix.
- The user decision for SPT-130 is that a member has one ordinary AI team leader chat in the current group; retrospective-linked conversations remain separate.
- Avoiding a DB constraint keeps this task inside the approved API/DB shape. The service will reuse an existing open `TEAM_LEAD_CHAT` with no week or retrospective scope before creating a new row.

## Goal
Make the AI team leader page behave like a normal persistent chat: entering the page should recover the existing ordinary `TEAM_LEAD_CHAT` for the authenticated member/group and load previous messages instead of starting a fresh conversation each time.

## Approach
- Add a repository lookup for the latest open ordinary `TEAM_LEAD_CHAT` scoped by `group_id`, `member_id`, `conversation_type = TEAM_LEAD_CHAT`, `status = OPEN`, and null week/retrospective references.
- Update `AiConversationService.openConversation` to return that existing conversation for ordinary team leader chat requests after membership validation and request validation.
- Keep `RETROSPECTIVE` and week/retrospective-scoped conversations on the existing create path.
- Update the frontend AI page to open/recover the conversation on page entry and then load the approved message-list recovery endpoint.
- Cover the backend change with a failing service reproduction test plus JDBC SQL/repository tests; cover the frontend behavior with a component test that renders recovered messages on mount.

## Step Plan
1. Write backend reproduction tests for ordinary `TEAM_LEAD_CHAT` reuse and repository lookup filtering.
2. Implement the backend repository query and service reuse branch.
3. Run targeted backend tests, then update frontend in its own branch/worktree.
4. Write frontend component test for automatic conversation recovery and message rendering.
5. Run targeted and full backend/frontend verification.
6. Commit, open PRs, run CodeRabbit/review gates, merge, and clean up.

## Done Criteria
- Reopening the AI team leader page for the same authenticated group member returns the existing open ordinary conversation instead of inserting a new one.
- Existing messages are loaded through `GET /api/v1/ai-conversations/{conversationId}/messages` without requiring the user to press "start conversation".
- Retrospective-linked AI conversations remain separate from ordinary team leader chat.
- Backend targeted tests and `./gradlew check build --no-daemon` pass.
- Frontend targeted unit test and build pass.
- Backend and frontend PRs pass review gates and are merged/cleaned up.

## Verification
- [x] `./gradlew test --tests 'com.studypot.aistudyleader.ai.service.AiConversationServiceTest.openTeamLeadChatReusesExistingOpenConversationForMember' --tests 'com.studypot.aistudyleader.ai.repository.JdbcAiConversationRepositoryTest.findOpenTeamLeadConversationQueriesGroupAndMember' --no-daemon`
- [x] `./gradlew test --tests 'com.studypot.aistudyleader.ai.service.AiConversationServiceTest' --tests 'com.studypot.aistudyleader.ai.repository.JdbcAiConversationRepositoryTest' --tests 'com.studypot.aistudyleader.ai.controller.AiConversationControllerTest' --no-daemon`
- [x] `./gradlew check build --no-daemon`
- [x] Frontend `npm run test:unit -- --run src/pages/group-workspace/ui/__tests__/GroupAiPage.spec.ts`
- [x] Frontend `npm run test:unit -- --run`
- [x] Frontend `npm run build`
- [x] Browser smoke at `http://127.0.0.1:5174/groups/018f7a4e-0000-7000-9000-000000000011/ai` confirmed recovered MSW chat messages render on page entry.
