# EXEC_PLAN: [study-group] 그룹 상태 전환/인원 제한 검증 구현

- Task slug: `spt-34-group-status-limits`
- Base branch: `develop`
- Feature branch: `codex/spt-34-group-status-limits`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-34-group-status-limits`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-34-group-status-limits`
- Jira issue: `SPT-34`
- Jira URL: https://studypot.atlassian.net/browse/SPT-34
- Jira summary: [study-group] 그룹 상태 전환/인원 제한 검증 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] Jira SPT-34 issue detail
- [x] docs/specs/prd-v1.md
- [x] docs/specs/user-journeys-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/domain-erd.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/architecture/backend-map.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] study-group-core
- [x] group-onboarding
- [x] curriculum-core

## Doc Notes
- Jira SPT-34 scope is `study_group.status` and `group_member.status`; acceptance calls out locked enum values and late joiners entering from the current week after onboarding completion.
- Existing SPT-29 join flow intentionally allowed only `ONBOARDING` groups; SPT-34 extends that deferred late-joiner path so `ACTIVE` groups can accept invite joins while keeping capacity and duplicate membership validation.
- Existing SPT-30/SPT-31 onboarding flow saves drafts only. Locked API already defines `POST /api/v1/groups/{groupId}/onboarding/me/submit`, so SPT-34 uses that endpoint to transition `group_onboarding_response` to `SUBMITTED` and the corresponding `group_member` to `ACTIVE`.
- `study_group` itself already transitions `ONBOARDING -> ACTIVE` inside SPT-32 curriculum start persistence. SPT-34 will strengthen that transition by requiring the owner member to be `ACTIVE`, which means the host submitted onboarding before starting.
- "현재 주차부터 참여" cannot create weekly progress until SPT-36 introduces `member_week_progress`; this task provides the status prerequisite by allowing active-group join + onboarding submit -> active member. Current-week progress creation remains out of scope for SPT-34.
- The locked v1 specs are not changed in this task.

## Goal
SPT-34의 목표는 그룹과 멤버 상태 전이가 주차 실행 전제에 맞게 동작하도록 보강하는 것이다. 인증 사용자는 `ACTIVE` 그룹에도 초대 코드로 합류할 수 있지만 처음에는 `PENDING_ONBOARDING` 멤버로 남고, 온보딩 제출 후에만 `ACTIVE` 멤버가 된다. 호스트 시작은 기존 SPT-32의 curriculum 생성 흐름을 유지하되 owner가 온보딩을 제출해 `ACTIVE` 멤버가 된 경우에만 `study_group.status = ACTIVE` 전환을 허용한다.

## Approach
- 기존 bounded context를 유지한다. active-group join과 capacity/duplicate validation은 `studygroup`에, onboarding submit과 member activation은 `onboarding`에, start precondition 보강은 `curriculum` service에 최소 변경으로 둔다.
- TDD 순서를 지킨다. 먼저 domain/service/controller/repository 테스트를 추가해 실패를 확인한 뒤 구현한다.
- `StudyGroupJoinTarget`은 `ONBOARDING`과 `ACTIVE`를 join 가능 상태로 판단하되 `DRAFT`, `COMPLETED`, `ARCHIVED`는 거절한다. capacity count는 기존처럼 live `PENDING_ONBOARDING`/`ACTIVE` 멤버를 포함한다.
- `GroupOnboardingResponse.submit(now)`는 제출 상태와 `submittedAt`을 모델링한다. `OnboardingService.submitMyOnboarding`은 현재 멤버 context를 확인하고 기존 draft/response를 제출한 뒤, 멤버가 `PENDING_ONBOARDING`이면 `ACTIVE`로 전환한다. 이미 `ACTIVE`인 late/current member submit은 멤버 activation을 반복하지 않고 응답을 반환한다.
- `CurriculumService.startStudy`는 owner-only와 `ONBOARDING` group 검증에 더해 owner member status가 `ACTIVE`인지 확인한다.

## Step Plan
1. RED domain tests:
   - `StudyGroupJoinTarget` accepts both `ONBOARDING` and `ACTIVE`.
   - `GroupMember` can transition `PENDING_ONBOARDING -> ACTIVE` with `activatedAt`.
   - `GroupOnboardingResponse.submit(now)` sets `SUBMITTED` and `submittedAt`.
2. GREEN domain implementation for joinable group statuses, member activation, and onboarding submission.
3. RED service tests:
   - `StudyGroupService.joinGroup` accepts an `ACTIVE` group while preserving capacity and duplicate validation.
   - `OnboardingService.submitMyOnboarding` submits an existing response and activates a pending member.
   - `OnboardingService.submitMyOnboarding` rejects missing response/non-member/left member paths.
   - `CurriculumService.startStudy` rejects an owner whose onboarding is still pending.
4. GREEN service/repository port changes:
   - add onboarding submit command/query method as needed.
   - add member status to `OnboardingMemberContext`.
   - add repository method to save submitted onboarding and activate pending member.
   - add curriculum owner-active precondition.
5. RED repository tests:
   - active group join target maps correctly through existing join query.
   - onboarding submit SQL updates `group_onboarding_response.status = SUBMITTED`, stores `submitted_at`, and updates `group_member.status = ACTIVE` from pending only.
   - curriculum start context test covers owner/member status mapping.
6. GREEN JDBC implementation and SQL updates.
7. RED controller tests:
   - `POST /api/v1/groups/{groupId}/onboarding/me/submit` requires authentication.
   - submit returns `SUBMITTED`, `submittedAt`, and keeps the locked `OnboardingResponse` shape.
   - active-group join returns pending member with existing `GroupMemberResponse` shape.
8. GREEN controller implementation for submit endpoint.
9. Run focused tests for studygroup/onboarding/curriculum slices.
10. Run `./gradlew check build --no-daemon`.
11. Commit with `[feat] 그룹 상태 전환 검증 보강`.
12. Create PR with `scripts/task/create-pr.sh`, run `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`, address one CodeRabbit feedback loop if needed, and finish manual merge readiness through `scripts/task/finish-pr.sh`.

## Done Criteria
- `POST /api/v1/groups/{groupId}/join` accepts `ONBOARDING` and `ACTIVE` groups with matching invite code, capacity available, and no existing live membership.
- Join still rejects non-joinable group statuses, duplicate live membership, and capacity overflow.
- `POST /api/v1/groups/{groupId}/onboarding/me/submit` requires authentication and current group membership.
- Onboarding submit stores `group_onboarding_response.status = SUBMITTED` and `submitted_at`.
- Onboarding submit transitions a pending `group_member` to `ACTIVE` and stores `activated_at`.
- `LEFT` or non-member users cannot submit onboarding through this flow.
- `POST /api/v1/groups/{groupId}/start` remains owner-only and `ONBOARDING`-group-only, and additionally rejects owner members that have not submitted onboarding.
- Late joiner current-week progress creation is documented as SPT-36 scope, not implemented here.
- Happy path, edge case, input validation, permission/status transition, repository SQL/mapping, and controller API tests cover the changed behavior.
- `./gradlew check build --no-daemon` passes and task state records the successful verification.
- PR is created against `develop`; CodeRabbit review marker and GitHub Actions Review Gate pass before manual merge notification.

## Implementation Notes
- Extended `StudyGroupJoinTarget` so invite join accepts `ONBOARDING` and `ACTIVE` groups. Joined users still start as `PENDING_ONBOARDING` and capacity/duplicate membership validation remains unchanged.
- Added `POST /api/v1/groups/{groupId}/onboarding/me/submit` using the locked OpenAPI path. Submission transitions the existing onboarding response to `SUBMITTED`, stores `submitted_at`, and activates a pending group member.
- Added member status to onboarding member context so `LEFT` members remain excluded and pending-member activation failures are not ignored.
- Strengthened curriculum start so the owner must be an `ACTIVE` member before `study_group.status` can move from `ONBOARDING` to `ACTIVE` through the SPT-32 start flow.
- Did not create `member_week_progress` for late joiners in SPT-34; current-week progress generation remains SPT-36 scope.

## Verification
- [x] `./gradlew test --tests com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTargetTest --tests com.studypot.aistudyleader.studygroup.domain.GroupMemberTest --tests com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponseTest --no-daemon`
- [x] `./gradlew test --tests com.studypot.aistudyleader.studygroup.service.StudyGroupServiceTest --tests com.studypot.aistudyleader.onboarding.service.OnboardingServiceTest --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --no-daemon`
- [x] `./gradlew test --tests com.studypot.aistudyleader.studygroup.repository.JdbcStudyGroupRepositoryTest --tests com.studypot.aistudyleader.onboarding.repository.JdbcOnboardingRepositoryTest --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --no-daemon`
- [x] `./gradlew test --tests com.studypot.aistudyleader.onboarding.controller.OnboardingControllerTest --tests com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest --no-daemon`
- [x] `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.*' --tests 'com.studypot.aistudyleader.onboarding.*' --tests 'com.studypot.aistudyleader.curriculum.*' --no-daemon`
- [x] `./gradlew check build --no-daemon`
