# Change Control v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Original locked date: `2026-04-29`
- Current lock refresh date: `2026-05-04`
- Lock unit: full v1 planning package.

## Current Approved Change
- Change Request: [CR-20260520-onboarding-simplification-auto-merge](./change-requests/CR-20260520-onboarding-simplification-auto-merge.md)
- ADR: [ADR-20260520-onboarding-simplification-auto-merge](./adr/ADR-20260520-onboarding-simplification-auto-merge.md)
- Result: The MVP onboarding API accepts one submitted payload with `skillLevel`, `additionalNote`, and `availabilitySlots`, hides internal score maps, and the PR finish harness auto-merges review-gate-passed PRs before cleanup.

## Previous Approved Change
- Change Request: [CR-20260520-detail-keyword-suggestion-api](./change-requests/CR-20260520-detail-keyword-suggestion-api.md)
- ADR: [ADR-20260520-detail-keyword-suggestion-api](./adr/ADR-20260520-detail-keyword-suggestion-api.md)
- Result: The MVP API exposes authenticated pre-creation detail keyword suggestions at `POST /api/v1/groups/detail-keyword-suggestions`, returning transient `keywords` only while preserving final keyword persistence through group creation.

## Earlier Approved Change
- Change Request: [CR-20260519-redis-rabbitmq-realtime-infra](./change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md)
- ADR: [ADR-20260519-redis-rabbitmq-realtime-infra](./adr/ADR-20260519-redis-rabbitmq-realtime-infra.md)
- Result: Redis is the short-lived rate-limit/TTL-lock protection layer, RabbitMQ is the async dispatch layer, MySQL remains durable source of truth, and production Redis/RabbitMQ activation is deferred to a later deployment task.

## Earlier Approved Change
- Change Request: [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md)
- ADR: [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md)
- Result: The MVP AI boundary uses a deterministic DB-first context builder before retrospective/chat LLM calls, keeps retrospective/chat/API/DB shapes unchanged, and defers vector/graph retrieval or service-split choices to later tasks.

## Earlier Approved Change
- Change Request: [CR-20260512-week-progress-read-endpoint](./change-requests/CR-20260512-week-progress-read-endpoint.md)
- ADR: [ADR-20260512-week-progress-read-endpoint](./adr/ADR-20260512-week-progress-read-endpoint.md)
- Result: The MVP API includes a read-only member week progress endpoint at `GET /api/v1/weeks/{weekId}/progress/me`.

## Earlier Approved Change
- Change Request: [CR-20260508-oauth2-cookie-login](./change-requests/CR-20260508-oauth2-cookie-login.md)
- ADR: [ADR-20260508-oauth2-cookie-login](./adr/ADR-20260508-oauth2-cookie-login.md)
- Result: Browser OAuth2 login can be completed through backend redirect/callback routes with HttpOnly access/refresh token cookies.

## Earlier Approved Change
- Change Request: [CR-20260506-auth-api-entrypoints](./change-requests/CR-20260506-auth-api-entrypoints.md)
- ADR: [ADR-20260506-auth-api-entrypoints](./adr/ADR-20260506-auth-api-entrypoints.md)
- Result: The MVP API includes explicit Google OAuth login, refresh, current-session logout, and all-session logout endpoints for `identity-core`.

## Earlier Approved Change
- Change Request: [CR-20260504-no-discord-inapp-notification](./change-requests/CR-20260504-no-discord-inapp-notification.md)
- ADR: [ADR-20260504-no-discord-inapp-notification](./adr/ADR-20260504-no-discord-inapp-notification.md)
- Result: Discord integration is removed from MVP, notification is in-app first, and AI team leader owns weekly next-week adjustment.

## Initial Approved Change
- Change Request: [CR-20260430-onboarding-mysql8-mvp](./change-requests/CR-20260430-onboarding-mysql8-mvp.md)
- ADR: [ADR-20260430-onboarding-mysql8-mvp](./adr/ADR-20260430-onboarding-mysql8-mvp.md)
- Result: v1 implementation source of truth is Requirements v0.3, ERD v0.8, MySQL8, and onboarding MVP.

## Locked Documents
- `docs/specs/prd-v1.md`
- `docs/specs/user-journeys-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/domain-erd.md`
- `docs/specs/db-contract-v1.md`
- `docs/specs/db-schema-v1.sql`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/notification-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`

## Change Rule
After v1 lock, any change to product scope, feature behavior, API shape, DB shape, enum, AI schema, notification behavior, permission rule, or QA acceptance requires:

1. Change Request using `docs/specs/change-request-template.md`.
2. ADR using `docs/specs/adr-template.md`.
3. Update to affected source documents.
4. Update to Confluence/Jira document references.
5. Update to Obsidian mirror summary if the vault is in use.
6. Harness validation.

## Allowed Without ADR
- Typo fixes that do not change meaning.
- Formatting changes that do not change meaning.
- Link/path corrections.
- Clarifying examples that do not add or remove behavior.

## Requires ADR
- New endpoint, removed endpoint, path change, request change, response change.
- New table, removed table, column change, enum change, constraint change.
- New feature_id or removal of a feature_id.
- AI JSON schema change.
- Notification type or delivery behavior change.
- Permission matrix change.
- MVP scope or non-scope change.
- DBMS baseline change.

## Decision Owners
- Product owner: user.
- Implementation owner: backend implementer for the relevant task.
- Documentation owner: current Codex session or assigned engineer.

## Review Checklist
- The change states why v1 cannot remain as-is.
- The change lists affected feature IDs.
- The change lists affected docs.
- The change has migration or compatibility notes when API/DB changes.
- The feature coverage matrix is updated.
- Jira documentation issues link to the current source of truth.
