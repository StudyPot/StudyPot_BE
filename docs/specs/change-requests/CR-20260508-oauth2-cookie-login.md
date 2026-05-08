# CR-20260508-oauth2-cookie-login

## Status
- Approved

## Request
- Add a backend-owned Google OAuth2 redirect entrypoint at `GET /api/oauth2/authorization/google`.
- Add a backend OAuth2 callback at `GET /api/login/oauth2/code/google` that completes login, issues application tokens as HttpOnly cookies, and redirects to the configured frontend success or failure URI.
- Keep the existing JSON authorization-code exchange endpoint for compatibility.

## Reason
- The locked v1 contract currently requires the frontend to obtain a Google authorization code and manage returned token values.
- The frontend should not directly manage application access or refresh token values when a browser cookie session can carry them more safely.
- Local and production browser login need a simple backend entrypoint that can be opened directly.

## Affected Feature IDs
- `identity-core`

## Affected Documents
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/operations/local-development.md`

## Impact
- Product: Users can start Google login by opening the backend authorization endpoint and return to the frontend after success.
- API: Adds non-v1 browser redirect endpoints and allows auth refresh/logout to read refresh tokens from HttpOnly cookies while keeping request-body compatibility.
- DB: No table or column changes.
- AI: None.
- Notification: None.
- Permissions: OAuth redirect/callback are anonymous endpoints; protected APIs can authenticate from the access-token cookie or bearer header.
- QA: Adds redirect, state mismatch, credentialed CORS, cookie auth, and refresh-cookie coverage.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner request in Codex session
- Date: 2026-05-08
~ Linked ADR: [ADR-20260508-oauth2-cookie-login](../adr/ADR-20260508-oauth2-cookie-login.md)
