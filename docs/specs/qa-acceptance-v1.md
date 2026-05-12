# AI Study Leader QA Acceptance v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.

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
| `study-group-core` | `QA-GRP-001` | Group creation requires name, topic, detail keywords, max members, and period. |
| `study-group-core` | `QA-GRP-002` | Creator becomes owner member with onboarding-aware status. |
| `study-group-core` | `QA-GRP-003` | Invite join respects max member count and duplicate membership rules. |
| `study-group-core` | `QA-GRP-004` | Owner-only group updates reject non-owner access. |
| `group-onboarding` | `QA-ONB-001` | Host and members can save and submit onboarding. |
| `group-onboarding` | `QA-ONB-002` | Keyword skill levels and task preferences reject values outside 1 to 5. |
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
| `retrospective-feedback` | `QA-RETRO-001` | Retrospective can be requested from current week progress. |
| `retrospective-feedback` | `QA-RETRO-002` | AI feedback and next-week adjustment are stored as JSON. |
| `retrospective-feedback` | `QA-RETRO-003` | Failed AI generation leaves retriable failed status. |
| `retrospective-feedback` | `QA-RETRO-004` | Member can read own retrospective only. |
| `ai-team-leader` | `QA-AI-001` | LLM usage is recorded for every AI call. |
| `ai-team-leader` | `QA-AI-002` | AI conversation stores user and assistant messages. |
| `ai-team-leader` | `QA-AI-003` | Redacted request metadata excludes secrets/tokens. |
| `ai-team-leader` | `QA-AI-004` | Invalid AI JSON output is rejected or marked failed. |
| `notification` | `QA-NOTI-001` | In-app notification stores recipient, title/body, idempotency key, status, and related resource links. |
| `notification` | `QA-NOTI-002` | Duplicate idempotency key does not create duplicate recipient notifications. |
| `notification` | `QA-NOTI-003` | Recipients can list and mark only their own notifications as read. |
| `notification` | `QA-NOTI-004` | Notification failure does not roll back core domain event. |

## Required Scenario Tests
- Google OAuth login -> read current user -> refresh token rotation -> old refresh token reuse rejected -> logout current session.
- Login from two sessions -> logout-all -> both refresh tokens rejected.
- Create group -> submit host onboarding -> invite member -> submit member onboarding -> host start.
- Host start with only partial onboarding completion.
- Current week task completion before due date.
- Current week progress read after progress creation.
- Overdue incomplete reason modal path.
- AI retrospective feedback with next-week adjustment.
- AI conversation message persistence and LLM usage logging.
- In-app notification idempotency and read-state update.
