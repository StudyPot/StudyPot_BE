# EXEC_PLAN: [db] Flyway와 ERD v0.8 MySQL8 스키마 적용

- Task slug: `spt-22-db-flyway-erd-v0`
- Base branch: `develop`
- Feature branch: `codex/spt-22-db-flyway-erd-v0`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-22-db-flyway-erd-v0`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-22-db-flyway-erd-v0`
- Jira issue: `SPT-22`
- Jira URL: https://studypot.atlassian.net/browse/SPT-22
- Jira summary: [db] Flyway와 ERD v0.8 MySQL8 스키마 적용
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
- [x] docs/confluence/04-erd-data-model.md
- [x] docs/architecture/backend-map.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

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
- `docs/specs/db-contract-v1.md` and `docs/specs/domain-erd.md` are `LOCKED_FOR_IMPLEMENTATION`; no table, column, enum, FK, JSON shape, or DBMS change is allowed without Change Request + ADR.
- `docs/specs/db-schema-v1.sql` is the locked ERD v0.8 MySQL8 DDL draft. The Flyway `V1` migration must match it rather than introduce an alternate schema.
- ERD v0.8 defines 19 MVP tables: identity/auth, group/onboarding/rules, curriculum/todo, AI/retrospective, notification, and LLM usage.
- Application-generated UUIDv7 values are stored as `BINARY(16)`, time values use `TIMESTAMP(6)`, flexible structured values use MySQL `JSON`, and charset is `utf8mb4`.
- Deferred meeting/session/invitation/external notification tables must not be added in this task.
- PR flow requires `create-pr.sh`, latest-head GitHub Actions Review Gate and role gate evidence, human merge, then `finish-pr.sh cleanup-merged`.

## Goal
Apply the locked ERD v0.8 MySQL8 schema as the backend's first Flyway migration, add runtime dependencies/configuration needed for Flyway-managed MySQL migrations, and add tests that prevent drift between the locked schema document and the executable migration.

## Approach
Use test-driven implementation:

1. Add a migration contract test that fails while no Flyway `V1` migration exists.
2. Add Flyway/MySQL dependencies and a guarded test profile so existing Spring context tests do not require a local MySQL instance.
3. Add `src/main/resources/db/migration/V1__erd_v0_8_mysql8_schema.sql` with the locked schema content from `docs/specs/db-schema-v1.sql`.
4. Keep `application.yml` focused on Flyway location/validation; concrete datasource credentials remain external Spring configuration.
5. Extend harness contract coverage so CI checks both the documentation schema and the executable Flyway migration.

## Step Plan
- [x] Write failing contract tests for missing Flyway migration, migration naming, exact schema parity, and required/deferred table coverage.
- [x] Run the focused failing test to prove the test guards the missing migration.
- [x] Add Flyway/MySQL Gradle dependencies.
- [x] Add Flyway application configuration without committing secrets or environment-specific credentials.
- [x] Add the `V1__erd_v0_8_mysql8_schema.sql` migration from the locked DDL draft.
- [x] Run focused tests and harness DB/schema checks.
- [x] Run `./gradlew check build --no-daemon`.
- [ ] Commit with `[feat] ...` subject and create the PR through `scripts/task/create-pr.sh`.

Verification evidence:
- Expected failing test before migration: `./gradlew test --tests com.studypot.aistudyleader.persistence.FlywayMigrationContractTest --no-daemon` failed because the Flyway ERD baseline migration did not exist.
- Expected failing harness check before migration: `bash scripts/tests/test_quality_gate_contracts.sh` failed with missing Flyway ERD baseline migration.
- Focused contract test after implementation: `./gradlew test --tests com.studypot.aistudyleader.persistence.FlywayMigrationContractTest --no-daemon` passed.
- Harness contract check after implementation: `bash scripts/tests/test_quality_gate_contracts.sh` passed.
- Full test suite: `./gradlew test --no-daemon` passed.
- Harness suite: `bash scripts/tests/run.sh` passed.
- Standard verification: `./gradlew check build --no-daemon` passed.

## Done Criteria
- Flyway migration exists under `src/main/resources/db/migration/` and is named as a first-version schema migration.
- The migration content matches `docs/specs/db-schema-v1.sql` after normalized comparison.
- Contract tests cover happy path parity, migration naming/input validation, and forbidden deferred tables.
- Existing Spring context/security tests pass without requiring a local MySQL server.
- `./gradlew check build --no-daemon` passes and task state records the successful verification.
- PR targets `develop`, includes Jira `SPT-22`, and is created through the harness script.
