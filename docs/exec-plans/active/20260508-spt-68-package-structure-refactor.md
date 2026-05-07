# EXEC_PLAN: [refactor] identity 패키지 구조를 도메인 중심 3계층으로 정리

- Task slug: `spt-68-package-structure-refactor`
- Base branch: `develop`
- Feature branch: `codex/spt-68-package-structure-refactor`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-68-package-structure-refactor`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-68-package-structure-refactor`
- Jira issue: `SPT-68`
- Jira URL: https://studypot.atlassian.net/browse/SPT-68
- Jira summary: [refactor] identity 패키지 구조를 도메인 중심 3계층으로 정리
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/specs/change-control-v1.md
- [x] docs/architecture/backend-map.md

## Related Feature IDs
- [ ] n/a-harness
- [x] identity-core

## Doc Notes
- User decision: use a domain-oriented layered package style for APIs instead of hexagonal `adapter/in/out` naming.
- Target package vocabulary is `controller/service/domain/repository/infrastructure`.
- Keep `global` as cross-cutting code; absorb the small `shared` package into `global` so the top-level tree does not have another architectural vocabulary.
- This is a refactor only: no REST path, request/response shape, DB migration, domain behavior, JWT/OAuth behavior, or locked v1 spec change.
- `docs/specs/change-control-v1.md` allows this without ADR because API/DB/product semantics do not change.
- Existing architecture test currently enforces `adapter.in.web`; it must be updated to enforce the new package structure.

## Goal
Make the source tree match the user's preferred style: `global` for common cross-cutting code, and domain packages such as `identity` organized by `controller`, `service`, `domain`, `repository`, and `infrastructure`.

## Approach
1. Update architecture tests first so legacy `adapter` packages and old controller placement fail.
2. Move identity controller from `identity.adapter.in.web` to `identity.controller`.
3. Move identity use cases, commands, DTO-ish application results, service ports, and service exceptions from `identity.application` to `identity.service`.
4. Move identity repository interfaces and JDBC repository implementations from `identity.adapter.out.persistence` to `identity.repository`.
5. Move Google OAuth and JWT/security technical implementations from `identity.adapter.out.*` to `identity.infrastructure.*`.
6. Move small shared kernel types from `shared.*` to `global.*`.
7. Update all imports, package declarations, tests, and architecture docs without changing behavior.
8. Address Copilot feedback by enforcing `repository` not depending on `service`, moving refresh token session state into `domain`, and moving repository uniqueness conflicts into `repository`.

## Step Plan
- [x] RED: update architecture tests to require the new package structure and confirm they fail before the move.
- [x] Move production Java packages and update imports.
- [x] Move test Java packages and update imports.
- [x] Update architecture documentation to describe `controller/service/domain/repository/infrastructure`.
- [x] Run focused architecture/identity tests.
- [x] Run `./gradlew check build --no-daemon`.
- [ ] Create PR through the harness and address review feedback.

## Done Criteria
- No production or test Java source remains under `identity.adapter.*`, `identity.application.*`, or `shared.*`.
- Identity code is organized under `identity.controller`, `identity.service`, `identity.domain`, `identity.repository`, and `identity.infrastructure.*`.
- Shared kernel types are available from `global.domain` or `global.application`.
- Architecture tests enforce the new domain-oriented layered package convention.
- Existing behavior tests still pass.
- `./gradlew check build --no-daemon` passes.

## Verification
- RED confirmed: `./gradlew test --tests 'com.studypot.aistudyleader.architecture.LayeredArchitectureTest' --no-daemon` failed before package moves because legacy placement rules were still violated.
- PASS: `./gradlew test --tests 'com.studypot.aistudyleader.architecture.LayeredArchitectureTest' --no-daemon`.
- PASS: `./gradlew test --tests 'com.studypot.aistudyleader.architecture.*' --tests 'com.studypot.aistudyleader.identity.*' --tests 'com.studypot.aistudyleader.global.domain.*' --no-daemon`.
- PASS: `./gradlew check build --no-daemon`.
- PASS: `bash scripts/tests/run.sh`.
- Copilot review: latest-head review received on PR #49 with 1 actionable thread. Addressed repository-to-service dependency leakage by moving `RefreshTokenSession` to `identity.domain`, moving `IdentityUniquenessConflictException` to `identity.repository`, and adding an ArchUnit repository dependency rule.
- RED after Copilot feedback: `./gradlew test --tests 'com.studypot.aistudyleader.architecture.LayeredArchitectureTest' --no-daemon` failed before the dependency cleanup because repository code depended on service types.
- PASS after Copilot feedback: `./gradlew test --tests 'com.studypot.aistudyleader.architecture.LayeredArchitectureTest' --no-daemon`.
- PASS after Copilot feedback: `./gradlew test --tests 'com.studypot.aistudyleader.architecture.*' --tests 'com.studypot.aistudyleader.identity.*' --tests 'com.studypot.aistudyleader.global.domain.*' --no-daemon`.
- PASS after Copilot feedback: `./gradlew check build --no-daemon`.
- PASS after Copilot feedback: `bash scripts/tests/run.sh`.
