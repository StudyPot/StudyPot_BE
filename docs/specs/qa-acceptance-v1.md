# QA Acceptance v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- QA scenarios are required before implementation is considered complete.
- Changes require Change Request and ADR.

## Global Acceptance
| ID | Scenario | Expected Result |
| --- | --- | --- |
| `QA-GLOBAL-001` | Invalid request payload | API returns `422` ProblemDetail with field errors. |
| `QA-GLOBAL-002` | Missing auth | API returns `401` ProblemDetail. |
| `QA-GLOBAL-003` | Authenticated user lacks group role | API returns `403` ProblemDetail. |
| `QA-GLOBAL-004` | Soft-deleted resource is requested | API returns `404` ProblemDetail. |
| `QA-GLOBAL-005` | List endpoint receives no cursor | API returns first page with `pageInfo`. |
| `QA-GLOBAL-006` | List endpoint receives valid cursor | API returns next page with stable ordering. |

## Feature Acceptance Matrix
| Feature ID | QA IDs |
| --- | --- |
| `identity-core` | `QA-ID-001`, `QA-ID-002`, `QA-ID-003` |
| `study-group-core` | `QA-GRP-001`, `QA-GRP-002`, `QA-GRP-003`, `QA-INV-001` |
| `study-group-rules` | `QA-RULE-001`, `QA-RULE-002` |
| `study-session-core` | `QA-SES-001`, `QA-SES-002`, `QA-ATT-001` |
| `structured-notes` | `QA-NOTE-001`, `QA-ACT-001`, `QA-PROG-001` |
| `ai-prep-brief` | `QA-AI-PREP-001`, `QA-AI-RUN-001` |
| `ai-feedback-report` | `QA-AI-FB-001`, `QA-AI-FB-002`, `QA-AI-PRIV-001` |
| `discord-notifications` | `QA-DIS-001`, `QA-DIS-002`, `QA-DIS-003` |

## Scenario Details
| ID | Scenario | Expected Result |
| --- | --- | --- |
| `QA-ID-001` | New Google OAuth user signs in. | `users` and `user_oauth_accounts` are created; `/users/me` returns active user. |
| `QA-ID-002` | Existing Google OAuth user signs in. | Existing user is reused; no duplicate active OAuth account. |
| `QA-ID-003` | User links Discord account already linked elsewhere. | API returns `409`; no second active link is created. |
| `QA-GRP-001` | Authenticated user creates group. | Group and owner membership are created. |
| `QA-GRP-002` | Manager updates group rules. | `study_groups.rules` updates and remains valid JSON object. |
| `QA-GRP-003` | Member tries to archive group. | API returns `403`. |
| `QA-INV-001` | Invitee accepts valid invitation. | Invitation becomes `accepted`; membership becomes active. |
| `QA-RULE-001` | Rules payload misses required policy section. | API returns `422`. |
| `QA-RULE-002` | Schedule defaults contain invalid reminder offset. | API returns `422`. |
| `QA-SES-001` | Manager creates session. | Session gets next `sequence_no`. |
| `QA-SES-002` | scheduled end is before start. | API returns `422`. |
| `QA-ATT-001` | Member upserts own attendance. | One active attendance row exists for session/member. |
| `QA-NOTE-001` | Member submits post note. | Note is stored with valid `structured_payload`. |
| `QA-ACT-001` | Manager assigns action item. | Action item is visible in session action item list. |
| `QA-PROG-001` | Member logs progress for goal. | Progress log links member and goal. |
| `QA-AI-PREP-001` | Manager generates preparation brief. | `ai_prompt_runs` succeeds and `ai_preparation_briefs` stores `PreparationBriefV1`. |
| `QA-AI-RUN-001` | AI provider fails. | Failed `ai_prompt_runs` is stored; API returns retryable ProblemDetail. |
| `QA-AI-FB-001` | Manager generates group feedback. | Group report is visible to all active members. |
| `QA-AI-FB-002` | Manager generates individual feedback. | Target member report is stored with `targetMemberId`. |
| `QA-AI-PRIV-001` | Member reads another member's individual feedback. | API returns `403`. |
| `QA-DIS-001` | Manager connects Discord channel. | Active `discord_notification_channels` row is created. |
| `QA-DIS-002` | Session reminder send succeeds. | Notification log becomes `sent`. |
| `QA-DIS-003` | Discord access revoked. | Channel becomes `revoked`; notification log becomes `failed`. |

## Release Readiness Checklist
- OpenAPI parses successfully.
- DDL draft defines all 18 ERD tables.
- Every feature_id has requirements and QA IDs.
- Every protected endpoint has permission coverage.
- AI schemas validate success and failure examples.
- Discord failure does not roll back business event.
- No v1 document contains unlocked product questions.
