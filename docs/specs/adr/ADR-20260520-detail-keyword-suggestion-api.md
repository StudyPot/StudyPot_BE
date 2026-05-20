# ADR-20260520 Detail Keyword Suggestion API

## Status
- Approved

## Context
- `REQ-AI-001` says AI can suggest detail keywords and that candidates are not persisted unless selected by the user.
- The existing backend has `DetailKeywordSuggestionService`, but it is not exposed through a controller or OpenAPI path.
- `POST /api/v1/groups` requires final `detailKeywords`, so a Swagger user cannot test the intended group-creation helper flow without manually inventing keywords.
- The prior internal service shape returned suggestion objects with optional reasons and a rationale, but the requested product/API shape is a simple list of keyword strings.

## Decision
- Expose `POST /api/v1/groups/detail-keyword-suggestions` as an authenticated endpoint before group creation.
- The request accepts `topic`, optional `hintKeywords`, and optional `maxCandidates`.
- The response returns only `keywords`, a non-empty array of selectable keyword strings.
- The LLM structured-output schema for `DETAIL_KEYWORD_SUGGEST` uses a stable top-level `keywords` field and rejects blank or empty generated candidates.
- Suggested keywords remain transient. The backend records `llm_usage`, but does not persist candidates to `study_group` or any candidate table.
- Final selected/direct keywords continue to be persisted only through `POST /api/v1/groups`.

## Consequences
- Positive: Swagger and frontend clients can execute the actual group-creation helper flow using only a broad topic such as `Spring Boot`.
- Positive: The response shape is simple and stable for UI selection controls.
- Positive: No DB migration is needed because suggestions remain transient.
- Negative: The API no longer returns per-keyword explanation text; if the UI later needs explanations, another change request is required.
- Migration or compatibility notes: Existing group creation remains unchanged. Existing persisted data is unaffected.

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

## Linked Change Request
- [CR-20260520-detail-keyword-suggestion-api](../change-requests/CR-20260520-detail-keyword-suggestion-api.md)
