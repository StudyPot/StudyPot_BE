# EXEC_PLAN: [db-core] UUIDv7/audit/soft delete 공통 정책

- Task slug: `spt-23-db-core-uuidv7-audit`
- Base branch: `develop`
- Feature branch: `codex/spt-23-db-core-uuidv7-audit`
- Worktree: `<generated feature worktree>/spt-23-db-core-uuidv7-audit`
- Port: `18080`
- Log dir: `<generated task log dir>/spt-23-db-core-uuidv7-audit`
- Jira issue: `SPT-23`
- Jira URL: https://studypot.atlassian.net/browse/SPT-23
- Jira summary: [db-core] UUIDv7/audit/soft delete 공통 정책
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/domain-erd.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/confluence/04-erd-data-model.md
- [x] docs/confluence/10-jira-mapping.md
- [x] docs/architecture/backend-map.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/operations/error-ledger.md
- [x] Jira SPT-23 description
- [x] RFC 9562 UUID version 7 layout

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
- SPT-23 acceptance requires application-generated UUIDv7 values stored as MySQL `BINARY(16)`, unified `created_at`/`updated_at`/`deleted_at` timestamp precision, and tests for live-row unique constraints in group/member/onboarding/curriculum domains.
- `docs/specs/db-contract-v1.md` and `docs/specs/domain-erd.md` are locked and define UUIDv7, `TIMESTAMP(6)`, JSON, and nullable `deleted_at` as the DB baseline.
- `docs/specs/db-schema-v1.sql` is the locked V1 baseline and must remain unchanged; SPT-23 should add follow-up executable migration policy instead of rewriting the V1 source schema.
- RFC 9562 defines UUIDv7 as a 48-bit Unix epoch millisecond timestamp in the most significant bits, version `7`, RFC variant bits, and random remaining bits.
- Current V1 migration already covers `BINARY(16)` IDs and audit columns, but group/member/onboarding/curriculum unique indexes are not live-row-aware once soft-deleted rows exist.
- MySQL unique indexes permit multiple `NULL` values, so live-row uniqueness can be modeled with generated columns that evaluate to the natural key only while `deleted_at is null`.
- PR flow requires latest-head Copilot review, resolved threads, role gate evidence, `review-gate-pass`, Mattermost manual merge notification, human merge, and cleanup.

## Goal
Implement the common DB core policy for UUIDv7 identifiers, audit timestamp/soft-delete handling, and live-row uniqueness so later identity, group, onboarding, curriculum, todo, AI, and notification features share one tested persistence baseline.

## Approach
Use test-driven implementation:

1. Add failing focused tests for UUIDv7 generation, UUID-to-`BINARY(16)` conversion, audit timestamp behavior, soft-delete state, and DB schema policy.
2. Keep V1 Flyway/schema parity intact and add a V2 migration for live-row unique generated columns/indexes required by SPT-23.
3. Add framework-free shared domain DB-core primitives for UUIDv7 IDs, audit timestamps, and soft-delete metadata.
4. Add persistence binary conversion helpers so adapters can store/read UUID values as 16-byte MySQL values without ad hoc byte manipulation.
5. Extend schema contract coverage to prove PK/FK `BINARY(16)`, `TIMESTAMP(6)` audit precision, and live-row unique constraints for `study_group`, `group_member`, `group_onboarding_response`, and `curriculum`.

## Step Plan
- [x] Write RED tests for UUIDv7 generation and binary conversion.
- [x] Write RED tests for audit timestamp and soft-delete primitives.
- [x] Write RED schema contract tests for V2 live-row unique policy.
- [x] Run the focused RED tests and record expected failures.
- [x] Implement the minimal shared DB-core primitives and persistence helpers.
- [x] Add `V2__db_core_live_row_unique_constraints.sql`.
- [x] Run focused tests.
- [x] Run harness DB/schema checks.
- [x] Run `bash scripts/tests/run.sh`.
- [x] Run `./gradlew check build --no-daemon`.
- [ ] Commit with `[feat] ...` subject and create PR through `scripts/task/create-pr.sh`.
- [ ] Address Copilot/reviewdog feedback, post role gate evidence, and send Mattermost manual merge notification through `finish-pr.sh`.

## Verification Log
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.shared.domain.UuidV7Test' --tests 'com.studypot.aistudyleader.shared.domain.AuditMetadataTest' --tests 'com.studypot.aistudyleader.global.persistence.UuidBinaryTest' --tests 'com.studypot.aistudyleader.persistence.DbCorePolicyContractTest' --no-daemon` failed as expected because `UuidV7`, `AuditMetadata`, and `UuidBinary` did not exist yet.
- GREEN: focused DB-core tests passed with the same command after implementation.
- PASS: `./gradlew test --no-daemon`
- PASS: `bash scripts/tests/run.sh`
- PASS: `./gradlew check build --no-daemon`
- Copilot review: latest-head review received on PR #36 with 4 actionable threads. Addressed optional backtick handling for PK assertions, case-insensitive audit column presence checks, UUIDv7 ordering test semantics, and UUID binary conversion allocation.
- PASS after Copilot fixes: `./gradlew test --tests 'com.studypot.aistudyleader.shared.domain.UuidV7Test' --tests 'com.studypot.aistudyleader.global.persistence.UuidBinaryTest' --tests 'com.studypot.aistudyleader.persistence.DbCorePolicyContractTest' --no-daemon`
- PASS after Copilot fixes: `bash scripts/tests/run.sh`
- PASS after Copilot fixes: `./gradlew check build --no-daemon`
- Copilot re-review: latest-head re-review received on PR #36 with 3 actionable threads. Addressed local absolute path exposure in EXEC_PLAN, more tolerant `ENGINE =` schema parsing, and strict RFC variant validation for UUIDv7.
- PASS after Copilot re-review fixes: `./gradlew test --tests 'com.studypot.aistudyleader.shared.domain.UuidV7Test' --tests 'com.studypot.aistudyleader.persistence.DbCorePolicyContractTest' --no-daemon`
- PASS after Copilot re-review fixes: `bash scripts/tests/run.sh`
- PASS after Copilot re-review fixes: `./gradlew check build --no-daemon`
- Copilot third review: latest-head third review received on PR #36 with 2 actionable threads. Addressed whitespace-tolerant V2 SQL assertions and Gradle Kotlin DSL root detection.
- PASS after Copilot third review fixes: `./gradlew test --tests 'com.studypot.aistudyleader.persistence.DbCorePolicyContractTest' --no-daemon`
- PASS after Copilot third review fixes: `bash scripts/tests/run.sh`
- PASS after Copilot third review fixes: `./gradlew check build --no-daemon`
- Copilot fourth review: latest-head fourth review received on PR #36 with 4 actionable threads. Addressed migration contract tests reading Flyway SQL from the test runtime classpath and documented V2 generated-column/index DDL rollout locking risk.
- PASS after Copilot fourth review fixes: `./gradlew cleanTest test --tests 'com.studypot.aistudyleader.persistence.DbCorePolicyContractTest' --no-daemon`
- PASS after Copilot fourth review fixes: `bash scripts/tests/run.sh`
- PASS after Copilot fourth review fixes: `./gradlew clean check build --no-daemon` followed by `./gradlew check build --no-daemon`
- Copilot fifth review: latest-head fifth review received on PR #36 with 2 actionable threads. Enforced monotonic audit timestamps and changed default UUIDv7 randomness to per-thread generation.
- PASS after Copilot fifth review fixes: `./gradlew cleanTest test --tests 'com.studypot.aistudyleader.shared.domain.AuditMetadataTest' --tests 'com.studypot.aistudyleader.shared.domain.UuidV7Test' --no-daemon`
- PASS after Copilot fifth review fixes: `bash scripts/tests/run.sh`
- PASS after Copilot fifth review fixes: `./gradlew check build --no-daemon`
- Copilot sixth review: latest-head sixth review received on PR #36 with 2 actionable threads. Made SQL normalization locale-independent and renamed the UUID binary fresh-array test.
- PASS after Copilot sixth review fixes: `./gradlew cleanTest test --tests 'com.studypot.aistudyleader.persistence.DbCorePolicyContractTest' --tests 'com.studypot.aistudyleader.global.persistence.UuidBinaryTest' --no-daemon`
- PASS after Copilot sixth review fixes: `bash scripts/tests/run.sh`
- PASS after Copilot sixth review fixes: `./gradlew check build --no-daemon`

## Done Criteria
- UUIDv7 generation produces RFC 9562 version 7, RFC variant UUIDs with millisecond timestamp ordering and injectable clock/randomness for tests.
- UUID binary conversion round-trips Java `UUID` values to exactly 16 bytes and rejects invalid input.
- Audit and soft-delete primitives use `Instant` values truncated to microsecond precision for MySQL `TIMESTAMP(6)`.
- V1 migration remains the locked ERD v0.8 baseline and V2 adds live-row unique generated columns/indexes without adding unrelated product tables.
- Contract tests cover PK/FK `BINARY(16)`, audit/soft-delete timestamp policy, and live-row uniqueness for group/member/onboarding/curriculum domains.
- `bash scripts/tests/run.sh` and `./gradlew check build --no-daemon` pass.
- PR targets `develop`, includes Jira `SPT-23`, receives latest-head Copilot review, resolves all actionable feedback, passes review gates, and sends Mattermost manual merge readiness.
