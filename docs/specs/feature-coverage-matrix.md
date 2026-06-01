# AI Study Leader Feature Coverage Matrix

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.

## Purpose
This matrix maps every MVP `feature_id` to source documents, API contracts, DB contracts, integration contracts, and QA evidence requirements.

| Feature ID | PRD | Requirements | API | DB | Integration | QA | Implementation Evidence | Test Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `identity-core` | `prd-v1.md` | `REQ-ID-001` to `REQ-ID-002` | `api-contract-v1.md`, `openapi.yaml` | `users`, `oauth_account`, `refresh_token` | `auth-permissions-v1.md` | `QA-ID-001` to `QA-ID-007` | Not started | Not started | Planned |
| `study-group-core` | `prd-v1.md` | `REQ-GRP-001` to `REQ-GRP-002`, `REQ-INV-001` | `api-contract-v1.md`, `openapi.yaml` | `study_group`, `group_member` | `auth-permissions-v1.md` | `QA-GRP-001` to `QA-GRP-004` | Not started | Not started | Planned |
| `group-onboarding` | `prd-v1.md` | `REQ-ONB-001` to `REQ-ONB-003` | `api-contract-v1.md`, `openapi.yaml` | `group_onboarding_response`, `member_availability_slot` | `auth-permissions-v1.md` | `QA-ONB-001` to `QA-ONB-004` | Not started | Not started | Planned |
| `curriculum-core` | `prd-v1.md` | `REQ-CUR-001` to `REQ-CUR-002` | `api-contract-v1.md`, `openapi.yaml` | `curriculum`, `curriculum_week`, `weekly_task` | `ai-contract-v1.md` | `QA-CUR-001` to `QA-CUR-003` | Not started | Not started | Planned |
| `weekly-todo` | `prd-v1.md` | `REQ-TODO-001` to `REQ-TODO-003` | `api-contract-v1.md`, `openapi.yaml` | `member_week_progress`, `task_completion` | `auth-permissions-v1.md` | `QA-TODO-001` to `QA-TODO-005` | Not started | Not started | Planned |
| `retrospective-feedback` | `prd-v1.md` | `REQ-RETRO-001` to `REQ-RETRO-002` | `api-contract-v1.md`, `openapi.yaml` | `retrospective`, `ai_conversation`, `ai_conversation_message` | `ai-contract-v1.md`, `auth-permissions-v1.md` | `QA-RETRO-001` to `QA-RETRO-004` | Not started | Not started | Planned |
| `ai-team-leader` | `prd-v1.md` | `REQ-AI-001` to `REQ-AI-003` | `api-contract-v1.md`, `openapi.yaml` | `llm_usage`, AI JSON columns | `ai-contract-v1.md` | `QA-AI-001` to `QA-AI-005` | Not started | Not started | Planned |
| `notification` | `prd-v1.md` | `REQ-NOTI-001` to `REQ-NOTI-003` | `api-contract-v1.md`, `openapi.yaml` | `notification` | `notification-contract-v1.md`, `auth-permissions-v1.md` | `QA-NOTI-001` to `QA-NOTI-005` | Not started | Not started | Planned |
| `n/a-harness` | `prd-v1.md` | global workflow | `n/a` | schema verification | `jira-board-sync.md`, `pr-review-gate.md` | `QA-GLOBAL-001` to `QA-GLOBAL-005` | `SPT-19`, `SPT-50` | Not started | Planned |

`weekly-todo` QA coverage through `QA-TODO-005` is approved by [CR-20260512-week-progress-read-endpoint](./change-requests/CR-20260512-week-progress-read-endpoint.md) and [ADR-20260512-week-progress-read-endpoint](./adr/ADR-20260512-week-progress-read-endpoint.md).

`retrospective-feedback` and `ai-team-leader` DB-first context boundaries are approved by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md) and [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md). The change does not add API paths, DB tables, enum values, or new feature IDs.

The authenticated pre-creation detail keyword suggestion API for `ai-team-leader` and `study-group-core` is approved by [CR-20260520-detail-keyword-suggestion-api](./change-requests/CR-20260520-detail-keyword-suggestion-api.md) and [ADR-20260520-detail-keyword-suggestion-api](./adr/ADR-20260520-detail-keyword-suggestion-api.md). It adds `POST /api/v1/groups/detail-keyword-suggestions`, keeps suggestions transient, and returns a keyword-only response.

The simplified one-step onboarding API and PR auto-merge harness behavior are approved by [CR-20260520-onboarding-simplification-auto-merge](./change-requests/CR-20260520-onboarding-simplification-auto-merge.md) and [ADR-20260520-onboarding-simplification-auto-merge](./adr/ADR-20260520-onboarding-simplification-auto-merge.md). The public onboarding API accepts `skillLevel`, `additionalNote`, and `availabilitySlots`; internal DB JSON compatibility is preserved without a schema migration.

The cross-site CSRF bootstrap endpoint for `identity-core` is approved by [CR-20260527-cross-site-csrf-bootstrap](./change-requests/CR-20260527-cross-site-csrf-bootstrap.md) and [ADR-20260527-cross-site-csrf-bootstrap](./adr/ADR-20260527-cross-site-csrf-bootstrap.md). It adds public safe `GET /api/v1/auth/csrf` so browser clients on a different site can send CSRF headers for cookie-backed unsafe requests without exposing access or refresh token values.

Redis/RabbitMQ runtime infrastructure boundaries for `identity-core`, `curriculum-core`, `retrospective-feedback`, `ai-team-leader`, `notification`, and `n/a-harness` are approved by [CR-20260519-redis-rabbitmq-realtime-infra](./change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md) and [ADR-20260519-redis-rabbitmq-realtime-infra](./adr/ADR-20260519-redis-rabbitmq-realtime-infra.md). The change does not add API paths, DB tables, enum values, notification types, permission actions, or new feature IDs.

Recipient-scoped notification SSE delivery for `notification` is approved by [CR-20260601-notification-sse-stream](./change-requests/CR-20260601-notification-sse-stream.md) and [ADR-20260601-notification-sse-stream](./adr/ADR-20260601-notification-sse-stream.md). It adds `GET /api/v1/users/me/notifications/stream`, keeps SSE as an in-app realtime transport rather than a new channel, and uses the existing list API for reconnect recovery.

## Coverage Rules
- A feature is implementable only when PRD, requirements, API, DB, integration, and QA cells are populated.
- All v1 feature rows are populated and locked.
- Implementation evidence must link to code paths or PRs after code exists.
- Test evidence must link to test paths and verification results after code exists.
- Harness/infrastructure tasks use `n/a-harness`, not product feature IDs.

## Change Rules
- Adding, removing, renaming, or changing a `feature_id` requires Change Request and ADR.
- Feature behavior changes must update all affected cells.
- Jira and Confluence references must be refreshed after repo source changes.
