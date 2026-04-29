# Change Control v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Locked date: `2026-04-29`
- Lock unit: full v1 planning package.

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
- `docs/specs/discord-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`

## Change Rule
After v1 lock, any change to product scope, feature behavior, API shape, DB shape, enum, AI schema, Discord behavior, permission rule, or QA acceptance requires:

1. Change Request using `docs/specs/change-request-template.md`.
2. ADR using `docs/specs/adr-template.md`.
3. Update to affected source documents.
4. Update to Obsidian mirror summary.
5. Harness validation.

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
- Discord notification type or delivery behavior change.
- Permission matrix change.
- MVP scope or non-scope change.

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
- Obsidian `Current State` is updated.
