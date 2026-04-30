# AI Study Leader AI Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.

## AI Responsibilities
| Purpose | Trigger | Output | Persistence |
| --- | --- | --- | --- |
| `DETAIL_KEYWORD_SUGGEST` | Group creation helper | Candidate detail keywords | Not persisted unless user selects them. |
| `CURRICULUM_GENERATE` | Host starts study | Curriculum, weeks, weekly tasks | `curriculum`, `curriculum_week`, `weekly_task`, `llm_usage` |
| `RETROSPECTIVE_FEEDBACK` | Week ended, incomplete modal, or user request | Feedback and next-week adjustment | `retrospective`, `llm_usage` |
| `TEAM_LEAD_CHAT` | Member chats with AI team leader | Assistant message and optional summary | `ai_conversation`, `ai_conversation_message`, `llm_usage` |

## Context Rules
- Detail keyword suggestions can use group topic and user-entered hints.
- Curriculum generation uses only onboarding responses submitted before host start.
- Late joiner onboarding can influence future retrospective/adjustment, not automatic full initial curriculum regeneration.
- Retrospective feedback uses onboarding summary, current week tasks, completion notes, incomplete reasons, and prior conversation summary.
- AI output must not expose another member's private note unless the permission contract explicitly allows group-level aggregation.

## Output Shapes
### Detail Keyword Suggestion
```json
{
  "suggestions": ["JPA", "Spring Security", "테스트 코드"],
  "rationale": "그룹 topic과 기간에 맞춘 세부 키워드입니다."
}
```

### Curriculum Generation
```json
{
  "title": "Spring Boot 6주 완성",
  "totalWeeks": 6,
  "weeks": [
    {
      "weekNumber": 1,
      "title": "JPA 기초와 환경 구성",
      "sprintGoal": "공통 환경을 만들고 핵심 개념을 맞춥니다.",
      "learningGoals": ["Entity 매핑 이해", "Repository 테스트 작성"],
      "resources": [{"title": "공식 문서", "url": "https://example.com"}],
      "tasks": [
        {"taskType": "READING", "title": "JPA 엔티티 매핑 읽기", "required": true},
        {"taskType": "PRACTICE", "title": "간단한 CRUD 구현", "required": true}
      ]
    }
  ]
}
```

### Retrospective Feedback
```json
{
  "summary": "실습 선호도가 높은 멤버가 많지만 Security 난이도가 높게 나타났습니다.",
  "strengths": ["완료율이 높음", "실습 증빙이 명확함"],
  "risks": ["마감 직전 미완료 사유가 집중됨"],
  "nextWeekAdjustment": {
    "difficulty": "slightly_lower",
    "taskChanges": ["Security 실습을 두 단계로 분리"],
    "memberNotes": [{"memberId": "uuid", "note": "기초 자료 우선 추천"}]
  }
}
```

### Team Leader Chat
```json
{
  "message": "이번 주 미완료 이유를 기준으로 다음 주 실습량을 줄이는 게 좋겠습니다.",
  "conversationSummaryPatch": "사용자는 실습 시간이 부족했고 보충 자료를 요청했습니다.",
  "nextWeekAdjustmentCandidate": {
    "taskChanges": ["필수 과제 1개를 선택 과제로 변경"]
  }
}
```

## LLM Usage Logging
- Every AI call creates `llm_usage`.
- Store provider, model, purpose, token counts, latency, status, cost if available, redacted request payload, and response summary.
- Do not store raw secrets, OAuth tokens, Discord tokens, or unredacted provider credentials.

## Failure Handling
- If curriculum generation fails, group remains `ONBOARDING` unless a partial curriculum was explicitly accepted.
- If retrospective generation fails, `retrospective.status = FAILED` and retry can create or update the same retrospective.
- If chat response fails, store the user message and failed `llm_usage`; do not invent assistant content.

## Deferred AI Scope
- Live meeting assistant.
- Voice transcription.
- Fully autonomous task reassignment without host/member-visible audit.
