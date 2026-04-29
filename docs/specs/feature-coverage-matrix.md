# AI Study Leader Feature Coverage Matrix

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Lock unit: full v1 planning package.
- Changes require Change Request and ADR.

## Purpose
This matrix maps every MVP `feature_id` to source documents, API contracts, DB contracts, integration contracts, and QA evidence requirements.

| Feature ID | PRD | Requirements | API | DB | Integration | QA | Implementation Evidence | Test Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `identity-core` | `prd-v1.md` | `REQ-ID-001` to `REQ-ID-003` | `api-contract-v1.md`, `openapi.yaml` | `users`, `user_oauth_accounts`, `user_discord_accounts` | `auth-permissions-v1.md` | `QA-ID-001` to `QA-ID-003` | Not started | Not started | Planned |
| `study-group-core` | `prd-v1.md` | `REQ-GRP-001` to `REQ-GRP-003`, `REQ-INV-001`, `REQ-GOAL-001`, `REQ-RES-001` | `api-contract-v1.md`, `openapi.yaml` | `study_groups`, `study_group_members`, `study_group_invitations`, `study_goals`, `study_resources` | `auth-permissions-v1.md` | `QA-GRP-001` to `QA-GRP-003`, `QA-INV-001` | Not started | Not started | Planned |
| `study-group-rules` | `prd-v1.md` | `REQ-RULE-001`, `REQ-RULE-002` | `api-contract-v1.md`, `openapi.yaml` | `study_groups.rules`, `study_groups.schedule_defaults` | `auth-permissions-v1.md` | `QA-RULE-001`, `QA-RULE-002` | Not started | Not started | Planned |
| `study-session-core` | `prd-v1.md` | `REQ-SES-001`, `REQ-SES-002`, `REQ-ATT-001` | `api-contract-v1.md`, `openapi.yaml` | `study_sessions`, `study_session_attendance` | `auth-permissions-v1.md` | `QA-SES-001`, `QA-SES-002`, `QA-ATT-001` | Not started | Not started | Planned |
| `structured-notes` | `prd-v1.md` | `REQ-NOTE-001`, `REQ-ACT-001`, `REQ-PROG-001` | `api-contract-v1.md`, `openapi.yaml` | `study_session_notes`, `study_session_action_items`, `member_progress_logs` | `auth-permissions-v1.md` | `QA-NOTE-001`, `QA-ACT-001`, `QA-PROG-001` | Not started | Not started | Planned |
| `ai-prep-brief` | `prd-v1.md` | `REQ-AI-PREP-001`, `REQ-AI-RUN-001` | `api-contract-v1.md`, `openapi.yaml` | `ai_prompt_runs`, `ai_preparation_briefs` | `ai-contract-v1.md` | `QA-AI-PREP-001`, `QA-AI-RUN-001` | Not started | Not started | Planned |
| `ai-feedback-report` | `prd-v1.md` | `REQ-AI-FB-001`, `REQ-AI-FB-002`, `REQ-AI-RUN-001` | `api-contract-v1.md`, `openapi.yaml` | `ai_prompt_runs`, `ai_feedback_reports`, `study_session_action_items` | `ai-contract-v1.md`, `auth-permissions-v1.md` | `QA-AI-FB-001`, `QA-AI-FB-002`, `QA-AI-PRIV-001` | Not started | Not started | Planned |
| `discord-notifications` | `prd-v1.md` | `REQ-DIS-001` to `REQ-DIS-003` | `api-contract-v1.md`, `openapi.yaml` | `discord_notification_channels`, `discord_notification_logs`, `user_discord_accounts` | `discord-contract-v1.md` | `QA-DIS-001` to `QA-DIS-003` | Not started | Not started | Planned |

## Coverage Rules
- A feature is implementable only when PRD, requirements, API, DB, integration, and QA cells are populated.
- All v1 feature rows are populated and locked.
- Implementation evidence must link to code paths or PRs after code exists.
- Test evidence must link to test paths and verification results after code exists.
- Harness/infrastructure tasks use `n/a-harness`, not these product feature IDs.

## Change Rules
- Adding, removing, renaming, or changing a `feature_id` requires Change Request and ADR.
- Feature behavior changes must update all affected cells.
- Obsidian mirror must be refreshed after repo source changes.
