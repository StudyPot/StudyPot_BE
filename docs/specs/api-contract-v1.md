# AI Study Leader API Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Machine contract: `docs/specs/openapi.yaml`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.

## Global Contract
- Base path: `/api/v1`.
- Authentication: bearer access token unless endpoint is explicitly anonymous.
- IDs: UUID strings at the API boundary; persistence stores UUIDv7 as `BINARY(16)`.
- Error format: RFC 9457-style problem detail.
- Pagination: cursor pagination for list endpoints that can grow.
- JSON fields preserve the shapes defined in DB and AI contracts.

## Resource Summary
| Resource | Feature ID | Description |
| --- | --- | --- |
| Auth/User | `identity-core` | Current user and OAuth/Discord identity linkage. |
| Study Group | `study-group-core` | Group creation, invite code, membership, status. |
| Onboarding | `group-onboarding` | Group/member onboarding responses and availability slots. |
| Curriculum | `curriculum-core` | Host-start generated curriculum, weeks, tasks. |
| Weekly Todo | `weekly-todo` | Member week progress and task completion/incomplete reasons. |
| Retrospective | `retrospective-feedback` | AI feedback and next-week adjustment. |
| AI Conversation | `ai-team-leader` | AI team leader chat sessions and messages. |
| Notification/Usage | `discord-notifications`, `ai-team-leader` | Notification logs and LLM usage. |

## Endpoint Index
| Method | Path | Feature ID | Actor | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/users/me` | `identity-core` | authenticated | Read current user. |
| `POST` | `/api/v1/users/me/discord-link` | `identity-core` | authenticated | Link Discord integration. |
| `DELETE` | `/api/v1/users/me/discord-link` | `identity-core` | authenticated | Unlink Discord integration. |
| `GET` | `/api/v1/groups` | `study-group-core` | authenticated | List my groups. |
| `POST` | `/api/v1/groups` | `study-group-core` | authenticated | Create group and owner membership. |
| `GET` | `/api/v1/groups/{groupId}` | `study-group-core` | group member | Read group. |
| `PATCH` | `/api/v1/groups/{groupId}` | `study-group-core` | owner | Update mutable group fields before active lock. |
| `POST` | `/api/v1/groups/{groupId}/join` | `study-group-core` | authenticated | Join by invite code/link. |
| `GET` | `/api/v1/groups/{groupId}/members` | `study-group-core` | group member | List members and onboarding status. |
| `POST` | `/api/v1/groups/{groupId}/start` | `curriculum-core` | owner | Start study and generate curriculum. |
| `GET` | `/api/v1/groups/{groupId}/onboarding/me` | `group-onboarding` | group member | Read my onboarding response. |
| `PUT` | `/api/v1/groups/{groupId}/onboarding/me` | `group-onboarding` | group member | Save draft/submitted onboarding response. |
| `POST` | `/api/v1/groups/{groupId}/onboarding/me/submit` | `group-onboarding` | group member | Submit onboarding. |
| `GET` | `/api/v1/groups/{groupId}/curriculum` | `curriculum-core` | group member | Read active curriculum. |
| `GET` | `/api/v1/groups/{groupId}/weeks/current` | `weekly-todo` | group member | Read current week with tasks and progress. |
| `GET` | `/api/v1/weeks/{weekId}/tasks` | `weekly-todo` | group member | List weekly tasks. |
| `PUT` | `/api/v1/weeks/{weekId}/progress/me` | `weekly-todo` | group member | Update member week progress note/status. |
| `POST` | `/api/v1/tasks/{taskId}/completion/me` | `weekly-todo` | group member | Complete, skip, or mark task incomplete. |
| `POST` | `/api/v1/weeks/{weekId}/retrospectives/me` | `retrospective-feedback` | group member | Request retrospective feedback. |
| `GET` | `/api/v1/weeks/{weekId}/retrospectives/me` | `retrospective-feedback` | group member | Read my retrospective. |
| `POST` | `/api/v1/groups/{groupId}/ai-conversations` | `ai-team-leader` | group member | Open AI team leader conversation. |
| `POST` | `/api/v1/ai-conversations/{conversationId}/messages` | `ai-team-leader` | conversation member | Send message and get assistant response. |
| `GET` | `/api/v1/groups/{groupId}/notifications` | `discord-notifications` | owner | List notification logs. |
| `GET` | `/api/v1/groups/{groupId}/llm-usage` | `ai-team-leader` | owner | List LLM usage records. |

## Key Request Shapes
### Create Group
```json
{
  "name": "Backend Interview Study",
  "topic": "Spring Boot",
  "detailKeywords": ["JPA", "Security", "Testing"],
  "maxMembers": 6,
  "startsAt": "2026-05-06",
  "endsAt": "2026-06-17"
}
```

### Submit Onboarding
```json
{
  "keywordSkillLevels": {"JPA": 2, "Security": 1, "Testing": 3},
  "taskPreferences": {"READING": 3, "PRACTICE": 5, "ASSIGNMENT": 4},
  "additionalNote": "실습 과제 위주가 좋아요.",
  "availabilitySlots": [
    {"dayOfWeek": 2, "startTime": "20:00", "endTime": "22:00", "timezone": "Asia/Seoul"}
  ]
}
```

### Task Completion
```json
{
  "status": "INCOMPLETE",
  "completionNote": null,
  "incompleteReason": "이번 주 개인 일정으로 실습을 못 끝냈습니다.",
  "evidenceUrl": null
}
```

## State Transition Rules
- `study_group`: `DRAFT -> ONBOARDING -> ACTIVE -> COMPLETED`; `ARCHIVED` can be applied by owner/admin flows.
- `group_member`: `PENDING_ONBOARDING -> ACTIVE -> LEFT`.
- `task_completion`: `TODO -> DONE`, `TODO -> INCOMPLETE`, `TODO -> SKIPPED`.
- `retrospective`: `PENDING -> PROCESSING -> COMPLETED` or `FAILED`.

## API Compatibility Rules
- Adding/removing endpoints, changing request fields, response fields, enum values, or authorization behavior requires Change Request and ADR.
- OpenAPI must parse before PR review.
- API examples must match DB contract enum values.
