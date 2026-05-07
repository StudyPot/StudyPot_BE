# ADR-20260506 Add MVP Auth API Entrypoints

## Status
- Approved

## Context
- The v1 API contract requires bearer access tokens for protected `/api/v1` endpoints.
- `identity-core` already includes Google OAuth account linkage and refresh-token lifecycle persistence.
- The locked OpenAPI file omitted the API entrypoints that issue, refresh, and revoke application tokens.
- Adding endpoints changes the locked API surface, so it requires a Change Request and ADR.

## Decision
- Add `POST /api/v1/auth/oauth/google` as the MVP login endpoint.
- The client sends a Google authorization code, redirect URI, and optional PKCE verifier; the backend exchanges it with Google, upserts the application user/OAuth account, stores only hashed application refresh tokens, and returns an application access/refresh token pair.
- Add `POST /api/v1/auth/refresh` to rotate refresh tokens and issue a new access token.
- Add `POST /api/v1/auth/logout` to revoke the submitted current refresh token for the authenticated user.
- Add `POST /api/v1/auth/logout-all` to revoke all refresh tokens for the authenticated user.
- Keep backend-owned OAuth redirect/callback routes out of the MVP REST contract; they can be added later if the frontend architecture needs them.

## Consequences
- Positive: The API contract now fully supports the existing `identity-core` login/session lifecycle.
- Positive: OpenAPI clients can generate typed request/response models for auth flows.
- Positive: Refresh token rotation and revocation are testable as first-class API behavior.
- Negative: The frontend must own the Google authorization-code acquisition flow for MVP.
- Migration or compatibility notes: No DB migration is required because existing identity tables already support the lifecycle.

## Affected Feature IDs
- `identity-core`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/05-api-spec.md`

## Linked Change Request
- [CR-20260506-auth-api-entrypoints](../change-requests/CR-20260506-auth-api-entrypoints.md)
