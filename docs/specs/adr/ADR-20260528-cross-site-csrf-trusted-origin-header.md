# ADR-20260528 Cross-Site CSRF Trusted-Origin Header Validation

## Status
- Approved

## Context
- Backend-owned OAuth login issues HttpOnly access and refresh cookies and redirects to the Netlify frontend.
- The frontend now calls `GET /api/v1/auth/csrf` and receives a CSRF token value for the `X-XSRF-TOKEN` header.
- The previous backend filter still required a matching backend-domain `XSRF-TOKEN` cookie for every unsafe cookie-backed request.
- In cross-site browser traffic, that double-submit cookie may not be present on the later unsafe request even though the frontend has a token header from the safe bootstrap endpoint.
- Browser attacks from untrusted origins cannot send credentialed custom-header requests unless the backend CORS policy allows that origin.

## Decision
- Keep `GET /api/v1/auth/csrf` as the safe token bootstrap endpoint.
- Keep accepting the existing double-submit cookie/header match when both are present.
- For unsafe cookie-backed browser requests without a matching XSRF cookie, accept a non-empty `X-XSRF-TOKEN` or `X-CSRF-TOKEN` header only when the request `Origin` is accepted by the configured `CorsConfigurationSource` and credentialed CORS is enabled.
- Keep rejecting headerless unsafe cookie-backed requests.
- Keep rejecting header-only requests from origins not allowed by the configured credentialed CORS policy.
- Keep Bearer-token API requests outside browser CSRF requirements.

## Consequences
- Positive: Netlify can complete refresh, logout, group creation, and detail-keyword suggestion calls after OAuth without depending on JavaScript access to backend-domain cookies.
- Positive: The backend has one trust source for frontend origins because CSRF trusted-origin checks reuse the same CORS configuration as browser API access.
- Positive: Same-site clients and Swagger-style clients that send a readable `XSRF-TOKEN` cookie with the matching header continue to work.
- Negative: CSRF enforcement is no longer the default Spring cookie repository equality check for unsafe requests; the application-owned browser CSRF filter is the enforcement point and must remain covered by tests.
- Migration or compatibility notes: No DB migration or frontend API shape change is required. Frontends should continue calling `/api/v1/auth/csrf` before unsafe cookie-backed requests.

## Affected Feature IDs
- `identity-core`
- `study-group-core`

## Affected Documents
- `docs/specs/api-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/change-control-v1.md`
- `docs/operations/deployment.md`
- `docs/operations/local-development.md`

## Linked Change Request
- [CR-20260528-cross-site-csrf-trusted-origin-header](../change-requests/CR-20260528-cross-site-csrf-trusted-origin-header.md)
