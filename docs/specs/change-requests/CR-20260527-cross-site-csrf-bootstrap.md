# CR-20260527-cross-site-csrf-bootstrap

## Status
- Approved

## Request
- Add `GET /api/v1/auth/csrf` as a public safe endpoint that issues a readable CSRF token value and matching `XSRF-TOKEN` cookie for browser cookie-backed unsafe requests.
- Configure the CSRF cookie with the same browser-domain policy as auth cookies, including production `Secure` and cross-site-compatible SameSite settings.
- Expose `X-XSRF-TOKEN` through CORS so cross-site frontends may read the response header when they choose not to read the JSON body.

## Reason
- Netlify frontend and rumiclean API are different sites.
- Auth token cookies can be sent cross-site with `SameSite=None; Secure`, but the frontend cannot read the backend-domain `XSRF-TOKEN` cookie with `document.cookie`.
- Without a readable bootstrap token, OAuth success redirects to the frontend but the first cookie-backed `POST /api/v1/auth/refresh` can be rejected by CSRF protection before refresh-token validation.

## Affected Feature IDs
- `identity-core`

## Affected Documents
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/operations/deployment.md`
- `docs/operations/local-development.md`

## Impact
- Product: Cross-site browser login can restore the session after OAuth success without exposing access or refresh token values.
- API: Adds a public safe `GET /api/v1/auth/csrf` endpoint returning CSRF metadata only.
- DB: No schema or data migration.
- AI: None.
- Notification: None.
- Permissions: The endpoint is anonymous, but it does not authenticate a user or return account data.
- QA: Adds cross-site CSRF bootstrap and refresh regression coverage.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner request in Codex session for SPT-116
- Date: 2026-05-27
- Linked ADR: [ADR-20260527-cross-site-csrf-bootstrap](../adr/ADR-20260527-cross-site-csrf-bootstrap.md)
