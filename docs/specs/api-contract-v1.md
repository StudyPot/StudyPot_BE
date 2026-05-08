# AI Study Leader API Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Machine contract: `docs/specs/openapi.yaml`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.
- Backend-owned OAuth2 cookie-login contract changes are authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md) under the v1 change-control process.

## Global Contract
- Base path: `/api/v1`.
- Authentication: bearer access token or `studypot_access_token` HttpOnly cookie unless endpoint is explicitly anonymous.
- Public auth endpoints: `GET /api/oauth2/authorization/google`, `GET /api/login/oauth2/code/google`, `POST /auth/oauth/google`, and `POST /auth/refresh` are explicitly anonymous and do not require authentication.
- IDs: UUID strings at the API boundary; persistence stores UUIDv7 as `BINARY(16)`.
- Error format: RFC 9457-style problem detail.
- Pagination: cursor pagination for list endpoints that can grow.
- JSON fields preserve the shapes defined in DB and AI contracts.

## Resource Summary
| Resource | Feature ID | Description |
| --- | --- | --- |
| Auth/User | `identity-core` | Current user and application session identity. |
| Study Group | `study-group-core` | Group creation, invite code, membership, status. |
| Onboarding | `group-onboarding` | Group/member onboarding responses and availability slots. |
| Curriculum | `curriculum-core` | Host-start generated curriculum, weeks, tasks. |
| Weekly Todo | `weekly-todo` | Member week progress and task completion/incomplete reasons. |
| Retrospective | `retrospective-feedback` | AI feedback and next-week adjustment. |
| AI Conversation | `ai-team-leader` | AI team leader chat sessions and messages. |
| Notification/Usage | `notification`, `ai-team-leader` | In-app notifications and LLM usage. |

## Endpoint Index
| Method | Path | Feature ID | Actor | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/oauth2/authorization/google` | `identity-core` | anonymous/browser | Start backend-owned Google OAuth2 login and redirect to Google; authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md). |
| `GET` | `/api/login/oauth2/code/google` | `identity-core` | Google OAuth/browser | Complete Google OAuth2 login, issue HttpOnly token cookies, and redirect to frontend; authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md). |
| `POST` | `/api/v1/auth/oauth/google` | `identity-core` | anonymous/client | Exchange a Google authorization code for application tokens; authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md). |
| `POST` | `/api/v1/auth/refresh` | `identity-core` | anonymous/client | Rotate refresh token and issue new application tokens; authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md). |
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
### Backend-Owned Google OAuth Login
- Approved by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md).
- Browser starts login with `GET /api/oauth2/authorization/google`.
- Backend redirects to Google through Spring Security OAuth2 Login. The PKCE verifier must be persisted server-side, such as in the short-lived server session or Redis entry keyed by `oauth_state`, for about 10 minutes and cleared when the callback completes; it must not be sent to the browser as an `oauth_pkce_verifier` cookie.
- A temporary `oauth_state` browser cookie may be used only for request correlation. If present, it is short-lived, HttpOnly, Secure in production, SameSite=Lax, Path=/, and expires within 10 minutes.
- Google returns to `GET /api/login/oauth2/code/google`.
- On success, backend sets `studypot_access_token` and `studypot_refresh_token` HttpOnly cookies, clears temporary OAuth state/browser cookies and server-side PKCE state, and redirects to the configured frontend success URI without token query parameters.
- Token cookies use Secure in production, HttpOnly, Path=/, configured SameSite policy, and Max-Age matching the access-token TTL and refresh-token TTL respectively.
- Configured frontend success/failure redirect URIs must be exact configured server-side values or validated against a server-side allowlist; invalid redirect targets must be rejected or replaced with a safe fallback to prevent open redirects.
- On failure or state mismatch, backend clears temporary OAuth state/browser cookies and server-side PKCE state, does not issue application token cookies, and redirects to the configured frontend failure URI.

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
`POST /api/v1/auth/refresh` may also omit the request body when the `studypot_refresh_token` HttpOnly cookie is present.
If both the JSON `refreshToken` and the `studypot_refresh_token` cookie are provided, the cookie value takes precedence and the JSON body value is ignored.

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
- `refresh_token`: active -> revoked when used for logout, logout-all, or detected reuse after rotation.
- `study_group`: `DRAFT -> ONBOARDING -> ACTIVE -> COMPLETED`; `ARCHIVED` can be applied by owner/admin flows.
- `group_member`: `PENDING_ONBOARDING -> ACTIVE -> LEFT`.
- `task_completion`: `TODO -> DONE`, `TODO -> INCOMPLETE`, `TODO -> SKIPPED`.
- `retrospective`: `PENDING -> PROCESSING -> COMPLETED` or `FAILED`.
- `notification`: `PENDING -> DELIVERED -> READ`; failed generation/delivery can become `FAILED` or `SKIPPED`.

## API Compatibility Rules
- Adding/removing endpoints, changing request fields, response fields, enum values, or authorization behavior requires Change Request and ADR.
- OpenAPI must parse before PR review.
- API examples must match DB contract enum values.

## Auth API Rules
- MVP login provider is Google OAuth.
- Browser clients should start Google login at `GET /api/oauth2/authorization/google`; backend-owned callback handling must not expose access or refresh token values in redirect URLs.
- Compatibility clients may still obtain the Google authorization code themselves and send it to `POST /auth/oauth/google`.
- The backend exchanges the authorization code, upserts `users` and `oauth_account`, stores application refresh-token hashes in `refresh_token`, and issues an access/refresh token pair. Backend-owned OAuth callback (`GET /api/login/oauth2/code/google`) issues tokens as HttpOnly cookies only; compatibility endpoint (`POST /api/v1/auth/oauth/google`) returns the JSON token response and also sets the same HttpOnly cookies.
- `POST /auth/refresh` rotates the refresh token; the old refresh token must not be accepted again. The refresh token may come from the JSON body or `studypot_refresh_token` HttpOnly cookie.
- `studypot_access_token` and `studypot_refresh_token` cookies are HttpOnly, Path=/, Secure in production, use the configured SameSite policy, and expire with the corresponding token TTL.
- `POST /auth/logout` revokes the submitted refresh token or refresh-token cookie for the authenticated user.
- `POST /auth/logout-all` revokes every active refresh token for the authenticated user.
- Raw application refresh tokens, provider access tokens, provider refresh tokens, and OAuth client secrets must not be logged or exposed in frontend-readable locations. JSON token responses remain only for compatibility clients.
