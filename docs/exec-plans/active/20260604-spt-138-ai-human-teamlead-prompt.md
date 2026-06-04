# EXEC_PLAN: AI팀장 채팅 내부 DB 표현 노출 방지 프롬프트 보강

- Task slug: `spt-138-ai-human-teamlead-prompt`
- Base branch: `develop`
- Feature branch: `codex/spt-138-ai-human-teamlead-prompt`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-138-ai-human-teamlead-prompt`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-138-ai-human-teamlead-prompt`
- Jira issue: `SPT-138`
- Jira URL: https://studypot.atlassian.net/browse/SPT-138
- Jira summary: AI팀장 채팅 내부 DB 표현 노출 방지 프롬프트 보강
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] ai-team-leader

## Doc Notes
- `TEAM_LEAD_CHAT` uses DB-first context before the provider call, but the member-facing message should sound like a human study team lead, not reveal the retrieval/audit mechanism.
- The user explicitly rejected phrases like `내가 DB에서 확인한 바로는` and `지금 바로 다음 액션 하나만 하자`.
- User decision: do not recommend next actions unless the member asks for a recommendation, asks what to do next, or asks how to proceed.
- This task does not change API shape, DB schema, persistence, permissions, or provider output schema.

## Goal
Make AI team leader chat keep DB-first grounding internally while answering like a real human team lead: no `DB/RAG/context` provenance language in member-facing messages, and no unsolicited next-action recommendations.

## Approach
Update the `TEAM_LEAD_CHAT` provider instructions and operating contract. Replace always-on `recommended next action` language with a conditional next-action policy. Add a small member-facing output sanitizer for obvious internal provenance prefixes as a safety net. Add provider tests that assert the new prompt contract and sanitizer behavior.

## Step Plan
1. Update prompt instructions to hide internal DB/RAG/context wording from member-facing messages.
2. Update operating contract to allow next-action recommendations only on explicit user request.
3. Add regression tests for natural human team lead language, internal provenance phrase suppression, and unrequested next-action policy.
4. Run targeted provider tests.
5. Run `./gradlew check build --no-daemon`, then PR/review gate/merge/deploy.

## Done Criteria
- Provider instructions forbid member-facing phrases such as `내가 DB에서 확인한 바로는`, `DB 기준`, `DB-first`, `RAG`, and `컨텍스트`.
- Provider instructions forbid `지금 바로 다음 액션 하나만 하자` and unsolicited next-action recommendations.
- DB-first grounding and missing-context safety remain intact.
- Targeted tests and full Gradle verification pass.
- PR, CodeRabbit review marker, GitHub Actions Review Gate, merge, cleanup, and deployment complete.

## Verification
- [x] `./gradlew test --tests 'com.studypot.aistudyleader.ai.service.ProviderBackedAiConversationAssistantResponseGeneratorTest' --no-daemon` - PASS
- [x] `./gradlew check build --no-daemon` - PASS
