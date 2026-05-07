# EXEC_PLAN: [foundation] Scaffold Spring Boot Java 21 Gradle project

- Task slug: `be-scaffold`
- Base branch: `develop`
- Feature branch: `codex/be-scaffold`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/be-scaffold`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/be-scaffold`
- Jira issue: `SPT-19`
- Jira URL: https://studypot.atlassian.net/browse/SPT-19
- Jira summary: [foundation] Spring Boot Java 21 Gradle 프로젝트 뼈대 만들기
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/specs/product-brief.md
- [x] docs/specs/prd-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/change-control-v1.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- Jira SPT-19 defines this as a foundation scaffold task with `REQ-GLOBAL-001`, `QA-GLOBAL-002`, and `QA-GLOBAL-005` as traceability anchors.
- Locked v1 docs require Java 21, Gradle, Spring Boot, REST APIs under `/api/v1`, OpenAPI 3.1, RFC 7807 `application/problem+json`, cursor pagination, and application-generated UUIDv7 for later persistence work.
- `docs/testing/codex-harness.md` states the Spring scaffold verification command should be `./gradlew check build --no-daemon` once the backend skeleton exists.
- `docs/specs/change-control-v1.md` locks product/API/DB behavior; this task creates scaffold and guardrails only, without adding new production endpoints or changing locked API contracts.

## Goal
Create a Java 21 + Gradle + Spring Boot backend scaffold that gives future product features a DDD-oriented package structure, API/security/error handling primitives, and executable verification.

## Approach
Use the official Spring Initializr metadata current on 2026-04-29 for Spring Boot 4.0.6 and Gradle wrapper 9.4.1. Keep production behavior minimal: no new product endpoint is introduced, while shared API DTOs, ProblemDetail handling, security entrypoints, and DDD base types are covered by tests. Add ArchUnit tests so domain/application code cannot depend on adapter or Spring infrastructure packages.

## Step Plan
1. Create Gradle/Spring Boot project files and wrapper.
2. Add DDD shared domain/application types and bounded-context package skeletons.
3. Add global API, error, and security primitives aligned with `/api/v1` and `application/problem+json`.
4. Add context, security, validation, pagination, domain, and layer guardrail tests.
5. Update harness docs to replace the placeholder verification command with `./gradlew check build --no-daemon`.
6. Run `./gradlew check build --no-daemon` and record the result.

## Done Criteria
- `./gradlew check build --no-daemon` passes locally.
- Spring Boot application context loads on Java 21.
- DDD layer guardrail tests pass.
- Validation errors produce `422` ProblemDetail with `fieldErrors`.
- Unauthenticated protected `/api/v1/**` access produces `401 application/problem+json`.
- Cursor page response includes `items` and `pageInfo`.
- Architecture and harness docs describe the new Spring Boot scaffold and verification command.
