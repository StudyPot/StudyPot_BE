# AI Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Provider/model: configurable.
- Output schemas: locked.
- Changes require Change Request and ADR.

## Provider-Neutral Rules
- The backend stores every AI call in `ai_prompt_runs`.
- `provider`, `model`, and `prompt_version` are runtime/configuration values.
- AI input snapshots must be redacted before persistence.
- Raw provider secrets, OAuth tokens, Discord bot tokens, and private credentials are never persisted in AI payloads.
- AI generation failure must create a failed `ai_prompt_runs` row with redacted `error_code` and `error_message`.

## Prompt Versions
| Prompt | Run Type | Version | Output |
| --- | --- | --- | --- |
| Session preparation | `preparation_brief` | `prep-brief-v1` | `PreparationBriefV1` |
| Session feedback | `feedback_report` | `feedback-report-v1` | `FeedbackReportV1` |
| Action extraction | `action_item_extraction` | `action-items-v1` | `ActionItemExtractionV1` |

## Input Snapshot Contract
`ai_prompt_runs.input_snapshot` stores a redacted object:

```json
{
  "group": {
    "id": "uuid",
    "name": "Algorithm Study",
    "rules": {},
    "scheduleDefaults": {}
  },
  "session": {
    "id": "uuid",
    "title": "Week 3 DP",
    "scheduledStartAt": "2026-05-02T10:00:00Z"
  },
  "members": [
    {
      "memberId": "uuid",
      "role": "member",
      "displayName": "member-1"
    }
  ],
  "notes": [],
  "goals": [],
  "actionItems": [],
  "resources": []
}
```

## `PreparationBriefV1`
Stored in `ai_preparation_briefs`.

```json
{
  "schemaVersion": "PreparationBriefV1",
  "title": "Week 3 준비 브리프",
  "summary": "이번 세션은 DP 패턴 복습에 집중합니다.",
  "agendaItems": [
    {
      "order": 1,
      "title": "지난 액션 아이템 점검",
      "description": "각자 완료 상태와 막힌 지점을 공유합니다.",
      "expectedMinutes": 10
    }
  ],
  "memberPrompts": [
    {
      "memberId": "uuid",
      "prompt": "이번 주에 해결한 문제와 막힌 DP 유형을 정리해오세요.",
      "reason": "지난 노트에서 DP 전이식 이해가 blocker로 기록되었습니다."
    }
  ],
  "risks": [
    {
      "level": "medium",
      "description": "사전 노트가 부족하면 피드백 품질이 낮아집니다."
    }
  ]
}
```

Validation rules:
- `schemaVersion` must equal `PreparationBriefV1`.
- `agendaItems` order starts at `1` and is unique.
- `memberPrompts.memberId` must reference a visible active group member when member-specific.
- `summary`, agenda titles, and prompts must be non-empty.

## `FeedbackReportV1`
Stored in `ai_feedback_reports`.

```json
{
  "schemaVersion": "FeedbackReportV1",
  "reportType": "group",
  "targetMemberId": null,
  "summary": "그룹은 과제 수행률이 높지만 blocker 공유가 늦었습니다.",
  "strengths": [
    {
      "title": "꾸준한 문제 풀이",
      "evidence": "세 명 모두 사전 노트에 풀이 기록을 남겼습니다."
    }
  ],
  "improvements": [
    {
      "title": "blocker 조기 공유",
      "suggestion": "세션 하루 전 blocker를 notes에 올립니다."
    }
  ],
  "nextActions": [
    {
      "title": "DP 전이식 예제 3개 정리",
      "assigneeMemberId": "uuid",
      "dueDate": "2026-05-09"
    }
  ]
}
```

Validation rules:
- `schemaVersion` must equal `FeedbackReportV1`.
- `reportType` is `group` or `individual`.
- `targetMemberId` is null for `group`.
- `targetMemberId` is required for `individual`.
- `nextActions` may be converted into `study_session_action_items`.
- Evidence must be derived from persisted notes, goals, action items, or session metadata.

## `ActionItemExtractionV1`
Stored in `ai_prompt_runs.output_payload`; converted action items are persisted in `study_session_action_items`.

```json
{
  "schemaVersion": "ActionItemExtractionV1",
  "items": [
    {
      "title": "그래프 BFS 템플릿 정리",
      "description": "다음 세션 전까지 공통 풀이 템플릿을 정리합니다.",
      "assignedMemberId": "uuid",
      "dueDate": "2026-05-09",
      "sourceNoteId": "uuid",
      "confidence": 0.82
    }
  ]
}
```

Validation rules:
- `confidence` is between `0` and `1`.
- A generated item with confidence below `0.6` is not auto-created; it can be shown as a suggestion in the client.
- `sourceNoteId` must point to the note used as evidence.

## AI Failure Behavior
| Failure | Stored Status | API Behavior |
| --- | --- | --- |
| Provider timeout | `failed` | `503` ProblemDetail with retryable code. |
| Invalid AI JSON | `failed` | `502` ProblemDetail; raw invalid text is not exposed. |
| Policy/validation rejection | `failed` | `422` ProblemDetail. |
| Missing required session data | no provider call | `409` ProblemDetail. |

## Post-MVP AI Items
- Real-time voice/STT.
- AI live meeting assistant.
- Provider-specific prompt tuning UI.
- Fine-grained token cost dashboards.
