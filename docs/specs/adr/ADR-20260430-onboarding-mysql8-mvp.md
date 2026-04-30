# ADR-20260430-onboarding-mysql8-mvp

## Status
- Accepted

## Context
- The first locked v1 package described a meeting/session-centered MVP using PostgreSQL and JSONB.
- The latest product decision moves meetings out of MVP and makes onboarding, AI curriculum, weekly todos, and retrospective feedback the primary user value.
- The latest ERD document is `ERD_설계문서_v0.8_MySQL8.docx`, with MySQL8 tables and onboarding entities.

## Decision
- Adopt Requirements v0.3 and ERD v0.8 as the v1 implementation source of truth.
- Use MySQL8 as the locked DB baseline.
- Store UUIDv7 identifiers as `BINARY(16)`.
- Store flexible structured fields as MySQL `JSON`.
- Replace session/note/action item MVP entities with onboarding, curriculum, weekly todo, retrospective, AI conversation, notification, and LLM usage entities.
- Treat meeting-centered features as post-MVP unless a new Change Request and ADR reintroduces them.

## Consequences
- Positive: Jira, API, DB, AI, Discord, and QA documents now match the product the user wants to build.
- Positive: host start and onboarding response timing are explicit enough for implementation tasks.
- Negative: previous session-centered docs and tests must be rewritten before implementation continues.
- Negative: MySQL JSON behavior differs from PostgreSQL JSONB, so query/index strategy must be explicit.
- Migration or compatibility notes: no production migration is required; first implementation migrations should be generated from `docs/specs/db-schema-v1.sql`.

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

## Linked Change Request
- [CR-20260430-onboarding-mysql8-mvp](../change-requests/CR-20260430-onboarding-mysql8-mvp.md)
