# CR-20260520-detail-keyword-suggestion-api

## Status
- Approved

## Request
- Add a public authenticated API for the group-creation helper flow that suggests detail keywords before the study group is created.
- Use `POST /api/v1/groups/detail-keyword-suggestions`.
- Accept a required broad `topic`, optional `hintKeywords`, and optional `maxCandidates`.
- Return only a stable `keywords` array of selectable keyword strings.
- Keep candidates transient: do not persist suggested keywords unless the user later submits selected/direct keywords through `POST /api/v1/groups`.

## Reason
- The locked v1 plan requires AI detail keyword suggestions, but the current backend only has an internal `DetailKeywordSuggestionService`.
- Swagger-based manual flow cannot exercise group creation naturally because `POST /api/v1/groups` already requires final `detailKeywords`.
- The product owner chose a simple keyword-only response for the creation helper instead of reason/rationale objects.

## Affected Feature IDs
- `ai-team-leader`
- `study-group-core`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/06-ai-team-leader.md`

## Impact
- Product: Hosts can request AI keyword candidates during group creation, then choose or edit final keywords before creating the group.
- API: Adds `POST /api/v1/groups/detail-keyword-suggestions` and new request/response schemas.
- DB: No table, column, enum, index, or migration change. Suggested candidates are not persisted.
- AI: Changes the detail keyword structured output shape to `{"keywords":["..."]}` for a stable response parameter.
- Notification: No notification behavior change.
- Permissions: Requires authenticated user only; no group permission applies because the group does not exist yet.
- QA: Add controller and service tests for success, authentication, validation, provider unavailable, malformed AI output, and usage logging.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session on 2026-05-20.
- Date: 2026-05-20
- Linked ADR: [ADR-20260520-detail-keyword-suggestion-api](../adr/ADR-20260520-detail-keyword-suggestion-api.md)
