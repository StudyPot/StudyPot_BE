# EXEC_PLAN: [common-api] 오류 응답/커서 페이지네이션 공통 구현

- Task slug: `spt-21-common-api`
- Base branch: `develop`
- Feature branch: `codex/spt-21-common-api`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-21-common-api`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-21-common-api`
- Jira issue: `SPT-21`
- Jira URL: https://studypot.atlassian.net/browse/SPT-21
- Jira summary: [common-api] 오류 응답/커서 페이지네이션 공통 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/change-control-v1.md
- [x] docs/architecture/backend-map.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] identity-core
- [x] study-group-core
- [x] group-onboarding
- [x] curriculum-core
- [x] weekly-todo
- [x] retrospective-feedback
- [x] ai-team-leader
- [x] notification

## Doc Notes
- API contract v1 is locked and requires RFC 9457-style ProblemDetail errors plus cursor pagination for list endpoints that can grow.
- This task implements shared API infrastructure only; no endpoint, request/response contract, enum, DB, AI, notification, or permission behavior changes are planned.
- Existing scaffold already contains `global.api` cursor page response records and `global.error` ProblemDetail validation handling.
- Spring MVC method parameter validation, such as query parameter validation, should also return the same validation ProblemDetail shape instead of falling through to framework defaults.
- PR flow requires Korean human-facing PR/review text, GitHub Actions review gate markers, role gate evidence, manual merge notification, and post-merge cleanup for Jira Done.

## Goal
Complete SPT-21 by formalizing the shared common API implementation for validation errors and cursor pagination, preserving the locked API contract while adding missing validation coverage for request parameter failures.

## Approach
- Keep the scope inside `global.api` and `global.error`.
- Use TDD for the missing method parameter validation behavior: add a MockMvc test that currently fails, then add the minimal exception handler mapping.
- Preserve existing cursor pagination shape and add no OpenAPI/spec changes because the locked documents already define the public contract at the required level.
- Run focused tests first, then the standard `./gradlew check build --no-daemon` verification before commit/PR.

## Step Plan
1. Inspect existing common API/error implementation and tests.
2. Add a failing validation test for invalid query parameter input returning a 422 ProblemDetail with field errors.
3. Implement Spring MVC method parameter validation handling in `ApiExceptionHandler`.
4. Re-run the focused global API/error tests.
5. Run `./gradlew check build --no-daemon`.
6. Commit with the required Korean subject format and create the PR through `scripts/task/create-pr.sh`.
7. Continue through review gate, manual merge notification, and post-merge cleanup unless an external blocker requires user input.

## Done Criteria
- `CursorPageResponse` and `PageInfoResponse` remain available for common cursor pagination responses.
- Request body validation and method/query parameter validation return `application/problem+json` with 422 status and `fieldErrors`.
- New and existing common API/error tests pass.
- Standard verification `./gradlew check build --no-daemon` passes and is recorded in task state.
- PR includes `EXEC_PLAN`, Jira `SPT-21`, verification evidence, and review gate checklist.

## Verification Evidence
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.global.error.ValidationProblemHandlerTest.invalidQueryParameterReturnsUnprocessableProblemDetailWithFieldErrors' --no-daemon` failed because invalid query parameter validation returned 400 instead of 422.
- GREEN: same focused test passed after adding `HandlerMethodValidationException` handling.
- Focused regression: `./gradlew test --tests 'com.studypot.aistudyleader.global.api.*' --tests 'com.studypot.aistudyleader.global.error.*' --no-daemon` passed.
- Standard verification: `./gradlew check build --no-daemon` passed.
