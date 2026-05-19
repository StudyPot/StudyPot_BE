# AI Study Leader AI Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.
- Retrospective/chat DB-first context boundary is authorized by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md) and [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md).
- Redis/RabbitMQ runtime boundaries are authorized by [CR-20260519-redis-rabbitmq-realtime-infra](./change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md) and [ADR-20260519-redis-rabbitmq-realtime-infra](./adr/ADR-20260519-redis-rabbitmq-realtime-infra.md).

## AI Responsibilities
| Purpose | Trigger | Output | Persistence |
| --- | --- | --- | --- |
| `DETAIL_KEYWORD_SUGGEST` | Group creation helper | Candidate detail keywords | Not persisted unless user selects them. |
| `CURRICULUM_GENERATE` | Host starts study | Curriculum, weeks, weekly tasks | `curriculum`, `curriculum_week`, `weekly_task`, `llm_usage` |
| `RETROSPECTIVE_FEEDBACK` | Week ended, incomplete modal, or user request | Feedback and next-week adjustment | `retrospective`, `llm_usage` |
| `TEAM_LEAD_CHAT` | Member chats with AI team leader | Assistant message and optional summary | `ai_conversation`, `ai_conversation_message`, `llm_usage` |

## Operating Role
- The AI team leader is a weekly operator, not a one-time curriculum generator.
- After every week, it reviews todo completion, incomplete reasons, member notes, and conversation summaries.
- It proposes `next_week_adjustment` for difficulty, task split, optional/required work, support material, and member-specific coaching notes.
- Host/member-visible audit data must explain what context was used for the adjustment.

## Context Rules
- Detail keyword suggestions can use group topic and user-entered hints.
- Curriculum generation uses only onboarding responses submitted before host start.
- Late joiner onboarding can influence future retrospective/adjustment, not automatic full initial curriculum regeneration.
- Retrospective feedback uses onboarding summary, current week tasks, completion notes, incomplete reasons, and prior conversation summary.
- Weekly adjustment can use late joiner onboarding only for future weeks.
- AI output must not expose another member's private note unless the permission contract explicitly allows group-level aggregation.

## DB-First Context Builder
- MVP retrospective/chat retrieval is a backend context builder, not a separate vector service.
- For `RETROSPECTIVE_FEEDBACK`, collect the authenticated member's current week, weekly tasks, member week progress, task completions, completion notes, incomplete reasons, relevant group rules, rule violations, prior retrospective feedback, prior next-week adjustments, onboarding summary, and retrospective-linked conversation summary before the provider call.
- For `TEAM_LEAD_CHAT`, collect the conversation, current group/week context, visible weekly tasks, the member's own progress/completion context, and allowed retrospective summary before the provider call.
- Store the final summarized input in `retrospective.input_summary` when a retrospective is created or updated.
- Store redacted request/source metadata in `llm_usage.request_payload` for audit. Do not store secrets, OAuth tokens, provider credentials, or raw private notes that exceed the permission contract.
- Vector store, GraphRAG, MCP, FastAPI service split, and broader agent orchestration are deferred to SPT-82 or later approved tasks.
- SPT-82 planning guidance is recorded in [ADR-20260519-ai-llm-rag-architecture](./adr/ADR-20260519-ai-llm-rag-architecture.md): the MVP default remains Spring Boot internal DB-first context building, with FastAPI/vector/graph/agent/MCP adoption gated by explicit transition criteria.

## Runtime Infrastructure Boundary
- MySQL remains the durable source for AI outputs, conversation records, retrospective state, curriculum state, and `llm_usage` audit.
- Redis may protect costly AI paths with short-lived rate limit counters and TTL-based duplicate generation locks, but Redis does not store final AI results or durable audit state.
- RabbitMQ may dispatch asynchronous AI jobs in a later task, but the worker must persist success, failure, idempotency, retry, and audit outcomes back to MySQL.
- A separate worker container, FastAPI service split, vector store, GraphRAG, MCP, and broader agent orchestration remain deferred until separately approved.

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
- Do not store raw secrets, OAuth tokens, or unredacted provider credentials.

## Failure Handling
- If curriculum generation fails, group remains `ONBOARDING` unless a partial curriculum was explicitly accepted.
- If retrospective generation fails, `retrospective.status = FAILED` and retry can create or update the same retrospective.
- If chat response fails, store the user message and failed `llm_usage`; do not invent assistant content.

## Deferred AI Scope
- Live meeting assistant.
- Voice transcription.
- Fully autonomous task reassignment without host/member-visible audit.
