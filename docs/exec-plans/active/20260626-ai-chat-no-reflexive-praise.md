# EXEC_PLAN: [fix] AI 팀장 채팅이 무조건 칭찬/공감으로 시작하는 문제

- Task slug: `ai-chat-no-reflexive-praise`
- Base branch: `develop`
- Feature branch: `codex/ai-chat-no-reflexive-praise`
- Worktree: `/Users/hyunwoo/Developer/Projects/StudyPot`
- Port: `TBD`
- Log dir: `/Users/hyunwoo/Developer/Projects/StudyPot/.codex/logs/ai-chat-no-reflexive-praise`
- Jira issue: ``
- Jira URL:
- Jira summary:
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md
- [x] src/main/java/com/studypot/aistudyleader/ai/service/ProviderBackedAiConversationAssistantResponseGenerator.java

## Related Docs
- [x] docs/specs/ai-contract-v1.md (TEAM_LEAD_CHAT 출력 계약 — message/summary/adjustment 스키마만 규정, 톤/말투는 미규정)
- [x] src/test/java/com/studypot/aistudyleader/ai/service/ProviderBackedAiConversationAssistantResponseGeneratorTest.java

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- ai-contract-v1.md: TEAM_LEAD_CHAT 계약은 출력 JSON 스키마(message, conversationSummaryPatch, nextWeekAdjustmentCandidate)만 정의하고 인삿말/공감 톤은 규정하지 않는다. 본 변경은 시스템 프롬프트의 톤 보정(언제 공감할지)만 바꾸고 출력 스키마/API 계약은 그대로이므로 LOCKED AI 계약 변경에 해당하지 않는다.
- AGENTS.md/ARCHITECTURE.md: 계층 규칙 위반 없음(서비스 레이어의 프롬프트 문자열만 수정). 버그 수정은 동작을 고정하는 테스트를 동반한다.

## Goal
AI 팀장 채팅이 상황과 무관하게(예: TODO 미완료 상태에서도) 무조건 칭찬/공감으로 말을 시작하는 문제를 고쳐, 공감은 멤버가 막히거나 지쳐 보일 때만 하고 하지 않은 진도를 칭찬하지 않도록 한다.

## Approach
- 근본 원인: `teamLeaderOperatingContract`의 `messageMustInclude`에 "acknowledge the member's feeling briefly"가 **무조건 포함 항목**으로 박혀, 조건부 지시(시스템 프롬프트 L56)를 덮어써 모든 응답이 공감/칭찬으로 시작됨.
- 수정:
  - `messageMustInclude`에서 무조건 공감 항목 제거.
  - `openingRule` 추가: 반사적 칭찬/공감 금지, 멤버가 막힘/지침/하소연일 때만 짧게 공감, 하지 않은 진도는 칭찬 금지(반말 유지).
  - 시스템 프롬프트(INSTRUCTIONS)의 공감 지시도 "ONLY when stuck/worried/tired/venting + 반사적 칭찬 금지 + 안 한 진도 칭찬 금지"로 강화.
- 그라운딩 정책(공급된 사실만 인정)과 일관: 미완료 과제를 완료처럼 칭찬하지 않음.

## Step Plan
1. 프롬프트 빌더(ProviderBackedAiConversationAssistantResponseGenerator)에서 무조건 공감 지시 위치 확인.
2. INSTRUCTIONS L56 강화 + `messageMustInclude` 무조건 공감 제거 + `openingRule` 추가.
3. 프롬프트 계약 검증 테스트 갱신(제거 문자열 → "Do NOT reflexively open with praise" 단언, 회귀 방지).
4. `./gradlew check build` 통과 후 PR → develop.

## Done Criteria
- `./gradlew check build` 통과.
- 계약 문자열에 무조건 공감 지시가 사라지고 반사적 칭찬 금지 규칙이 포함됨(테스트로 고정).
- 출력 스키마/API 계약 변경 없음.
