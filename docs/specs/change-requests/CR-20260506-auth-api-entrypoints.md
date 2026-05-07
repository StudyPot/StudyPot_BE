# CR-20260506-auth-api-entrypoints

## Status
- Approved

## Request
- Add explicit MVP authentication API entrypoints for Google OAuth login, access-token refresh, current-session logout, and all-session logout.
- Use a JSON authorization-code exchange endpoint instead of backend-owned OAuth redirect/callback routes for the MVP REST contract.

## Reason
- Locked v1 already includes `identity-core`, `oauth_account`, `refresh_token`, and bearer-token protected APIs.
- The locked API contract only exposes `GET /users/me`; it does not define how a client obtains, refreshes, or revokes application tokens.
- Implementation cannot complete the login/session lifecycle safely without a machine-readable OpenAPI contract for those auth flows.

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

## Impact
- Product: No new product surface beyond the existing login/session lifecycle; the missing auth entrypoints are made explicit.
- API: Adds `POST /api/v1/auth/oauth/google`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, and `POST /api/v1/auth/logout-all`.
- DB: No table or column changes; existing `users`, `oauth_account`, and `refresh_token` support the change.
- AI: None.
- Notification: None.
- Permissions: OAuth login and refresh are explicitly anonymous/client endpoints; logout endpoints are authenticated.
- QA: Adds auth login, token rotation/reuse prevention, current-session logout, and logout-all acceptance.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner request in Codex session
- Date: 2026-05-06
- Linked ADR: [ADR-20260506-auth-api-entrypoints](../adr/ADR-20260506-auth-api-entrypoints.md)
