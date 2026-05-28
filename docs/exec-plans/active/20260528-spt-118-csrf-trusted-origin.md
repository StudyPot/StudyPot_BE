# EXEC_PLAN: [fix] Cross-site CSRF trusted-origin 헤더 검증 적용

- Task slug: `spt-118-csrf-trusted-origin`
- Base branch: `develop`
- Feature branch: `codex/spt-118-csrf-trusted-origin`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-118-csrf-trusted-origin`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-118-csrf-trusted-origin`
- Jira issue: `SPT-118`
- Jira URL: https://studypot.atlassian.net/browse/SPT-118
- Jira summary: [fix] Cross-site CSRF trusted-origin 헤더 검증 적용
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260527-cross-site-csrf-bootstrap.md
- [x] docs/specs/adr/ADR-20260527-cross-site-csrf-bootstrap.md
- [x] docs/operations/deployment.md
- [x] docs/operations/local-development.md

## Related Feature IDs
- [x] identity-core
- [x] n/a-harness

## Doc Notes
- Production frontend now contains the SPT-117 CSRF bootstrap bundle and calls `GET /api/v1/auth/csrf`.
- Runtime evidence shows `/api/v1/auth/csrf` returns 200 and the frontend receives a token, but unsafe cookie-backed requests can still stop at 403 before controller timing logs.
- Current backend CSRF filter requires `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header equality. A cross-site frontend can receive the bootstrap token but may not attach the backend-domain XSRF cookie on later unsafe requests, so the header is present while the double-submit cookie check fails.
- The locked v1 specs require Change Request + ADR updates for this browser cookie/CSRF contract adjustment.

## Goal
Allow credentialed browser requests from configured trusted CORS origins to use the bootstrapped CSRF token header for unsafe cookie-backed requests even when the backend-domain `XSRF-TOKEN` cookie is not readable or not attached by the browser, while continuing to reject headerless unsafe requests and untrusted origins.

## Approach
- Add failing regression coverage for trusted-origin header-only CSRF requests and untrusted header-only rejection.
- Keep the existing double-submit cookie/header equality path for same-site or cookie-available browser requests.
- Reuse the configured `CorsConfigurationSource` as the trust source for origin checks instead of introducing a second origin list.
- Keep Bearer-token API requests outside CSRF requirements.
- Update the locked-contract docs through a new CR/ADR that scopes the relaxation to configured credentialed browser origins.

## Step Plan
1. Add RED tests around `BrowserCsrfProtectionFilter` and at least one MockMvc endpoint path reproducing trusted-origin header-only behavior.
2. Adjust security configuration so the custom browser CSRF filter is the enforcement point for unsafe cookie-backed requests and can evaluate trusted-origin header-only requests.
3. Update CR/ADR, auth/API specs, and deployment/local docs with the refined CSRF contract.
4. Run focused tests, contract scripts where relevant, and `./gradlew check build --no-daemon`.
5. Commit, create PR, run CodeRabbit review, pass review gate, finish/merge, then verify deployed behavior with live backend smoke checks.

## Done Criteria
- Trusted configured origin + non-empty CSRF header reaches controller/service validation instead of failing with CSRF 403 when no `XSRF-TOKEN` cookie is present.
- Untrusted origin + CSRF header without matching cookie remains forbidden.
- Headerless unsafe cookie-backed requests remain forbidden.
- Existing cookie/header double-submit requests still pass.
- Documentation records the approved v1 contract change.
- Standard verification passes before PR creation.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest.crossSiteRefreshCanUseBootstrappedCsrfHeaderWhenXsrfCookieIsUnavailable' --no-daemon` failed with expected `401` vs actual `403`.
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.global.security.BrowserCsrfProtectionFilterTest' --no-daemon --rerun-tasks`
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.auth.controller.AuthControllerTest' --no-daemon`
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest' --no-daemon`
- GREEN: `bash scripts/tests/test_auth_api_contracts.sh`
- GREEN: `bash scripts/tests/test_deployment_contracts.sh`
- GREEN: `bash scripts/tests/test_docs_source_of_truth.sh`
- GREEN: `./gradlew check build --no-daemon`
