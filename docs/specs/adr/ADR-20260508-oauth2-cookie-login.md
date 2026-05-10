# ADR-20260508 Backend OAuth2 Redirect Login With HttpOnly Cookies

## Status
- Approved

## Context
- ADR-20260506 chose a frontend-owned Google authorization-code acquisition flow for MVP.
- The frontend should not directly store or manage application token values for browser login.
- The backend already owns Google token exchange, user upsert, JWT issuing, refresh-token hashing, rotation, and revocation.
- Adding backend-owned OAuth redirect/callback routes changes the locked v1 API/security behavior and requires a Change Request and ADR.

## Decision
- Add `GET /api/oauth2/authorization/google` as the browser login entrypoint.
- Generate OAuth `state` and PKCE verifier on the backend. The PKCE verifier is stored server-side, keyed by `oauth_state`, and is never sent to the browser; a short-lived HttpOnly `oauth_state` cookie may be used only for request correlation.
- Add `GET /api/login/oauth2/code/google` as the Google callback endpoint.
- On callback success, reuse `AuthSessionService` to issue the application access/refresh token pair, set them as HttpOnly cookies, clear temporary OAuth state/browser cookies and server-side PKCE state, and redirect to `studypot.auth.oauth2.frontend-success-uri`.
- On callback failure or state mismatch, clear temporary OAuth state/browser cookies and server-side PKCE state, do not issue token cookies, and redirect to `studypot.auth.oauth2.frontend-failure-uri`.
- Keep `POST /api/v1/auth/oauth/google` returning the existing JSON token response for compatibility, but also set the same HttpOnly token cookies.
- Keep bearer header authentication and add access-token cookie authentication as an alternative for protected APIs.

## Consequences
- Positive: Browser login no longer requires frontend JavaScript to receive or persist token values.
- Positive: Existing refresh-token hashing/rotation and user linkage logic stays centralized in `AuthSessionService`.
- Positive: Frontend integration becomes a simple redirect to the backend login entrypoint followed by a frontend success/failure redirect.
- Negative: Browser clients must be configured with credentialed CORS and matching SameSite/Secure cookie settings.
- Migration or compatibility notes: No DB migration is required. Existing JSON auth clients continue to work.

## Affected Feature IDs
- `identity-core`

## Affected Documents
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/operations/local-development.md`

## Linked Change Request
- [CR-20260508-oauth2-cookie-login](../change-requests/CR-20260508-oauth2-cookie-login.md)
