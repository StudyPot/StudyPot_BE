# EXEC_PLAN: [onboarding] group_onboarding_response 저장/조회 구현

- Task slug: `spt-30-onboarding-group-onboarding-response`
- Base branch: `develop`
- Feature branch: `codex/spt-30-onboarding-group-onboarding-response`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-30-onboarding-group-onboarding-response`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-30-onboarding-group-onboarding-response`
- Jira issue: `SPT-30`
- Jira URL: https://studypot.atlassian.net/browse/SPT-30
- Jira summary: [onboarding] group_onboarding_response 저장/조회 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/domain-erd.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/architecture/backend-map.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] group-onboarding

## Doc Notes
- `group-onboarding` is the next MVP golden-path step after SPT-28 group creation and SPT-29 member join.
- `REQ-ONB-001` requires host/member onboarding responses to store keyword skill levels, task preferences, note, and submitted timestamp. SPT-30 handles draft save/read only; submitted timestamp remains null until the later submit task.
- `REQ-ONB-002` and `QA-ONB-003` cover availability slots, but the user goal explicitly defers availability slot storage. The locked OpenAPI request shape still contains `availabilitySlots`; SPT-30 accepts the field as an empty list and rejects non-empty slots rather than silently dropping data.
- `REQ-ONB-003` and `QA-ONB-002` require keyword skill and task preference scores to stay in the 1 to 5 range.
- Draft save allows partial maps. The later submit flow can enforce completeness before `SUBMITTED`; SPT-30 only validates provided keyword keys against the group's `detailKeywords` and task keys against the locked task type enum.
- Access is group-member scoped. The service resolves the current user's live `PENDING_ONBOARDING` or `ACTIVE` membership for the group before reading or saving my onboarding response.

## Goal
SPT-30의 목표는 SPT-29에서 스터디 그룹에 가입된 멤버가 자기 온보딩 응답을 저장하고 다시 조회할 수 있도록 `group_onboarding_response` 저장/조회 백엔드 수직 흐름을 완성하는 것이다. 최초 저장은 `DRAFT` 상태로 생성하고, 이후 저장은 같은 멤버의 기존 응답 row를 갱신한다.

## Approach
- 새 `onboarding` bounded context를 `domain`, `service`, `repository`, `controller` 계층으로 추가한다.
- `OnboardingService`는 인증 사용자와 그룹 ID로 현재 멤버십을 확인하고, 그룹의 `detailKeywords` 기준으로 keyword score key/value를 검증한 뒤 draft response를 upsert한다.
- `OnboardingRepository` JDBC 구현은 `study_group`, `group_member`, `group_onboarding_response`를 조회/저장하며 JSON 필드는 `ObjectMapper`로 직렬화한다.
- REST API는 locked OpenAPI 경로인 `GET /api/v1/groups/{groupId}/onboarding/me`, `PUT /api/v1/groups/{groupId}/onboarding/me`를 구현한다. `POST /submit`은 후속 작업 범위로 남긴다.
- 오류 응답은 기존 ProblemDetail 형식을 재사용한다: 그룹 없음/응답 없음은 404, 멤버가 아니면 403, 잘못된 입력은 422.

## Step Plan
1. SPT-29 이후 develop 코드에서 `studygroup` service/repository/controller 패턴과 global error handler 패턴을 확인한다.
2. 실패 테스트를 먼저 작성한다.
   - domain: DRAFT 온보딩 응답 생성, score 범위 검증, keyword/task key 검증, additional note 정규화.
   - service: 저장 성공, 기존 응답 갱신, 조회 성공, 그룹 없음, 멤버 아님, 응답 없음, invalid keyword, invalid task type, invalid score를 검증한다.
   - controller: 인증 필요, 요청 검증 실패, non-empty availabilitySlots 거절, 저장 응답, 조회 응답을 검증한다.
   - repository: 멤버십 context 조회, response 조회, insert/update SQL, JSON serialization을 검증한다.
3. 테스트 실패를 확인한다.
4. 최소 구현을 추가한다.
   - `onboarding.domain`: `GroupOnboardingResponse`, `GroupOnboardingStatus`, `OnboardingMemberContext`, `TaskPreferenceType`.
   - `onboarding.service`: command/result/exception과 `OnboardingService`.
   - `onboarding.repository`: repository interface, JDBC SQL, JDBC implementation, persistence configuration.
   - `onboarding.controller`: `GET/PUT /api/v1/groups/{groupId}/onboarding/me` endpoint와 request/response DTO.
   - `global.error.ApiExceptionHandler`: onboarding 예외를 ProblemDetail로 매핑.
5. 추가한 focused tests를 통과시킨다.
6. `./gradlew check build --no-daemon`으로 전체 검증을 통과시킨다.
7. 커밋은 `[feat] 온보딩 응답 저장 조회 구현` 형식으로 만든다.
8. `scripts/task/create-pr.sh`로 PR을 생성하고 CodeRabbit review, review gate, manual merge notification까지 진행한다.

## Done Criteria
- 인증된 group member가 `PUT /api/v1/groups/{groupId}/onboarding/me`로 keyword skill levels, task preferences, additional note를 저장하면 `group_onboarding_response`가 `DRAFT` 상태로 생성 또는 갱신된다.
- 인증된 group member가 `GET /api/v1/groups/{groupId}/onboarding/me`로 자신의 저장된 온보딩 응답을 조회할 수 있다.
- 같은 member의 반복 저장은 duplicate row를 만들지 않고 기존 response를 갱신한다.
- provided `keywordSkillLevels` key는 그룹 `detailKeywords` 안에 있어야 하고, provided `taskPreferences` key는 locked task type enum 안에 있어야 하며, 모든 score 값은 1~5여야 한다.
- 그룹 없음, 멤버 아님, 응답 없음, invalid input, non-empty availabilitySlots는 명확한 ProblemDetail 오류로 거절된다.
- availability slot 저장, 온보딩 최종 제출, `group_member.status = ACTIVE` 전환, 그룹 전체 온보딩 완료 판단, 커리큘럼 생성, 알림 생성은 구현하지 않는다.
- domain/service/controller/repository 테스트가 happy path, edge case, input validation을 포함한다.
- `./gradlew check build --no-daemon`이 성공하고 task state에 성공 검증이 기록된다.
- PR에는 Jira `SPT-30` 링크, EXEC_PLAN, 검증 결과가 포함되고 CodeRabbit review marker/review gate 조건을 충족한다.

## Verification Evidence
- `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.*' --no-daemon` -> PASS.
- `./gradlew check build --no-daemon` -> PASS.
- CodeRabbit review `NEEDS_FIX` 5건 대응:
  - 저장소 empty/null 입력 edge test 추가.
  - keyword score 상한 검증 test 추가.
  - `OnboardingMemberContext` null guard 정리.
  - draft 저장 후 재조회 대신 `OnboardingRepository.saveDraft` 반환값 사용.
  - service fake repository가 group/user/member ID를 검증하도록 보강.
- Review fix 후 `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.*' --no-daemon` -> PASS.
- Review fix 후 `./gradlew check build --no-daemon` -> PASS.
