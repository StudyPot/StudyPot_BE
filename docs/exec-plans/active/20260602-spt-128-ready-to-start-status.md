# EXEC_PLAN: [study-group] 온보딩 완료 후 시작 대기 상태 구현

- Task slug: `spt-128-ready-to-start-status`
- Base branch: `develop`
- Feature branch: `codex/spt-128-ready-to-start-status`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-128-ready-to-start-status`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-128-ready-to-start-status`
- Jira issue: `SPT-128`
- Jira URL: https://studypot.atlassian.net/browse/SPT-128
- Jira summary: [study-group] 온보딩 완료 후 시작 대기 상태 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-request-template.md
- [x] docs/specs/adr-template.md
- [x] docs/specs/product-brief.md
- [x] docs/specs/prd-v1.md
- [x] docs/specs/user-journeys-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/domain-erd.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/confluence/01-mvp-golden-path.md
- [x] docs/confluence/02-requirements.md
- [x] docs/confluence/04-erd-data-model.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/07-permissions-state.md
- [x] docs/confluence/09-qa-acceptance.md
- [x] docs/confluence/10-jira-mapping.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] study-group-core
- [x] group-onboarding
- [x] curriculum-core

## Doc Notes
- User request: represent the state where study onboarding is done but the host has not clicked study start yet.
- Product decision for this task: add `study_group.status = READY_TO_START` between `ONBOARDING` and `ACTIVE`.
- Preserve the existing locked behavior that the host can start before every invitee completes onboarding. Therefore `READY_TO_START` means the owner/host onboarding has been submitted and the owner membership is active; invited members may still be `PENDING_ONBOARDING`.
- `READY_TO_START` is not an active-study state. Curriculum, board, retrospective, notification-log, and LLM usage features that require `ACTIVE` membership/group status should continue to require `ACTIVE`.
- The enum change requires CR/ADR plus source spec, OpenAPI, DB check constraint, and Confluence draft updates.
- CodeRabbit review confirmed the Flyway V1 baseline must not be rewritten after prior application; keep `READY_TO_START` in the V4 migration and current enum contracts while leaving the V1 baseline DDL unchanged.

## Goal
Add an explicit `READY_TO_START` study group status for the post-host-onboarding, pre-start gap, and make host start transition `READY_TO_START -> ACTIVE`.

## Approach
- Follow TDD: add failing service/repository/API contract tests before production code.
- Add approved Change Request and ADR dated 2026-06-02 for the locked enum and state-transition change.
- Extend `StudyGroupStatus` and DB/API/docs status lists with `READY_TO_START`.
- In onboarding submit, after the owner is active, transition the group from `ONBOARDING` to `READY_TO_START` when the submitting member is the group owner.
- In curriculum start, require `READY_TO_START` instead of `ONBOARDING`, and update the repository transition guard to `READY_TO_START -> ACTIVE`.
- Include `READY_TO_START` in current-member visible group queries and join-target acceptance so pending invitees can still join/onboard before the host starts.
- Keep active-only read/write surfaces unchanged.

## Step Plan
1. Add failing tests for onboarding owner submission transitioning the group to `READY_TO_START`, non-owner submission not transitioning the group, start requiring `READY_TO_START`, and SQL/docs contract expectations.
2. Implement repository API and SQL for `ONBOARDING -> READY_TO_START`.
3. Update curriculum start service and SQL from `ONBOARDING` guard to `READY_TO_START`.
4. Update OpenAPI, DB schema check constraint, source specs, CR/ADR, and Confluence drafts.
5. Run focused tests for onboarding, curriculum, study-group repository/controller, and docs/static contracts.
6. Run `./gradlew check build --no-daemon`.
7. Commit with `[feat] ...`, create PR through `scripts/task/create-pr.sh`, run CodeRabbit review, address one feedback pass if needed, verify PR readiness, and finish PR.

## Done Criteria
- Jira SPT-128 remains linked in task state and PR body.
- `study_group.status` supports `READY_TO_START` in Java enum, OpenAPI enum, DB check constraint, source specs, and Confluence drafts.
- Owner onboarding submission moves an `ONBOARDING` group to `READY_TO_START`.
- Non-owner onboarding submission does not move the group status.
- Host start accepts `READY_TO_START` and still rejects groups that are not ready.
- Existing active-only feature access does not broaden to `READY_TO_START`.
- Focused tests and `./gradlew check build --no-daemon` pass.
- PR review gate and CodeRabbit marker requirements are satisfied before merge/cleanup.

## Verification Evidence
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.service.OnboardingServiceTest' --tests 'com.studypot.aistudyleader.onboarding.repository.JdbcOnboardingRepositoryTest' --tests 'com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest' --tests 'com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest' --tests 'com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTargetTest' --no-daemon` failed because `StudyGroupStatus.READY_TO_START`, `MARK_STUDY_GROUP_READY_TO_START`, and repository readiness transition methods did not exist.
- GREEN focused: same focused test command passed after implementation.
- Related Java sweep: `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.*' --tests 'com.studypot.aistudyleader.curriculum.*' --tests 'com.studypot.aistudyleader.studygroup.*' --tests 'com.studypot.aistudyleader.persistence.*' --no-daemon` passed.
- Static contracts: `bash scripts/tests/test_quality_gate_contracts.sh`, `bash scripts/tests/test_pr_scripts_static.sh`, and `bash scripts/tests/test_swagger_docs_contracts.sh` passed.
- Full verification: `./gradlew check build --no-daemon` passed.
- CodeRabbit review on PR #206 raised 2 issues: duplicate product-brief numbering and V1/V4 check-constraint migration conflict.
- CodeRabbit addressed scope: renumbered the MVP Golden Path steps, restored `V1__erd_v0_8_mysql8_schema.sql` and `docs/specs/db-schema-v1.sql` to the unchanged ERD baseline, documented `V4__study_group_ready_to_start_status.sql` as the single executable status check-constraint change, and added `FlywayMigrationContractTest` coverage for that policy.
- After CodeRabbit fixes, `./gradlew test --tests 'com.studypot.aistudyleader.persistence.FlywayMigrationContractTest' --no-daemon`, `bash scripts/tests/test_quality_gate_contracts.sh`, `bash scripts/tests/test_pr_scripts_static.sh`, `bash scripts/tests/test_swagger_docs_contracts.sh`, and `./gradlew check build --no-daemon` passed.
