# ADR-20260527 Cross-Site CSRF Bootstrap For Cookie Sessions

## Status
- Approved

## Context
- Backend-owned Google OAuth login issues HttpOnly access and refresh cookies and redirects to the configured frontend success URI.
- Production currently uses Netlify for the frontend and `studypot.rumiclean.com` for the API, so the two origins are different sites.
- The frontend already sends API requests with credentials included, and SPT-114/SPT-115 configured token cookies as `SameSite=None; Secure`.
- Cookie-backed unsafe requests still require CSRF protection. A frontend on a different site cannot read an API-domain `XSRF-TOKEN` cookie through `document.cookie`, so it needs another safe way to learn the header value.

## Decision
- Add `GET /api/v1/auth/csrf` as an anonymous safe endpoint.
- The endpoint returns `cookieName`, `headerName`, and `token` in JSON and also sets `X-XSRF-TOKEN` response header to the same token value.
- The Spring CSRF cookie repository uses the configured auth cookie path, domain, Secure flag, and SameSite policy for `XSRF-TOKEN`, while keeping the CSRF cookie readable.
- Existing access and refresh token cookies remain HttpOnly and are never returned by this endpoint.
- Existing cookie-backed unsafe request protection remains in place; clients must echo the bootstrap token in `X-XSRF-TOKEN`.

## Consequences
- Positive: Netlify can complete OAuth success session restore without needing access to backend-domain cookies through `document.cookie`.
- Positive: CSRF protection remains active for refresh, logout, and other cookie-backed unsafe requests.
- Positive: The contract also supports future same-site frontend deployments because same-site clients may still read the cookie directly.
- Negative: Browser clients must call one extra safe endpoint before the first unsafe cookie-backed request when they are deployed on a different site.
- Migration or compatibility notes: Existing bearer-token clients and same-site cookie clients continue to work.

## Affected Feature IDs
- `identity-core`

## Affected Documents
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/operations/deployment.md`
- `docs/operations/local-development.md`

## Linked Change Request
- [CR-20260527-cross-site-csrf-bootstrap](../change-requests/CR-20260527-cross-site-csrf-bootstrap.md)
