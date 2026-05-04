# CR-20260430-onboarding-mysql8-mvp

## Status
- Approved

## Request
- Replace the locked meeting/session-centered v1 implementation baseline with the onboarding MVP baseline from Requirements v0.3 and ERD v0.8.
- Change the DB baseline from PostgreSQL/JSONB to MySQL8/JSON with UUIDv7 stored as `BINARY(16)`.

## Reason
- Product planning changed before implementation: synchronous meeting automation is deferred.
- The core MVP now starts from study group creation, invite link sharing, group-specific member onboarding, host start, AI curriculum generation, weekly todo execution, and AI team leader retrospective feedback.
- Keeping the old meeting-centered locked docs would cause Jira tasks, DB schema, OpenAPI, and acceptance criteria to implement the wrong product.

## Affected Feature IDs
- `identity-core`
- `study-group-core`
- `group-onboarding`
- `curriculum-core`
- `weekly-todo`
- `retrospective-feedback`
- `ai-team-leader`
- `discord-notifications`
- `n/a-harness`

## Affected Documents
- `ARCHITECTURE.md`
- `docs/index.md`
- `docs/specs/prd-v1.md`
- `docs/specs/user-journeys-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/domain-erd.md`
- `docs/specs/db-contract-v1.md`
- `docs/specs/db-schema-v1.sql`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/discord-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`

## Impact
- Product: MVP flow becomes onboarding and weekly todo based. Heavy meeting automation is deferred.
- API: group, invite, onboarding, host start, curriculum, weekly task, retrospective, AI conversation, notification, and LLM usage resources become primary.
- DB: ERD v0.8 entity set replaces the previous session/note/action-item schema.
- AI: AI team leader generates detail keyword suggestions, curriculum, feedback, and next-week adjustments from onboarding and weekly progress context.
- Discord: Discord remains notification delivery and integration storage, not the core study record.
- Permissions: owner/member access is tied to group status and onboarding status.
- QA: acceptance tests focus on onboarding completeness, host start, weekly todo completion/incomplete reasons, feedback, and schema coverage.

## Compatibility
- Backward compatible: no. No production data exists, so destructive schema replacement is acceptable before implementation.
- Migration required: no runtime data migration; implementation must create new migrations from this locked schema.

## Decision
- Approved by: user
- Date: 2026-04-30
- Linked ADR: [ADR-20260430-onboarding-mysql8-mvp](../adr/ADR-20260430-onboarding-mysql8-mvp.md)
