# 05 API 명세

## 문서 상태
- Jira: `SPT-7`
- Lock status: `LOCKED_FOR_IMPLEMENTATION`
- Human contract: `docs/specs/api-contract-v1.md`
- Machine contract: `docs/specs/openapi.yaml`
- OpenAPI version: `3.1.0`
- Current contract size: 25 paths, 31 schemas
- Approved changes: `CR-20260430-onboarding-mysql8-mvp`, `CR-20260504-no-discord-inapp-notification`, `CR-20260506-auth-api-entrypoints`
- 변경 규칙: endpoint, path, request/response field, enum, authorization behavior 변경은 Change Request + ADR 필요

## Global Contract
- Base path: `/api/v1`
- Authentication: bearer access token unless an endpoint is explicitly anonymous.
- Public auth endpoints: `POST /auth/oauth/google`, `POST /auth/refresh`.
- ID format: UUID string at the API boundary.
- Persistence ID format: application-generated UUIDv7 stored as MySQL `BINARY(16)`.
- Error format: RFC 9457-style problem detail.
- Pagination: cursor pagination for lists that can grow.
- JSON fields preserve shapes defined in DB and AI contracts.

## Resource Summary
| Resource | Feature ID | Purpose |
| --- | --- | --- |
| Auth/User | `identity-core` | Current user and session identity. |
| Study Group | `study-group-core` | Group creation, invite code, membership, and status. |
| Onboarding | `group-onboarding` | Group/member onboarding responses and availability slots. |
| Curriculum | `curriculum-core` | Host-start generated curriculum, weeks, and tasks. |
| Weekly Todo | `weekly-todo` | Member week progress and task completion/incomplete reasons. |
| Retrospective | `retrospective-feedback` | AI feedback and next-week adjustment. |
| AI Conversation | `ai-team-leader` | AI team leader chat sessions and messages. |
| Notification/Usage | `notification`, `ai-team-leader` | In-app notifications and LLM usage logs. |

## Endpoint Index
| Method | Path | Feature ID | Actor | Purpose |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/auth/oauth/google` | `identity-core` | anonymous/client | Exchange a Google authorization code for application tokens. |
| `POST` | `/api/v1/auth/refresh` | `identity-core` | anonymous/client | Rotate refresh token and issue new application tokens. |
| `POST` | `/api/v1/auth/logout` | `identity-core` | authenticated | Revoke the submitted current refresh token. |
| `POST` | `/api/v1/auth/logout-all` | `identity-core` | authenticated | Revoke all refresh tokens for the current user. |
| `GET` | `/api/v1/users/me` | `identity-core` | authenticated | Read current user. |
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
| `GET` | `/api/v1/users/me/notifications` | `notification` | authenticated | List my in-app notifications. |
| `POST` | `/api/v1/notifications/{notificationId}/read` | `notification` | notification recipient | Mark one in-app notification as read. |
| `POST` | `/api/v1/users/me/notifications/read-all` | `notification` | authenticated | Mark all my in-app notifications as read. |
| `GET` | `/api/v1/groups/{groupId}/notifications` | `notification` | owner | List group notification logs for audit. |
| `GET` | `/api/v1/groups/{groupId}/llm-usage` | `ai-team-leader` | owner | List LLM usage records. |

## Key Request Shapes
### Google OAuth Login
```json
{
  "authorizationCode": "google-authorization-code",
  "redirectUri": "https://app.studypot.example/auth/callback",
  "codeVerifier": "pkce-code-verifier"
}
```

### Refresh Token
```json
{
  "refreshToken": "raw-refresh-token-issued-by-backend"
}
```

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
- `notification`: `PENDING -> DELIVERED -> READ`; failed generation/delivery can become `FAILED` or `SKIPPED`.

## Permission Summary
- Bearer token authentication is required for every `/api/v1` endpoint unless explicitly documented otherwise.
- Google OAuth login and refresh endpoints are explicitly public auth endpoints.
- Current-session logout and logout-all require bearer authentication.
- Group read access requires membership.
- Group update, host start, group notification logs, and LLM usage logs are owner-only.
- Pending members may submit onboarding but cannot complete weekly tasks.
- Active members may read curriculum/current week, complete their own tasks, request their own retrospective, and chat with the AI team leader.
- Notification list/read endpoints are scoped to the authenticated recipient user.
- Cross-group access must be rejected even when a resource ID exists.

## Notification API Contract
- MVP channel is `IN_APP`.
- Recipients can list and mark only their own notifications as read.
- Owner group notification logs are audit-only for MVP; owners cannot mutate another member's read state.
- Notification rows must retain recipient, title/body, payload, related resource IDs, idempotency key, status, delivered timestamp, and read timestamp.

## Verification
- OpenAPI YAML must parse before PR review.
- Current local parse: `openapi=3.1.0`, `paths=25`, `schemas=31`.
- Standard repo verification: `./gradlew check build --no-daemon`.

## 추적 링크
- Human API contract: `docs/specs/api-contract-v1.md`
- Machine API contract: `docs/specs/openapi.yaml`
- Permissions: `docs/specs/auth-permissions-v1.md`
- Notification contract: `docs/specs/notification-contract-v1.md`
- QA acceptance: `docs/specs/qa-acceptance-v1.md`
- Jira: `SPT-7`
