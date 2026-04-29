# Requirements v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Requirement IDs are stable for v1.
- Changes require Change Request and ADR.

## Global Requirements
| ID | Requirement | Acceptance |
| --- | --- | --- |
| `REQ-GLOBAL-001` | All public APIs are under `/api/v1`. | OpenAPI has no public path outside `/api/v1`. |
| `REQ-GLOBAL-002` | All API errors use `application/problem+json`. | Error responses reference `ProblemDetail`. |
| `REQ-GLOBAL-003` | All list APIs use cursor pagination. | List responses include `items` and `pageInfo`. |
| `REQ-GLOBAL-004` | All mutable records use soft delete. | ERD and DDL include `deleted_at`. |
| `REQ-GLOBAL-005` | UUIDv7 IDs are supplied by application code. | DDL primary keys are `uuid`; implementation must not use random UUIDv4 defaults. |

## Feature Requirements
| Feature ID | Requirement ID | Requirement | Acceptance |
| --- | --- | --- | --- |
| `identity-core` | `REQ-ID-001` | Users authenticate with Google OAuth. | OAuth account is stored in `user_oauth_accounts`; current user endpoint returns profile. |
| `identity-core` | `REQ-ID-002` | Users can link and unlink one primary Discord account. | `user_discord_accounts` stores Discord ID and `is_primary`. |
| `identity-core` | `REQ-ID-003` | Discord ID is not stored on core study tables. | DDL has Discord IDs only in Discord integration tables. |
| `study-group-core` | `REQ-GRP-001` | Authenticated users can create study groups. | Creator becomes active `owner`. |
| `study-group-core` | `REQ-GRP-002` | Owners/managers can invite members. | Invitation supports email, Discord user ID, or link-only token. |
| `study-group-core` | `REQ-GRP-003` | Group membership has role and status. | Roles are `owner`, `manager`, `member`; statuses are `active`, `paused`, `left`, `removed`. |
| `study-group-rules` | `REQ-RULE-001` | Group rules are stored in `study_groups.rules JSONB`. | Rules include attendance, meeting, and AI feedback policy sections. |
| `study-group-rules` | `REQ-RULE-002` | Schedule defaults are stored in `study_groups.schedule_defaults JSONB`. | Defaults include duration, recurrence, and reminder offsets. |
| `study-session-core` | `REQ-SES-001` | Managers can create scheduled sessions. | Session gets group-local `sequence_no`. |
| `study-session-core` | `REQ-SES-002` | Attendance is tracked per session/member. | Unique active row exists for `(session_id, member_id)`. |
| `structured-notes` | `REQ-NOTE-001` | Members can submit structured notes. | Notes support `pre_note`, `post_note`, `decision`, `blocker`, `summary`. |
| `structured-notes` | `REQ-ACT-001` | Sessions can have action items. | Action items support assignee, source note, due date, and status. |
| `study-group-core` | `REQ-GOAL-001` | Groups can define goals. | Goals can be group-level or member-owned. |
| `structured-notes` | `REQ-PROG-001` | Members can log progress. | Logs attach to member and optional goal/session. |
| `study-group-core` | `REQ-RES-001` | Groups can save study resources. | Resources support URL, type, title, description, and metadata. |
| `ai-prep-brief` | `REQ-AI-PREP-001` | AI can generate pre-session preparation briefs. | Output follows `PreparationBriefV1`. |
| `ai-feedback-report` | `REQ-AI-FB-001` | AI can generate post-session group feedback. | Output follows `FeedbackReportV1` with `reportType = group`. |
| `ai-feedback-report` | `REQ-AI-FB-002` | AI can generate individual member feedback. | Output follows `FeedbackReportV1` with `reportType = individual`. |
| `ai-prep-brief` | `REQ-AI-RUN-001` | Every AI generation is traceable. | `ai_prompt_runs` records type, provider, model, status, input snapshot, output payload, and token counts. |
| `discord-notifications` | `REQ-DIS-001` | Groups can connect Discord notification channels. | Active channel row stores guild, channel, status, and settings. |
| `discord-notifications` | `REQ-DIS-002` | MVP sends notification-only events. | Supported types are `session_reminder`, `prep_brief`, `feedback_ready`, `action_item_due`. |
| `discord-notifications` | `REQ-DIS-003` | Delivery attempts are logged. | `discord_notification_logs` records payload, scheduled time, sent time, status, and error. |

## Exception And Failure Requirements
| ID | Scenario | Required Behavior |
| --- | --- | --- |
| `REQ-ERR-001` | Unauthorized request | Return `401` ProblemDetail. |
| `REQ-ERR-002` | Authenticated but not permitted | Return `403` ProblemDetail. |
| `REQ-ERR-003` | Resource not found or soft-deleted | Return `404` ProblemDetail. |
| `REQ-ERR-004` | Duplicate active unique resource | Return `409` ProblemDetail. |
| `REQ-ERR-005` | Invalid request payload | Return `422` ProblemDetail with field errors. |
| `REQ-ERR-006` | AI provider failure | Persist failed `ai_prompt_runs`; return retryable ProblemDetail. |
| `REQ-ERR-007` | Discord delivery failure | Persist failed `discord_notification_logs`; do not fail unrelated business transaction. |

## Traceability Rule
Every implementation EXEC_PLAN must include the relevant `Feature ID` and at least one `Requirement ID` in Doc Notes.
