# EXEC_PLAN: [retrospective] 회고/AI 대화 권한/조회 정책 구현

- Task slug: `spt-45-retrospective-ai`
- Base branch: `develop`
- Feature branch: `codex/spt-45-retrospective-ai`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-45-retrospective-ai`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-45-retrospective-ai`
- Jira issue: `SPT-45`
- Jira URL: https://studypot.atlassian.net/browse/SPT-45
- Jira summary: [retrospective] 회고/AI 대화 권한/조회 정책 구현
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md

## Related Feature IDs
- [x] retrospective-feedback
- [x] ai-team-leader

## Doc Notes
- `auth-permissions-v1.md` requires active members to request retrospectives/chat, members to read only their own retrospective/conversation records, owners only to read group LLM usage logs, and cross-group access to be rejected even when resource IDs exist.
- `ai-contract-v1.md` and the approved retrospective RAG CR/ADR keep the DB-first context builder inside Spring Boot and require context/source metadata to avoid cross-member private raw notes and secrets.
- `api-contract-v1.md` and `openapi.yaml` already define the retrospective, AI conversation, and group LLM usage endpoints. SPT-45 must not add or change endpoint shapes.
- `qa-acceptance-v1.md` requires retrospective/chat context-builder privacy coverage and own-retrospective-only access.

## Goal
Implement and verify the SPT-45 permission/privacy slice for existing retrospective, AI conversation, and LLM usage flows without changing the locked API/DB contract.

## Approach
- Keep API shapes unchanged.
- Add RED tests around raw AI conversation message access, LEFT member denial, cross-member/cross-group ID substitution, owner-only LLM usage access, and request-payload redaction.
- Strengthen AI conversation message access so raw message listing requires an active member of the conversation.
- Tighten JDBC access-context SQL so conversation `group_id` and member `group_id` must match, and retrospective references are resolved through the same group membership boundary.
- Preserve existing retrospective and LLM usage behavior where tests already prove owner/self-only access.

## Step Plan
1. Add focused failing tests for SPT-45 gaps in AI conversation service/repository/controller, retrospective controller/service, LLM usage controller/service, and LLM usage redaction.
2. Implement only the missing permission guards and SQL predicates needed for those tests.
3. Run targeted tests for AI conversation, retrospective, and LLM usage.
4. Run `./gradlew check build --no-daemon`.
5. Commit, create PR, run CodeRabbit review, address valid feedback once, and finish the PR gate.

## Done Criteria
- LEFT members cannot open a new AI conversation or request a retrospective; tests cover service/controller behavior.
- Raw AI conversation messages are listable only after active membership for the conversation is confirmed.
- Cross-member retrospective IDs and cross-group week/group IDs cannot be used to open AI conversations.
- Retrospective read/request remains scoped to authenticated member's own week progress and retrospective.
- Only group owners can read `GET /api/v1/groups/{groupId}/llm-usage`; non-owner, LEFT owner, missing group, and cross-group users are rejected.
- `llm_usage.request_payload` redacts token/OAuth/provider-key-style fields and truncates excessive private note text, including nested payload values.
- Repository SQL/tests show access-context queries are scoped by group, member, user, soft-delete state, and stable resource ownership.
- `./gradlew check build --no-daemon` passes.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.ai.*' --tests 'com.studypot.aistudyleader.retrospective.service.RetrospectiveServiceTest' --tests 'com.studypot.aistudyleader.retrospective.controller.RetrospectiveControllerTest' --tests 'com.studypot.aistudyleader.llm.*' --no-daemon` failed on missing message-read active-member guard, missing AI conversation ownership SQL predicates, and providerKey redaction.
- GREEN: same targeted command passed after implementation.
- Standard: `./gradlew check build --no-daemon` passed.
