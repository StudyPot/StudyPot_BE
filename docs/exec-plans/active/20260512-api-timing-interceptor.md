# EXEC_PLAN: [observability] API 요청 처리 시간 로그 추가

- Task slug: `api-timing-interceptor`
- Base branch: `develop`
- Feature branch: `codex/api-timing-interceptor`
- Worktree: `/Users/hyunwoo/Developer/Projects/StudyPot`
- Port: `TBD`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/api-timing-interceptor`
- Jira issue: `SPT-77`
- Jira URL: https://studypot.atlassian.net/browse/SPT-77
- Jira summary: [observability] API 요청 처리 시간 로그 추가
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [ ] docs/operations/pr-review-gate.md
- [ ] docs/operations/jira-board-sync.md
- [ ] docs/operations/obsidian-error-ledger.md
- [ ] docs/...

## Related Feature IDs
- [x] n/a-harness
- [ ] <feature-id>

## Doc Notes
This is an observability/infrastructure change, not a product API/DB contract change. `docs/testing/codex-harness.md` confirms the standard verification command and the requirement to include tests with source changes.

## Goal
Record `/api/v1/**` request processing time with a Spring MVC interceptor without logging credentials, cookies, headers, or request bodies.

## Approach
Register an `ApiTimingInterceptor` through `WebMvcConfigurer` and scope it to the shared `ApiPaths.V1 + "/**"` API path. Store request-local start time as a servlet request attribute so concurrent requests do not share mutable state.

## Step Plan
1. Keep the user-authored interceptor and MVC configuration changes.
2. Add a focused unit test for interceptor timing state and completion behavior.
3. Run `./gradlew check build --no-daemon`.
4. Commit the scoped source, test, task-state, and EXEC_PLAN changes.

## Done Criteria
`/api/v1/**` requests pass through the timing interceptor, non-sensitive timing data is logged after completion, interceptor behavior is covered by tests, and the full Gradle verification passes.
