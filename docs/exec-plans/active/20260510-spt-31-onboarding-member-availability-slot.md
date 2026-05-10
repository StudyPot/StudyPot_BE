# EXEC_PLAN: [onboarding] member_availability_slot 저장/검증 구현

- Task slug: `spt-31-onboarding-member-availability-slot`
- Base branch: `develop`
- Feature branch: `codex/spt-31-onboarding-member-availability-slot`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-31-onboarding-member-availability-slot`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-31-onboarding-member-availability-slot`
- Jira issue: `SPT-31`
- Jira URL: https://studypot.atlassian.net/browse/SPT-31
- Jira summary: [onboarding] member_availability_slot 저장/검증 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/domain-erd.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/architecture/backend-map.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] group-onboarding

## Doc Notes
- `REQ-ONB-002` requires recurring availability slots with day of week, start time, end time, and timezone.
- `QA-ONB-003` requires invalid day/time windows to be rejected.
- Locked OpenAPI already includes `availabilitySlots` in `SaveOnboardingRequest` and `OnboardingResponse`; SPT-31 must implement the existing API shape instead of changing the contract.
- DB schema already has `member_availability_slot` with FK links to `group_onboarding_response` and `group_member`, `day_of_week` check `0..6`, and `end_time > start_time`.
- SPT-30 intentionally accepted only empty `availabilitySlots`; SPT-31 removes that temporary guard and persists/returns real slots.
- Slot replacement will delete the current response's previous slot rows and insert the current request list in one service transaction. The table has no stable natural key or display order column, and the user goal says repeated saves should not leave duplicate rows.
- This task stays in draft save/read scope. Submit, member `ACTIVE` transition, group completion, curriculum scheduling/optimization, and notifications remain follow-up work.

## Goal
SPT-31의 목표는 SPT-30에서 저장/조회할 수 있게 된 내 온보딩 응답에 `availabilitySlots`를 실제로 저장하고 다시 조회할 수 있도록 `member_availability_slot` 저장/검증 백엔드 수직 흐름을 완성하는 것이다.

## Approach
- Add a domain value/entity for `MemberAvailabilitySlot` that validates day range, `HH:mm` time parsing, `endTime > startTime`, and valid Java `ZoneId` timezone values.
- Extend `GroupOnboardingResponse` to carry immutable availability slot lists and validate that slots belong to the same onboarding response/member.
- Extend `SaveMyOnboardingCommand` and the REST request DTO to accept typed availability slots instead of the SPT-30 temporary `List<Map<String,Object>>` empty-only guard.
- Extend `OnboardingService.saveMyDraft` to create slot IDs from the existing UUID generator, attach slots to the draft response, and keep all validation errors mapped to `InvalidOnboardingRequestException(field = "availabilitySlots")`.
- Extend `JdbcOnboardingRepository` so `saveDraft` upserts `group_onboarding_response`, replaces current `member_availability_slot` rows for the response, inserts the requested slot rows, and returns the saved aggregate.
- Extend response mapping so `GET/PUT /api/v1/groups/{groupId}/onboarding/me` returns saved availability slots.

## Step Plan
1. Write failing domain tests for valid availability slot creation, invalid day, invalid time order, invalid time format, invalid timezone, and response/member mismatch.
2. Write failing service tests for saving availability slots, updating an existing response with the current slot list, retrieving slots, and translating invalid slot input to `availabilitySlots` field errors.
3. Write failing controller tests that non-empty `availabilitySlots` are accepted and returned, and invalid slot payloads are rejected with ProblemDetail validation.
4. Write failing repository tests for loading response slots and replacing persisted slots during `saveDraft`.
5. Run focused onboarding tests and confirm RED failures are due to missing SPT-31 behavior.
6. Implement minimal domain/service/controller/repository changes.
7. Run focused onboarding tests until GREEN.
8. Run `./gradlew check build --no-daemon`.
9. Commit as `[feat] 온보딩 가능 시간 저장 조회 구현`.
10. Create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, address feedback once if needed, wait for latest-head review gate, and run `scripts/task/finish-pr.sh`.

## Done Criteria
- `PUT /api/v1/groups/{groupId}/onboarding/me` stores keyword skill levels, task preferences, additional note, and availability slots for the authenticated current group member.
- `GET /api/v1/groups/{groupId}/onboarding/me` returns the authenticated member's saved availability slots with the onboarding response.
- Each slot is linked to the saved `group_onboarding_response` and `group_member`.
- Re-saving onboarding replaces the current member response's slot set with the current request list without leaving duplicate active rows.
- Day of week outside `0..6`, malformed times, `endTime <= startTime`, blank/invalid timezone, missing group, and non-member access are rejected with clear ProblemDetail errors.
- Existing SPT-30 score/key validation continues to work.
- Availability slot storage/read tests cover domain, service, controller, and repository behavior.
- `./gradlew check build --no-daemon` passes and PR review gate requirements are satisfied.

## Verification Evidence
- Baseline before SPT-31 tests: `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.*' --no-daemon` -> PASS.
- RED after SPT-31 tests: `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.*' --no-daemon` -> FAIL as expected because `MemberAvailabilitySlot` and `AvailabilitySlotCommand` were missing.
- Focused after implementation: `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.*' --no-daemon` -> PASS.
- Full verification: `./gradlew check build --no-daemon` -> PASS.
