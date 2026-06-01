# AI Study Leader API Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Machine contract: `docs/specs/openapi.yaml`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.
- Backend-owned OAuth2 cookie-login contract changes are authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md) under the v1 change-control process.
- Cross-site CSRF bootstrap for cookie-backed browser sessions is authorized by [CR-20260527-cross-site-csrf-bootstrap](./change-requests/CR-20260527-cross-site-csrf-bootstrap.md) and [ADR-20260527-cross-site-csrf-bootstrap](./adr/ADR-20260527-cross-site-csrf-bootstrap.md).
- Cross-site CSRF trusted-origin header validation is authorized by [CR-20260528-cross-site-csrf-trusted-origin-header](./change-requests/CR-20260528-cross-site-csrf-trusted-origin-header.md) and [ADR-20260528-cross-site-csrf-trusted-origin-header](./adr/ADR-20260528-cross-site-csrf-trusted-origin-header.md).
- Member week progress read endpoint is authorized by [CR-20260512-week-progress-read-endpoint](./change-requests/CR-20260512-week-progress-read-endpoint.md) and [ADR-20260512-week-progress-read-endpoint](./adr/ADR-20260512-week-progress-read-endpoint.md).
- Retrospective/chat DB-first context boundary is authorized by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md) and [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md).
- Notification SSE stream is authorized by [CR-20260601-notification-sse-stream](./change-requests/CR-20260601-notification-sse-stream.md) and [ADR-20260601-notification-sse-stream](./adr/ADR-20260601-notification-sse-stream.md).
- AI conversation SSE stream and message-list recovery are authorized by [CR-20260601-ai-conversation-sse-stream](./change-requests/CR-20260601-ai-conversation-sse-stream.md) and [ADR-20260601-ai-conversation-sse-stream](./adr/ADR-20260601-ai-conversation-sse-stream.md).

## Global Contract
- Base path: `/api/v1`.
- Authentication: bearer access token or `studypot_access_token` HttpOnly cookie unless endpoint is explicitly anonymous.
- Public auth endpoints: `GET /api/oauth2/authorization/google`, `GET /api/login/oauth2/code/google`, `GET /api/v1/auth/csrf`, `POST /api/v1/auth/oauth/google`, and `POST /api/v1/auth/refresh` are explicitly anonymous and do not require authentication.
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

## Retrospective and AI Conversation Boundary
- `POST /api/v1/weeks/{weekId}/retrospectives/me` requests or returns a stateful retrospective result for the authenticated member's week/progress context.
- `POST /api/v1/groups/{groupId}/ai-conversations` opens a chat session. When `conversationType = RETROSPECTIVE`, the session may link to an existing or future retrospective through `retrospectiveId`.
- `POST /api/v1/ai-conversations/{conversationId}/messages` stores user and assistant messages. Assistant messages can contribute to conversation summaries and later retrospective context.
- `GET /api/v1/ai-conversations/{conversationId}/messages` returns cursor-paged conversation messages for active conversation members and reconnect recovery.
- `GET /api/v1/ai-conversations/{conversationId}/stream` subscribes an active conversation member to best-effort SSE lifecycle events for the conversation.
- The MVP context builder for retrospective/chat is internal to the backend. It does not add public API request or response fields in SPT-81.
- The machine OpenAPI contract remains unchanged by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md); provider, vector store, and FastAPI service choices are not exposed at the REST boundary.

## Endpoint Index
| Method | Path | Feature ID | Actor | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/api/oauth2/authorization/google` | `identity-core` | anonymous/browser | Start backend-owned Google OAuth2 login and redirect to Google; authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md). |
| `GET` | `/api/login/oauth2/code/google` | `identity-core` | Google OAuth/browser | Complete Google OAuth2 login, issue HttpOnly token cookies, and redirect to frontend; authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md). |
| `POST` | `/api/v1/auth/oauth/google` | `identity-core` | anonymous/client | Exchange a Google authorization code for application tokens; authorized by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md). |
| `GET` | `/api/v1/auth/csrf` | `identity-core` | anonymous/browser | Bootstrap a readable CSRF token value for cross-site cookie-backed unsafe requests, setting `XSRF-TOKEN` when the browser accepts the backend-domain cookie; authorized by [CR-20260527-cross-site-csrf-bootstrap](./change-requests/CR-20260527-cross-site-csrf-bootstrap.md), [ADR-20260527-cross-site-csrf-bootstrap](./adr/ADR-20260527-cross-site-csrf-bootstrap.md), [CR-20260528-cross-site-csrf-trusted-origin-header](./change-requests/CR-20260528-cross-site-csrf-trusted-origin-header.md), and [ADR-20260528-cross-site-csrf-trusted-origin-header](./adr/ADR-20260528-cross-site-csrf-trusted-origin-header.md). |
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
| `GET` | `/api/v1/groups/{groupId}/members/me/profile` | `study-group-core` | current group member | Read my group-scoped member profile and current study summaries. |
| `PATCH` | `/api/v1/groups/{groupId}/members/me/profile` | `study-group-core` | current group member | Update my group-scoped display name. |
| `POST` | `/api/v1/groups/{groupId}/start` | `curriculum-core` | owner | Start study and generate curriculum. |
| `GET` | `/api/v1/groups/{groupId}/onboarding/me` | `group-onboarding` | group member | Read my onboarding response. |
| `POST` | `/api/v1/groups/{groupId}/onboarding/me` | `group-onboarding` | group member | Submit onboarding with overall skill level, note, and availability. |
| `GET` | `/api/v1/groups/{groupId}/curriculum` | `curriculum-core` | group member | Read active curriculum. |
| `GET` | `/api/v1/groups/{groupId}/weeks/current` | `weekly-todo` | group member | Read current week metadata. |
| `GET` | `/api/v1/weeks/{weekId}/tasks` | `weekly-todo` | group member | List weekly tasks. |
| `GET` | `/api/v1/weeks/{weekId}/progress/me` | `weekly-todo` | group member | Read my member week progress. |
| `PUT` | `/api/v1/weeks/{weekId}/progress/me` | `weekly-todo` | group member | Update member week progress note/status. |
| `POST` | `/api/v1/tasks/{taskId}/completion/me` | `weekly-todo` | group member | Complete, skip, or mark task incomplete. |
| `POST` | `/api/v1/weeks/{weekId}/retrospectives/me` | `retrospective-feedback` | group member | Request retrospective feedback. |
| `GET` | `/api/v1/weeks/{weekId}/retrospectives/me` | `retrospective-feedback` | group member | Read my retrospective. |
| `POST` | `/api/v1/groups/{groupId}/ai-conversations` | `ai-team-leader` | group member | Open AI team leader conversation. |
| `GET` | `/api/v1/ai-conversations/{conversationId}/messages` | `ai-team-leader` | conversation member | List AI team leader conversation messages for reconnect recovery. |
| `POST` | `/api/v1/ai-conversations/{conversationId}/messages` | `ai-team-leader` | conversation member | Send message and get assistant response. |
| `GET` | `/api/v1/ai-conversations/{conversationId}/stream` | `ai-team-leader` | conversation member | Subscribe to AI team leader conversation SSE lifecycle events. |
| `GET` | `/api/v1/users/me/notifications` | `notification` | authenticated | List my in-app notifications. |
| `GET` | `/api/v1/users/me/notifications/stream` | `notification` | authenticated | Subscribe to my in-app notification SSE stream. |
| `POST` | `/api/v1/notifications/{notificationId}/read` | `notification` | notification recipient | Mark one in-app notification as read. |
| `POST` | `/api/v1/users/me/notifications/read-all` | `notification` | authenticated | Mark all my in-app notifications as read. |
| `GET` | `/api/v1/groups/{groupId}/notifications` | `notification` | owner | List group notification logs for audit. |
| `GET` | `/api/v1/groups/{groupId}/llm-usage` | `ai-team-leader` | owner | List LLM usage records. |

## Key Request Shapes
### Backend-Owned Google OAuth Login
- Approved by [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md) and [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md).
- Browser starts login with `GET /api/oauth2/authorization/google`.
- Backend redirects to Google through Spring Security OAuth2 Login. The PKCE verifier must be persisted server-side, such as in the short-lived server session or Redis entry keyed by the OAuth `state` parameter value, for about 10 minutes and cleared when the callback completes; it must not be sent to the browser as an `oauth_pkce_verifier` cookie.
- A temporary `oauth_state` browser cookie may be used only as an optional client-server correlation check for the same OAuth `state` value. If present, it is short-lived, HttpOnly, Secure in production, SameSite=Lax, Path=/, and expires within 10 minutes.
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
Browser clients that rely on HttpOnly cookies and cannot read the backend-domain `XSRF-TOKEN` cookie directly, such as a Netlify frontend calling the rumiclean API, must first call `GET /api/v1/auth/csrf` with credentials included. The response body and `X-XSRF-TOKEN` response header contain the token value to echo in the `X-XSRF-TOKEN` request header. The backend also sets the matching `XSRF-TOKEN` cookie using the configured browser cookie policy when the browser accepts it. If a trusted configured CORS origin sends the custom CSRF header but the XSRF cookie is unavailable on the unsafe request, the backend accepts the header-only CSRF evidence. Headerless requests and header-only requests from untrusted origins remain forbidden.

```json
{
  "refreshToken": "raw-refresh-token-issued-by-backend"
}
```
`POST /api/v1/auth/refresh` may also omit the request body when the `studypot_refresh_token` HttpOnly cookie is present.
If both the JSON `refreshToken` and the `studypot_refresh_token` cookie are provided, the cookie value takes precedence and the JSON body value is ignored.
On success, the response body returns only `tokenType`, `expiresIn`, and `user`; the raw access/refresh token values are delivered through rotated HttpOnly cookies. On missing, invalid, expired, revoked, or reused refresh tokens, the endpoint returns 401 Problem Detail with a refresh-token-specific `code` such as `REFRESH_TOKEN_REQUIRED`, `REFRESH_TOKEN_INVALID`, or `REFRESH_TOKEN_EXPIRED`, and clears the application token cookies when cookie support is configured.

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

### Suggest Detail Keywords
`POST /api/v1/groups/detail-keyword-suggestions`

Request:
```json
{
  "topic": "Spring Boot",
  "hintKeywords": [],
  "maxCandidates": 5
}
```

`hintKeywords` and `maxCandidates` can be omitted. The default candidate limit is 5.

Response:
```json
{
  "keywords": ["JPA", "Spring Security", "Spring Batch"]
}
```

Suggested keywords are transient. Only the final selected or directly entered `detailKeywords` are persisted by `POST /api/v1/groups`.

### Submit Onboarding
```json
{
  "skillLevel": 3,
  "additionalNote": "JPA는 처음이고 실습 과제 위주가 좋아요.",
  "availabilitySlots": [
    {"dayOfWeek": 2, "startTime": "20:00", "endTime": "22:00", "timezone": "Asia/Seoul"}
  ]
}
```

The backend maps `skillLevel` to internal keyword score JSON for the group's detail keywords so existing curriculum and retrospective context builders continue to work. Public onboarding responses expose `skillLevel`, not `keywordSkillLevels` or `taskPreferences`.

### Group Member Profile
Approved by [CR-20260601-group-member-profile-api](./change-requests/CR-20260601-group-member-profile-api.md) and [ADR-20260601-group-member-profile-api](./adr/ADR-20260601-group-member-profile-api.md).

`GET /api/v1/groups/{groupId}/members/me/profile` response:
```json
{
  "groupId": "018f6f55-6fb1-7d62-a711-25f7c6d16a28",
  "memberId": "018f6f55-75e9-78d2-9f5c-598945b93400",
  "userId": "018f6f55-6f42-7e11-b479-120c5f2e9d42",
  "displayName": "현우",
  "permission": "MEMBER",
  "status": "ACTIVE",
  "onboarding": {
    "submitted": true,
    "skillLevel": 3,
    "submittedAt": "2026-05-10T01:00:00Z"
  },
  "currentWeek": {
    "weekId": "018f6f55-8bf2-78d9-a332-6e74b1484520",
    "weekNumber": 2,
    "sprintGoal": "JPA 실습",
    "startsAt": "2026-05-17T00:00:00Z",
    "endsAt": "2026-05-24T00:00:00Z",
    "progressStatus": "IN_PROGRESS"
  },
  "taskCompletion": {
    "totalCount": 4,
    "doneCount": 2,
    "incompleteCount": 1,
    "skippedCount": 1
  },
  "retrospective": {
    "feedbackReady": true
  }
}
```

`PATCH /api/v1/groups/{groupId}/members/me/profile` request:
```json
{
  "displayName": "현우"
}
```

- `displayName` is the group-scoped display name stored in `group_member.display_name`; it is distinct from global user nickname/email.
- PATCH is limited to `displayName` with 1 to 80 non-blank characters. Introduction, goal, memo, and profile image fields are deferred until a DB/API change is approved.
- Only current `PENDING_ONBOARDING` or `ACTIVE` members can read/update their own profile. Existing groups with no current membership, `LEFT` membership, deleted membership, or another user return forbidden. Missing groups return not found.
- `currentWeek` is omitted/null when the group has no active current week. `taskCompletion` still returns zero counts in that case.

### Task Completion
Request:
```json
{
  "status": "INCOMPLETE",
  "completionNote": null,
  "incompleteReason": "이번 주 개인 일정으로 실습을 못 끝냈습니다.",
  "evidenceUrl": null
}
```

Response:
```json
{
  "id": "018f6f55-8f6c-7334-a781-84152e57e4f4",
  "taskId": "018f6f55-8d26-73ed-828f-b955fbd6328a",
  "status": "INCOMPLETE",
  "completedAt": null,
  "reasonSubmittedAt": "2026-05-24T10:30:30Z",
  "completionNote": null,
  "incompleteReason": "이번 주 개인 일정으로 실습을 못 끝냈습니다.",
  "evidenceUrl": null
}
```

- `DONE`: `completionNote` and `evidenceUrl` are optional. `incompleteReason` is not allowed. If the task is already `DONE`, repeated `DONE` requests are idempotent and preserve the first completion timestamp, note, and evidence URL.
- `INCOMPLETE`: `incompleteReason` is required. `completionNote` and `evidenceUrl` are not allowed. `reasonSubmittedAt` stores the first incomplete-reason submission timestamp.
- `SKIPPED`: completion, incomplete, and evidence fields are not allowed.
- The API is member-scoped. A user can mutate only their own completion for a task in a group where they have active membership; another group's task returns forbidden.

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
- Cross-site browser clients should bootstrap CSRF with `GET /api/v1/auth/csrf` before cookie-backed unsafe requests such as refresh, logout, group creation, onboarding submit, and task updates. Same-site clients may echo the readable `XSRF-TOKEN` cookie; trusted configured CORS origins may use the custom CSRF header from the bootstrap response when the XSRF cookie is unavailable.
- The backend exchanges the authorization code, upserts `users` and `oauth_account`, stores application refresh-token hashes in `refresh_token`, and issues an access/refresh token pair. Backend-owned OAuth callback (`GET /api/login/oauth2/code/google`) issues tokens as HttpOnly cookies only; compatibility endpoint (`POST /api/v1/auth/oauth/google`) returns the JSON token response and also sets the same HttpOnly cookies.
- `POST /api/v1/auth/refresh` rotates the refresh token; the old refresh token must not be accepted again. The refresh token may come from the JSON body or `studypot_refresh_token` HttpOnly cookie.
- `studypot_access_token` and `studypot_refresh_token` cookies are HttpOnly, Path=/, Secure in production, use the configured SameSite policy, and expire with the corresponding token TTL.
- `POST /api/v1/auth/logout` revokes the submitted refresh token or refresh-token cookie for the authenticated user.
- `POST /auth/logout-all` revokes every active refresh token for the authenticated user.
- Raw application refresh tokens, provider access tokens, provider refresh tokens, and OAuth client secrets must not be logged or exposed in frontend-readable locations. JSON token responses remain only for compatibility clients.
