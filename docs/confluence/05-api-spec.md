# 05 API 명세

## 문서 상태
- Jira: `SPT-7`
- Lock status: `LOCKED_FOR_IMPLEMENTATION`
- Human contract: `docs/specs/api-contract-v1.md`
- Machine contract: `docs/specs/openapi.yaml`
- OpenAPI version: `3.1.0`
- Current contract size: 36 paths, 59 schemas
- Approved changes: `CR-20260430-onboarding-mysql8-mvp`, `CR-20260504-no-discord-inapp-notification`, `CR-20260506-auth-api-entrypoints`, `CR-20260508-oauth2-cookie-login`, `CR-20260512-week-progress-read-endpoint`, `CR-20260512-retrospective-rag-boundary`, `CR-20260520-onboarding-simplification-auto-merge`, `CR-20260601-notification-sse-stream`, `CR-20260601-ai-conversation-sse-stream`, `CR-20260601-task-completion-response-contract`, `CR-20260601-group-member-profile-api`, `CR-20260601-study-group-board-api`, `CR-20260601-fixed-weekly-sprint-windows`
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
| Study Group Board | `study-group-board` | Group-visible default boards, posts, and comments. |
| Onboarding | `group-onboarding` | Group/member onboarding responses and availability slots. |
| Curriculum | `curriculum-core` | Host-start generated curriculum, weeks, and tasks. |
| Weekly Todo | `weekly-todo` | Member week progress and task completion/incomplete reasons. |
| Retrospective | `retrospective-feedback` | AI feedback and next-week adjustment. |
| AI Conversation | `ai-team-leader` | AI team leader chat sessions and messages. |
| Notification/Usage | `notification`, `ai-team-leader` | In-app notifications and LLM usage logs. |

## Retrospective / AI Chat Boundary
- `retrospective` is the final week/member/progress feedback state.
- `ai_conversation` and `ai_conversation_message` are the chat/input surface and may link to a retrospective when `conversationType = RETROSPECTIVE`.
- SPT-81 does not add public API fields or OpenAPI paths. The DB-first context builder runs inside the backend before retrospective/chat LLM provider calls.
- Vector store, GraphRAG, MCP, FastAPI service split, and broader agent orchestration are deferred to SPT-82 or later approved tasks. SPT-82's Proposed ADR keeps the current API request/response boundary unchanged; streaming or service-split endpoints require a later approved task.

## SSAFY Coach Direct Implementation Evidence
This section maps rubric-facing API surfaces to concrete backend implementation and verification files. It is evidence metadata for analysis, not a new API behavior contract.

| Rubric | API surface | Controller | Service | Repository / Mapper | Verification |
| --- | --- | --- | --- | --- | --- |
| `#01` Core CRUD, `#03` detail lookup | `POST/GET/PATCH/DELETE /api/v1/groups/catalog`, `GET /api/v1/groups/{groupId}` | `src/main/java/com/studypot/aistudyleader/studygroup/catalog/controller/StudyGroupCatalogController.java`, `src/main/java/com/studypot/aistudyleader/studygroup/controller/StudyGroupController.java` | `src/main/java/com/studypot/aistudyleader/studygroup/catalog/StudyGroupCatalogService.java`, `src/main/java/com/studypot/aistudyleader/studygroup/service/StudyGroupService.java` | `src/main/java/com/studypot/aistudyleader/studygroup/catalog/StudyGroupCatalogMapper.java`, `src/main/java/com/studypot/aistudyleader/studygroup/catalog/StudyGroupCatalogSqlProvider.java`, `src/main/java/com/studypot/aistudyleader/studygroup/repository/JdbcStudyGroupRepository.java` | `src/test/java/com/studypot/aistudyleader/studygroup/controller/StudyGroupControllerTest.java` |
| `#02` list/search/sort | `GET /api/v1/groups/catalog?keyword&status&sort&pageSize&cursor`, `GET /api/v1/groups/{groupId}/boards/{boardId}/posts?sort&order&cursor` | `src/main/java/com/studypot/aistudyleader/studygroup/catalog/controller/StudyGroupCatalogController.java`, `src/main/java/com/studypot/aistudyleader/studygroup/board/controller/GroupBoardController.java` | `src/main/java/com/studypot/aistudyleader/studygroup/catalog/StudyGroupCatalogService.java`, `src/main/java/com/studypot/aistudyleader/studygroup/board/service/GroupBoardService.java` | `src/main/java/com/studypot/aistudyleader/studygroup/repository/StudyGroupMyBatisSqlProvider.java`, `src/main/java/com/studypot/aistudyleader/studygroup/catalog/StudyGroupCatalogSqlProvider.java`, `src/main/java/com/studypot/aistudyleader/studygroup/board/repository/GroupBoardJdbcSql.java` | `src/test/java/com/studypot/aistudyleader/studygroup/repository/StudyGroupMyBatisSqlProviderTest.java`, `src/test/java/com/studypot/aistudyleader/studygroup/board/controller/GroupBoardControllerTest.java` |
| `#04` reviews | `GET/POST /api/v1/groups/{groupId}/reviews`, `GET /api/v1/groups/{groupId}/reviews/me`, `GET /api/v1/groups/{groupId}/reviews/stats`, `DELETE /api/v1/reviews/{reviewId}` | `src/main/java/com/studypot/aistudyleader/review/controller/ReviewController.java` | `src/main/java/com/studypot/aistudyleader/review/ReviewService.java` | `src/main/java/com/studypot/aistudyleader/review/ReviewRepository.java`, `src/main/java/com/studypot/aistudyleader/review/InMemoryReviewRepository.java` | `src/test/java/com/studypot/aistudyleader/review/ReviewServiceTest.java` |
| `#06` signup | `POST /api/v1/auth/signup`, `GET /api/v1/auth/signup/email-availability` | `src/main/java/com/studypot/aistudyleader/auth/controller/SignupController.java` | `src/main/java/com/studypot/aistudyleader/auth/service/SignupService.java` | `src/main/java/com/studypot/aistudyleader/auth/repository/JdbcAuthAccountRepository.java`, `src/main/java/com/studypot/aistudyleader/auth/repository/AuthJdbcSql.java`, `src/main/resources/db/migration/V5__users_password_hash.sql` | `src/test/java/com/studypot/aistudyleader/auth/service/SignupServiceTest.java` |
| `#07` profile | `GET/PATCH /api/v1/users/me`, `GET/PATCH /api/v1/groups/{groupId}/members/me/profile` | `src/main/java/com/studypot/aistudyleader/auth/controller/AuthController.java`, `src/main/java/com/studypot/aistudyleader/studygroup/controller/StudyGroupController.java` | `src/main/java/com/studypot/aistudyleader/auth/service/AuthSessionService.java`, `src/main/java/com/studypot/aistudyleader/studygroup/service/StudyGroupService.java` | `src/main/java/com/studypot/aistudyleader/auth/repository/JdbcAuthAccountRepository.java`, `src/main/java/com/studypot/aistudyleader/studygroup/repository/JdbcStudyGroupRepository.java` | `src/test/java/com/studypot/aistudyleader/auth/controller/AuthControllerTest.java`, `src/test/java/com/studypot/aistudyleader/studygroup/service/StudyGroupServiceTest.java` |
| `#10` board CRUD, `#11` comment CRUD | `GET/POST/PATCH/DELETE /api/v1/groups/{groupId}/boards...`, `GET/POST/PATCH/DELETE /api/v1/groups/{groupId}/comments...` | `src/main/java/com/studypot/aistudyleader/studygroup/board/controller/GroupBoardController.java` | `src/main/java/com/studypot/aistudyleader/studygroup/board/service/GroupBoardService.java` | `src/main/java/com/studypot/aistudyleader/studygroup/board/repository/JdbcGroupBoardRepository.java`, `src/main/java/com/studypot/aistudyleader/studygroup/board/repository/GroupBoardJdbcSql.java` | `src/test/java/com/studypot/aistudyleader/studygroup/board/controller/GroupBoardControllerTest.java`, `src/test/java/com/studypot/aistudyleader/studygroup/board/service/GroupBoardServiceTest.java` |

## Endpoint Index
| Method | Path | Feature ID | Actor | Purpose |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/auth/signup` | `identity-core` | anonymous/client | Create email/password account with BCrypt password hashing. |
| `GET` | `/api/v1/auth/signup/email-availability` | `identity-core` | anonymous/client | Check whether an email can be used for signup. |
| `POST` | `/api/v1/auth/oauth/google` | `identity-core` | anonymous/client | Exchange a Google authorization code for application tokens. |
| `POST` | `/api/v1/auth/refresh` | `identity-core` | anonymous/client | Rotate refresh token and issue new application tokens. |
| `POST` | `/api/v1/auth/logout` | `identity-core` | authenticated | Revoke the submitted current refresh token. |
| `POST` | `/api/v1/auth/logout-all` | `identity-core` | authenticated | Revoke all refresh tokens for the current user. |
| `GET` | `/api/v1/users/me` | `identity-core` | authenticated | Read current user. |
| `PATCH` | `/api/v1/users/me` | `identity-core` | authenticated | Update current user's nickname, image, bio, preferred topics, and skill level. |
| `POST` | `/api/v1/groups/catalog` | `study-group-core` | authenticated | Create a catalog-visible study group. |
| `GET` | `/api/v1/groups/catalog` | `study-group-core` | authenticated | Search, sort, and cursor-page catalog-visible study groups. |
| `GET` | `/api/v1/groups/catalog/{groupId}` | `study-group-core` | authenticated | Read catalog-visible study group detail with aggregate fields. |
| `PATCH` | `/api/v1/groups/catalog/{groupId}` | `study-group-core` | authenticated | Update catalog-visible study group fields. |
| `DELETE` | `/api/v1/groups/catalog/{groupId}` | `study-group-core` | authenticated | Delete a catalog-visible study group. |
| `GET` | `/api/v1/groups` | `study-group-core` | authenticated | List my groups. |
| `POST` | `/api/v1/groups` | `study-group-core` | authenticated | Create group and owner membership. |
| `GET` | `/api/v1/groups/{groupId}` | `study-group-core` | group member | Read group. |
| `PATCH` | `/api/v1/groups/{groupId}` | `study-group-core` | owner | Update mutable group fields before active lock. |
| `POST` | `/api/v1/groups/{groupId}/join` | `study-group-core` | authenticated | Join by invite code/link. |
| `GET` | `/api/v1/groups/{groupId}/members` | `study-group-core` | group member | List members and onboarding status. |
| `GET` | `/api/v1/groups/{groupId}/members/me/profile` | `study-group-core` | current group member | Read my group-scoped member profile and current study summaries. |
| `PATCH` | `/api/v1/groups/{groupId}/members/me/profile` | `study-group-core` | current group member | Update my group-scoped display name. |
| `GET` | `/api/v1/groups/{groupId}/boards` | `study-group-board` | active group member | List or initialize default boards. |
| `GET` | `/api/v1/groups/{groupId}/boards/{boardId}/posts` | `study-group-board` | active group member | List board posts. |
| `POST` | `/api/v1/groups/{groupId}/boards/{boardId}/posts` | `study-group-board` | active group member | Create board post. |
| `GET` | `/api/v1/groups/{groupId}/posts/{postId}` | `study-group-board` | active group member | Read board post. |
| `PATCH` | `/api/v1/groups/{groupId}/posts/{postId}` | `study-group-board` | post author or owner | Update post content or pinned state. |
| `DELETE` | `/api/v1/groups/{groupId}/posts/{postId}` | `study-group-board` | post author or owner | Delete board post. |
| `GET` | `/api/v1/groups/{groupId}/posts/{postId}/comments` | `study-group-board` | active group member | List post comments. |
| `POST` | `/api/v1/groups/{groupId}/posts/{postId}/comments` | `study-group-board` | active group member | Create post comment. |
| `PATCH` | `/api/v1/groups/{groupId}/comments/{commentId}` | `study-group-board` | comment author | Update comment. |
| `DELETE` | `/api/v1/groups/{groupId}/comments/{commentId}` | `study-group-board` | comment author or owner | Delete comment. |
| `POST` | `/api/v1/groups/{groupId}/start` | `curriculum-core` | owner | Start study, derive fixed one-week sprint windows, and generate curriculum. |
| `GET` | `/api/v1/groups/{groupId}/onboarding/me` | `group-onboarding` | group member | Read my onboarding response. |
| `POST` | `/api/v1/groups/{groupId}/onboarding/me` | `group-onboarding` | group member | Submit onboarding with overall skill level, note, and availability. |
| `GET` | `/api/v1/groups/{groupId}/curriculum` | `curriculum-core` | group member | Read active curriculum. |
| `GET` | `/api/v1/groups/{groupId}/weeks/current` | `weekly-todo` | group member | Read current week metadata. |
| `GET` | `/api/v1/groups/{groupId}/learning-activity/me` | `weekly-todo` | active group member | Read group-home current learning activity with current week, my progress, tasks, and my task completion states. |
| `GET` | `/api/v1/weeks/{weekId}/tasks` | `weekly-todo` | group member | List weekly tasks. |
| `GET` | `/api/v1/weeks/{weekId}/progress/me` | `weekly-todo` | group member | Read my member week progress. |
| `PUT` | `/api/v1/weeks/{weekId}/progress/me` | `weekly-todo` | group member | Update member week progress note/status. |
| `POST` | `/api/v1/tasks/{taskId}/completion/me` | `weekly-todo` | group member | Complete, skip, or mark task incomplete. |
| `POST` | `/api/v1/tasks/{taskId}/completion/me/done` | `weekly-todo` | group member | Mark my task completion as done. |
| `POST` | `/api/v1/tasks/{taskId}/completion/me/incomplete` | `weekly-todo` | group member | Mark my task completion as incomplete with a required reason. |
| `POST` | `/api/v1/tasks/{taskId}/completion/me/skip` | `weekly-todo` | group member | Mark my task completion as skipped. |
| `GET` | `/api/v1/groups/{groupId}/reviews` | `retrospective-feedback` | authenticated | List reviews for a group. |
| `POST` | `/api/v1/groups/{groupId}/reviews` | `retrospective-feedback` | authenticated | Create one review per author and group. |
| `GET` | `/api/v1/groups/{groupId}/reviews/me` | `retrospective-feedback` | review author | Read my group review. |
| `GET` | `/api/v1/groups/{groupId}/reviews/stats` | `retrospective-feedback` | authenticated | Read group review count, average rating, and rating distribution. |
| `GET` | `/api/v1/review-targets/{targetId}/reviews` | `retrospective-feedback` | authenticated | List reviews for a target. |
| `POST` | `/api/v1/review-targets/{targetId}/reviews` | `retrospective-feedback` | authenticated | Create one review per author and target. |
| `GET` | `/api/v1/review-targets/{targetId}/reviews/summary` | `retrospective-feedback` | authenticated | Read review count and average rating. |
| `DELETE` | `/api/v1/reviews/{reviewId}` | `retrospective-feedback` | review author | Delete own review after author validation. |
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
`startsAt`/`endsAt` are later used by host start to derive fixed one-week curriculum sprint windows. Sprint duration is not configurable in the current API.

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

Response:
```json
{
  "keywords": ["JPA", "Spring Security", "Spring Batch"]
}
```

Suggested keyword candidates are transient and are not persisted unless selected or directly entered in the later create-group request.

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

Public onboarding responses expose `skillLevel`, not internal keyword score or task preference maps.

### Group Member Profile
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

- Approved by `CR-20260601-group-member-profile-api`.
- `displayName` is the study-group member display name stored in `group_member.display_name`, not the global account profile.
- PATCH is limited to 1 to 80 non-blank `displayName` characters.
- Only current `PENDING_ONBOARDING` or `ACTIVE` members can read/update their own profile. Missing groups return not found; existing groups without current membership return forbidden.

### Current Learning Activity
`GET /api/v1/groups/{groupId}/learning-activity/me` returns the current group-home learning activity read model:

- `currentWeek`: current `CurriculumWeekResponse`.
- `progress`: existing `MemberWeekProgressResponse`, or null when no progress row exists.
- `progressStatus`: existing progress status, or `NOT_STARTED` when progress is null.
- `taskCompletion`: total/done/incomplete/skipped counts for the current member.
- `tasks`: each current-week task plus the current member's completion snapshot. Missing completion rows are returned as `status = TODO`.

This endpoint does not create progress or completion rows. Only `ACTIVE` group members can read it.

### Study Group Board
- Approved by `CR-20260601-study-group-board-api`.
- Default board types are `NOTICE`, `QUESTION`, `RESOURCE`, and `RETROSPECTIVE`; `GET /groups/{groupId}/boards` initializes missing default rows idempotently.
- Posts require non-blank `title` up to 200 characters and `content` up to 10,000 characters.
- Comments require non-blank `content` up to 3,000 characters.
- Post lists use pinned-first newest-first cursor pagination. Comment lists use creation-order cursor pagination.
- Only `ACTIVE` members can read/write boards. Authors can edit/delete their own content. OWNER users can delete any post/comment and update post `pinned`, but cannot rewrite another member's title/content.
- Uploads, mentions, reactions, search, tags, realtime board events, board notifications, and AI summaries are deferred.

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

- `DONE`: `completionNote` and `evidenceUrl` are optional; repeated `DONE` requests preserve the first completion values.
- `INCOMPLETE`: `incompleteReason` is required and `reasonSubmittedAt` is returned.
- `SKIPPED`: completion, incomplete, and evidence fields are empty.
- Button-friendly wrappers are available at `/completion/me/done`, `/completion/me/incomplete`, and `/completion/me/skip`; they delegate to the same state machine and return the same response.
- Only active members of the task's group can update their own completion.

## State Transition Rules
- `study_group`: `DRAFT -> ONBOARDING -> READY_TO_START -> ACTIVE -> COMPLETED`; `ARCHIVED` can be applied by owner/admin flows.
- `group_member`: `PENDING_ONBOARDING -> ACTIVE -> LEFT`.
- `group_board_post`: `PUBLISHED -> DELETED`.
- `group_board_comment`: `PUBLISHED -> DELETED`.
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
- Active members may read curriculum/current week, read their own week progress, complete their own tasks, request their own retrospective, and chat with the AI team leader.
- Notification list/read endpoints are scoped to the authenticated recipient user.
- Cross-group access must be rejected even when a resource ID exists.

## Notification API Contract
- MVP channel is `IN_APP`.
- Recipients can list and mark only their own notifications as read.
- Owner group notification logs are audit-only for MVP; owners cannot mutate another member's read state.
- Notification rows must retain recipient, title/body, payload, related resource IDs, idempotency key, status, delivered timestamp, and read timestamp.

## Verification
- OpenAPI YAML must parse before PR review.
- Current local parse: `openapi=3.1.0`, `paths=40`, `schemas=65`.
- Standard repo verification: `./gradlew check build --no-daemon`.

## 추적 링크
- Human API contract: `docs/specs/api-contract-v1.md`
- Machine API contract: `docs/specs/openapi.yaml`
- Permissions: `docs/specs/auth-permissions-v1.md`
- Notification contract: `docs/specs/notification-contract-v1.md`
- QA acceptance: `docs/specs/qa-acceptance-v1.md`
- Jira: `SPT-7`
