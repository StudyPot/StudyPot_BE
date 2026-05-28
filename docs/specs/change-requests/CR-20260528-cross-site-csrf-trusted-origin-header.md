# CR-20260528-cross-site-csrf-trusted-origin-header

## Status
- Approved

## Request
- Refine browser CSRF validation so configured credentialed CORS origins may send the bootstrapped CSRF value in the `X-XSRF-TOKEN` request header even when the backend-domain `XSRF-TOKEN` cookie is not available on the unsafe request.
- Keep the existing double-submit path where an `XSRF-TOKEN` cookie and CSRF header match.
- Continue rejecting unsafe cookie-backed requests that have no CSRF header or come from origins not allowed by the configured credentialed CORS policy.

## Reason
- Netlify frontend and rumiclean API are deployed on different sites.
- SPT-116 added `GET /api/v1/auth/csrf`, and SPT-117 calls it from the frontend. Runtime evidence shows the token reaches the frontend.
- Some browser flows can still send the CSRF header without attaching the API-domain `XSRF-TOKEN` cookie on later unsafe requests. The previous strict cookie/header equality check rejects those requests with 403 before refresh, logout, or group controllers can run.
- The custom CSRF header is still protected by CORS preflight: untrusted origins cannot read the bootstrap response or send credentialed custom-header requests through the configured browser CORS policy.

## Affected Feature IDs
- `identity-core`
- `study-group-core`

## Affected Documents
- `docs/specs/api-contract-v1.md`
- `docs/specs/auth-permissions-v1.md`
- `docs/specs/change-control-v1.md`
- `docs/operations/deployment.md`
- `docs/operations/local-development.md`

## Impact
- Product: Cross-site browser login, logout, and authenticated group creation helpers can use cookie-backed auth from the Netlify frontend.
- API: No endpoint path, request body, or response body changes.
- DB: No schema or data migration.
- AI: None.
- Notification: None.
- Permissions: Unsafe cookie-backed browser requests still require CSRF evidence; the trust source is the configured credentialed CORS origin plus a non-empty custom CSRF header when the XSRF cookie is unavailable.
- QA: Adds refresh, logout, detail-keyword suggestion, and filter-level regression coverage for trusted-origin header-only CSRF requests.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner request in Codex session for SPT-118 after SPT-116/SPT-117 production verification showed token delivery but unsafe requests still returned 403.
- Date: 2026-05-28
- Linked ADR: [ADR-20260528-cross-site-csrf-trusted-origin-header](../adr/ADR-20260528-cross-site-csrf-trusted-origin-header.md)
