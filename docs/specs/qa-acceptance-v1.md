# AI Study Leader QA Acceptance v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.
- Retrospective/chat context boundary is authorized by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md) and [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md).
- Onboarding simplification is authorized by [CR-20260520-onboarding-simplification-auto-merge](./change-requests/CR-20260520-onboarding-simplification-auto-merge.md) and [ADR-20260520-onboarding-simplification-auto-merge](./adr/ADR-20260520-onboarding-simplification-auto-merge.md).
- Notification SSE stream is authorized by [CR-20260601-notification-sse-stream](./change-requests/CR-20260601-notification-sse-stream.md) and [ADR-20260601-notification-sse-stream](./adr/ADR-20260601-notification-sse-stream.md).
- AI conversation SSE stream and message-list recovery are authorized by [CR-20260601-ai-conversation-sse-stream](./change-requests/CR-20260601-ai-conversation-sse-stream.md) and [ADR-20260601-ai-conversation-sse-stream](./adr/ADR-20260601-ai-conversation-sse-stream.md).
- Group member profile read/update is authorized by [CR-20260601-group-member-profile-api](./change-requests/CR-20260601-group-member-profile-api.md) and [ADR-20260601-group-member-profile-api](./adr/ADR-20260601-group-member-profile-api.md).
- Study group board APIs are authorized by [CR-20260601-study-group-board-api](./change-requests/CR-20260601-study-group-board-api.md) and [ADR-20260601-study-group-board-api](./adr/ADR-20260601-study-group-board-api.md).

## Global QA
| ID | Acceptance |
| --- | --- |
| `QA-GLOBAL-001` | `./gradlew check build --no-daemon` passes before merge. |
| `QA-GLOBAL-002` | OpenAPI YAML parses and contains `openapi`, `info.title`, and `paths`. |
| `QA-GLOBAL-003` | DB schema includes all ERD v0.8 tables. |
| `QA-GLOBAL-004` | Jira task has source document links and current labels. |
| `QA-GLOBAL-005` | No active Jira issue keeps stale `erd-v06`, `erd-v07`, or `meeting` labels. |

## Feature QA
| Feature ID | QA IDs | Acceptance |
| --- | --- | --- |
| `identity-core` | `QA-ID-001` | User profile can be read with authenticated context. |
| `identity-core` | `QA-ID-002` | OAuth/refresh token records are stored without raw token leakage. |
| `identity-core` | `QA-ID-003` | Refresh token revocation prevents reuse. |
| `identity-core` | `QA-ID-004` | Google OAuth authorization-code exchange creates or updates the user, OAuth account, and application token pair. |
| `identity-core` | `QA-ID-005` | Refresh token rotation returns a new token pair and rejects reuse of the old refresh token. |
| `identity-core` | `QA-ID-006` | Current-session logout and logout-all revoke the intended refresh tokens. |
| `identity-core` | `QA-ID-007` | Missing, invalid, expired, revoked, or reused refresh tokens return a stable 401 Problem Detail and clear application token cookies when present. |
| `study-group-core` | `QA-GRP-001` | Group creation requires name, topic, detail keywords, max members, and period. |
| `study-group-core` | `QA-GRP-002` | Creator becomes owner member with onboarding-aware status. |
| `study-group-core` | `QA-GRP-003` | Invite join respects max member count and duplicate membership rules. |
| `study-group-core` | `QA-GRP-004` | Owner-only group updates reject non-owner access. |
| `study-group-core` | `QA-GRP-005` | Current members can read/update their own group member profile, while non-members and LEFT members are rejected. |
| `study-group-board` | `QA-BOARD-001` | Active members can list group boards and missing default boards are initialized idempotently. |
| `study-group-board` | `QA-BOARD-002` | Active members can create and read board posts, and invalid title/content/page cursor values return validation problems. |
| `study-group-board` | `QA-BOARD-003` | Post list pagination orders pinned posts first, then newest posts, and returns a next cursor when more rows exist. |
| `study-group-board` | `QA-BOARD-004` | Authors can update/delete their own posts; OWNER users can pin/delete; non-author OWNER users cannot rewrite another member's title/content. |
| `study-group-board` | `QA-BOARD-005` | Active members can create/list/update/delete comments, with author-only content edits and OWNER moderation deletes. |
| `study-group-board` | `QA-BOARD-006` | Non-members, PENDING_ONBOARDING members, LEFT members, deleted memberships, and cross-group resource access are rejected. |
| `group-onboarding` | `QA-ONB-001` | Host and members can submit onboarding in one request. |
| `group-onboarding` | `QA-ONB-002` | Overall skill level rejects values outside 1 to 5. |
| `group-onboarding` | `QA-ONB-003` | Availability slots reject invalid day/time windows. |
| `group-onboarding` | `QA-ONB-004` | One member cannot create duplicate active onboarding responses. |
| `curriculum-core` | `QA-CUR-001` | Host start creates curriculum from submitted onboarding summary. |
| `curriculum-core` | `QA-CUR-002` | Host can start without waiting for every invitee. |
| `curriculum-core` | `QA-CUR-003` | Late joiner onboarding does not auto-regenerate initial curriculum. |
| `weekly-todo` | `QA-TODO-001` | Weekly tasks are listed for active members. |
| `weekly-todo` | `QA-TODO-002` | Member can complete own task with timestamp and note. |
| `weekly-todo` | `QA-TODO-003` | Overdue incomplete task requires incomplete reason. |
| `weekly-todo` | `QA-TODO-004` | Cross-member completion updates are rejected. |
| `weekly-todo` | `QA-TODO-005` | Member can read own existing week progress without mutating it. |
| `weekly-todo` | `QA-TODO-006` | Task completion responses include frontend display fields and reject cross-group task access. |
| `retrospective-feedback` | `QA-RETRO-001` | Retrospective can be requested from current week progress. |
| `retrospective-feedback` | `QA-RETRO-002` | AI feedback and next-week adjustment are stored as JSON using DB-first context from progress, tasks, completions, incomplete reasons, rules/violations, prior feedback, and conversation summary. |
| `retrospective-feedback` | `QA-RETRO-003` | Failed AI generation leaves retriable failed status. |
| `retrospective-feedback` | `QA-RETRO-004` | Member can read own retrospective only. |
| `ai-team-leader` | `QA-AI-001` | LLM usage is recorded for every AI call, including redacted context/source metadata for retrospective/chat calls. |
| `ai-team-leader` | `QA-AI-002` | AI conversation stores user and assistant messages. |
| `ai-team-leader` | `QA-AI-003` | Redacted request metadata excludes secrets/tokens. |
| `ai-team-leader` | `QA-AI-004` | Invalid AI JSON output is rejected or marked failed. |
| `ai-team-leader` | `QA-AI-005` | Detail keyword suggestion API returns a non-empty `keywords` list from an authenticated pre-creation request and does not persist candidates. |
| `ai-team-leader` | `QA-AI-006` | Active conversation members can list and subscribe to their own AI conversation messages through SSE, receive user/assistant success/failure lifecycle events, and disconnected streams are cleaned up. |
| `notification` | `QA-NOTI-001` | In-app notification stores recipient, title/body, idempotency key, status, and related resource links. |
| `notification` | `QA-NOTI-002` | Duplicate idempotency key does not create duplicate recipient notifications. |
| `notification` | `QA-NOTI-003` | Recipients can list and mark only their own notifications as read. |
| `notification` | `QA-NOTI-004` | Notification failure does not roll back core domain event. |
| `notification` | `QA-NOTI-005` | Authenticated SSE subscribers receive only their own newly created in-app notifications, including notifications from other groups, and disconnected streams are cleaned up. |

## Required Scenario Tests
- Google OAuth login -> read current user -> refresh token rotation -> old refresh token reuse rejected -> logout current session.
- Login from two sessions -> logout-all -> both refresh tokens rejected.
- Create group -> submit host onboarding -> invite member -> submit member onboarding -> host start.
- Group-scoped my profile read/update returns member, onboarding, current-week, task-completion, and retrospective summaries and rejects non-member access.
- Group board default initialization -> post create/list/read/update/delete -> comment create/list/update/delete with author, owner, inactive member, and cross-group rejection checks.
- Host start with only partial onboarding completion.
- Current week task completion before due date.
- Current week progress read after progress creation.
- Overdue incomplete reason modal path.
- Task completion response rendering for done, incomplete, skipped, idempotent repeated done, pending-member rejection, and cross-group task rejection.
- AI retrospective feedback with next-week adjustment.
- Retrospective/chat context builder excludes cross-member private raw notes while preserving allowed group-level summaries.
- AI conversation message persistence and LLM usage logging.
- AI conversation message-list recovery and SSE lifecycle events for user save, assistant generation start, assistant success, assistant failure, send-failure isolation, and disconnect cleanup.
- In-app notification idempotency and read-state update.
- In-app notification SSE subscribe, recipient isolation, cross-group recipient delivery, send-failure isolation, and disconnect cleanup.
