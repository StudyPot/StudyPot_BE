# API Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Machine contract: `docs/specs/openapi.yaml`
- Changes require Change Request and ADR.

## API Baseline
- Style: REST.
- Base path: `/api/v1`.
- Format: JSON.
- OpenAPI: 3.1.0.
- Auth: session or bearer token chosen during Spring scaffold, but endpoint behavior remains fixed.
- Errors: RFC 7807 style `application/problem+json`.
- Pagination: cursor pagination for list endpoints.
- Time: RFC 3339 timestamps in UTC; clients may render with group/user timezone.
- IDs: UUID strings generated as UUIDv7 by application code.

## Common Response Rules
| Case | Status | Body |
| --- | --- | --- |
| Create | `201` | Created resource. |
| Read/update | `200` | Resource or result body. |
| Delete/archive | `204` | Empty body. |
| Validation failure | `422` | `ProblemDetail` with `fieldErrors`. |
| Conflict | `409` | `ProblemDetail`. |
| Unauthorized | `401` | `ProblemDetail`. |
| Forbidden | `403` | `ProblemDetail`. |
| Not found | `404` | `ProblemDetail`. |

## Cursor Pagination
List responses use:

```json
{
  "items": [],
  "pageInfo": {
    "nextCursor": "opaque-cursor",
    "hasNext": true
  }
}
```

Query params:
- `cursor`: opaque cursor from prior page.
- `limit`: default `20`, maximum `100`.

## Endpoint Groups

### Auth And User
| Method | Path | Feature | Permission | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/auth/oauth/google/authorize` | `identity-core` | anonymous | Start Google OAuth. |
| `POST` | `/api/v1/auth/oauth/google/callback` | `identity-core` | anonymous | Complete OAuth callback. |
| `GET` | `/api/v1/users/me` | `identity-core` | authenticated | Read current user. |
| `POST` | `/api/v1/users/me/discord-link` | `identity-core` | authenticated | Link Discord account. |
| `DELETE` | `/api/v1/users/me/discord-link` | `identity-core` | authenticated | Unlink Discord account. |

### Study Groups
| Method | Path | Feature | Permission | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/groups` | `study-group-core` | authenticated | List my groups. |
| `POST` | `/api/v1/groups` | `study-group-core` | authenticated | Create group and owner membership. |
| `GET` | `/api/v1/groups/{groupId}` | `study-group-core` | group member | Read group. |
| `PATCH` | `/api/v1/groups/{groupId}` | `study-group-core`, `study-group-rules` | owner/manager | Update mutable group fields except slug. |
| `DELETE` | `/api/v1/groups/{groupId}` | `study-group-core` | owner | Archive group. |

### Members And Invitations
| Method | Path | Feature | Permission | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/groups/{groupId}/members` | `study-group-core` | group member | List members. |
| `PATCH` | `/api/v1/groups/{groupId}/members/{memberId}` | `study-group-core` | owner/manager | Update member role/status. |
| `GET` | `/api/v1/groups/{groupId}/invitations` | `study-group-core` | owner/manager | List invitations. |
| `POST` | `/api/v1/groups/{groupId}/invitations` | `study-group-core` | owner/manager | Create invitation. |
| `POST` | `/api/v1/invitations/{invitationId}/accept` | `study-group-core` | authenticated | Accept invitation. |

### Sessions, Attendance, Notes, Action Items
| Method | Path | Feature | Permission | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/groups/{groupId}/sessions` | `study-session-core` | group member | List sessions. |
| `POST` | `/api/v1/groups/{groupId}/sessions` | `study-session-core` | owner/manager | Create session. |
| `GET` | `/api/v1/sessions/{sessionId}` | `study-session-core` | group member | Read session. |
| `PATCH` | `/api/v1/sessions/{sessionId}` | `study-session-core` | owner/manager | Update session. |
| `GET` | `/api/v1/sessions/{sessionId}/attendance` | `study-session-core` | group member | List attendance. |
| `PUT` | `/api/v1/sessions/{sessionId}/attendance/{memberId}` | `study-session-core` | self or manager | Upsert attendance. |
| `GET` | `/api/v1/sessions/{sessionId}/notes` | `structured-notes` | group member | List notes. |
| `POST` | `/api/v1/sessions/{sessionId}/notes` | `structured-notes` | group member | Submit note. |
| `GET` | `/api/v1/sessions/{sessionId}/action-items` | `structured-notes` | group member | List action items. |
| `POST` | `/api/v1/sessions/{sessionId}/action-items` | `structured-notes` | owner/manager or self-owned member | Create action item. |
| `PATCH` | `/api/v1/action-items/{actionItemId}` | `structured-notes` | assignee or manager | Update action item. |

### Goals And Resources
| Method | Path | Feature | Permission | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/groups/{groupId}/goals` | `study-group-core` | group member | List goals. |
| `POST` | `/api/v1/groups/{groupId}/goals` | `study-group-core` | owner/manager | Create goal. |
| `PATCH` | `/api/v1/goals/{goalId}` | `study-group-core` | owner/manager or owner member | Update goal. |
| `GET` | `/api/v1/groups/{groupId}/resources` | `study-group-core` | group member | List resources. |
| `POST` | `/api/v1/groups/{groupId}/resources` | `study-group-core` | group member | Add resource. |

### AI
| Method | Path | Feature | Permission | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/sessions/{sessionId}/ai/preparation-briefs` | `ai-prep-brief` | group member | List preparation briefs. |
| `POST` | `/api/v1/sessions/{sessionId}/ai/preparation-briefs` | `ai-prep-brief` | owner/manager | Generate preparation brief. |
| `GET` | `/api/v1/sessions/{sessionId}/ai/feedback-reports` | `ai-feedback-report` | group member with privacy rules | List feedback reports. |
| `POST` | `/api/v1/sessions/{sessionId}/ai/feedback-reports` | `ai-feedback-report` | owner/manager | Generate feedback report. |

### Discord Notifications
| Method | Path | Feature | Permission | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/groups/{groupId}/discord/channels` | `discord-notifications` | owner/manager | List connected channels. |
| `POST` | `/api/v1/groups/{groupId}/discord/channels` | `discord-notifications` | owner/manager | Connect channel. |
| `PATCH` | `/api/v1/discord/channels/{channelId}` | `discord-notifications` | owner/manager | Update notification settings/status. |
| `DELETE` | `/api/v1/discord/channels/{channelId}` | `discord-notifications` | owner/manager | Disable channel. |
| `GET` | `/api/v1/groups/{groupId}/discord/notification-logs` | `discord-notifications` | owner/manager | List notification logs. |

## DTO Naming For Spring
- Request DTOs: `<Action><Resource>Request`, for example `CreateStudyGroupRequest`.
- Response DTOs: `<Resource>Response`, for example `StudyGroupResponse`.
- Page DTOs: `CursorPageResponse<T>`.
- Error DTO: `ProblemDetailResponse` if wrapping Spring's `ProblemDetail` is needed.

## Contract Boundaries
- This API contract defines behavior and wire shape.
- It does not create controllers, DTO classes, Spring Security config, or database migrations.
- Any endpoint addition, removal, path change, enum change, or response shape change requires Change Request and ADR.
