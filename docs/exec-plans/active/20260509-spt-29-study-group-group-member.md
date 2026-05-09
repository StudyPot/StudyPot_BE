# EXEC_PLAN: [study-group] group_member 가입/권한/상태 구현

- Task slug: `spt-29-study-group-group-member`
- Base branch: `develop`
- Feature branch: `codex/spt-29-study-group-group-member`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-29-study-group-group-member`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-29-study-group-group-member`
- Jira issue: `SPT-29`
- Jira URL: https://studypot.atlassian.net/browse/SPT-29
- Jira summary: [study-group] group_member 가입/권한/상태 구현
- Status: `implemented`

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
- [x] study-group-core

## Doc Notes
- `study-group-core` covers group creation, invite code, membership, and status. SPT-29 continues the SPT-28 group creation path by using `study_group.invite_code` to create the non-owner `group_member` row.
- `REQ-INV-001` requires invite link/code sharing and pending member records. `QA-GRP-003` specifically requires max member count and duplicate membership rules.
- The locked API already defines `POST /api/v1/groups/{groupId}/join` with `JoinGroupRequest.inviteCode` and `GroupMemberResponse`.
- Group status validation is scoped to the MVP onboarding join path: only `ONBOARDING` groups accept this SPT-29 join flow. Late joiner handling for already active groups remains deferred per the user goal.
- Capacity is counted with live `PENDING_ONBOARDING` and `ACTIVE` members so the owner created by SPT-28 is included in `maxMembers`.
- Duplicate joining is rejected before insert for existing live `PENDING_ONBOARDING` or `ACTIVE` membership and also guarded by the DB live unique key for race conditions.
- The service locks the target `study_group` row during join validation so capacity check and member insert are serialized per group.

## Goal
SPT-29의 목표는 SPT-28에서 생성된 `study_group.invite_code`를 사용해 인증된 사용자가 스터디 그룹에 `MEMBER` 권한과 `PENDING_ONBOARDING` 상태로 가입하는 백엔드 수직 흐름을 완성하는 것이다. 구현 범위는 초대 코드 검증, 그룹 존재/상태 검증, 정원 제한 검증, 중복 가입 방지, 멤버 생성, 저장소, 서비스, API 응답, 테스트까지 포함한다.

## Approach
- 기존 `studygroup` bounded context의 계층 구조를 유지한다: controller는 DTO/인증 주체 추출, service는 가입 유스케이스와 불변식, domain은 `GroupMember` 생성 규칙과 join 대상 검증, repository는 JDBC 조회/저장을 담당한다.
- TDD로 먼저 domain/service/controller/repository 테스트를 추가해 실패를 확인한 뒤 구현한다.
- API는 locked OpenAPI 경로인 `POST /api/v1/groups/{groupId}/join`을 구현하고 성공 시 `GroupMemberResponse`를 반환한다.
- 거절 케이스는 ProblemDetail 형식으로 명확히 응답한다: 그룹 없음은 not-found, 초대 코드 불일치/비가입 가능 상태/정원 초과/중복 가입은 conflict 계열 문제로 표현한다.

## Step Plan
1. 현재 SPT-28 코드 표면을 기준으로 `GroupMember.member(...)`, join target 값 객체, service command/result/exception, repository port 메서드, controller request/response 추가 지점을 확인한다.
2. 실패 테스트를 먼저 작성한다.
   - domain: 일반 멤버 생성 시 `MEMBER`/`PENDING_ONBOARDING`과 표시명 정규화가 적용된다.
   - service: 정상 가입, 그룹 없음, 초대 코드 불일치, 비가입 가능 그룹 상태, 정원 초과, 중복 가입을 검증한다.
   - controller: 인증 필요, 요청 검증 실패, 가입 성공 응답, 주요 거절 ProblemDetail을 검증한다.
   - repository: join 대상 조회, 현재 멤버 수 조회, 기존 멤버 조회, 멤버 insert, duplicate key 변환을 검증한다.
3. 실패 테스트가 예상한 이유로 실패하는지 확인한다.
4. 최소 구현을 추가한다.
   - `GroupMember.member(...)` factory를 추가한다.
   - `StudyGroupJoinTarget` 또는 동등한 도메인 값 객체로 invite/status/capacity 검증을 캡슐화한다.
   - `JoinStudyGroupCommand`, `StudyGroupJoinResult`, join rejection exceptions를 추가한다.
   - `StudyGroupService.joinGroup(...)`에서 존재, 상태, 초대 코드, 정원, 중복 가입을 순서대로 검증하고 새 멤버를 저장한다.
   - `StudyGroupRepository`와 JDBC 구현에 join 조회/저장 메서드를 추가한다.
   - `StudyGroupController`에 join endpoint와 `GroupMemberResponse`를 추가한다.
   - `ApiExceptionHandler`/`ProblemDetailFactory`에 join 거절 응답을 연결한다.
5. 추가한 단위/슬라이스 테스트를 통과시킨다.
6. `./gradlew check build --no-daemon`으로 전체 검증을 통과시킨다.
7. 커밋은 `[feat] 스터디 그룹 가입 흐름 구현` 형식으로 만든다.
8. `scripts/task/create-pr.sh`로 PR을 만들고, `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`로 CodeRabbit agent review를 요청한다.
9. CodeRabbit 이슈가 있으면 한 번 수정, 재검증, addressed evidence 게시 후 PR ready gate까지 진행한다.

## Done Criteria
- 인증된 사용자가 올바른 `inviteCode`로 `POST /api/v1/groups/{groupId}/join`을 호출하면 `group_member`가 저장되고 응답에 `id`, `groupId`, `userId`, `permission=MEMBER`, `status=PENDING_ONBOARDING`, `displayName`이 포함된다.
- 존재하지 않는 그룹, 초대 코드 불일치, `ONBOARDING`이 아닌 그룹, 정원 초과, 기존 활성/온보딩 멤버 중복 가입이 명확한 ProblemDetail 오류로 거절된다.
- 정원 검증은 owner를 포함한 현재 `PENDING_ONBOARDING`/`ACTIVE` 멤버 수가 `study_group.max_members` 이상이면 거절한다.
- SPT-29 범위를 넘어서는 온보딩 응답 저장, availability slot 저장, onboarding submit 후 ACTIVE 전환, 커리큘럼 생성, late joiner 처리, 알림 생성은 구현하지 않는다.
- domain/service/controller/repository 테스트가 happy path, edge case, input validation을 포함한다.
- `./gradlew check build --no-daemon`이 성공하고 task state에 성공 검증이 기록된다.
- PR에는 Jira `SPT-29` 링크, EXEC_PLAN, 검증 결과가 포함되고 CodeRabbit review marker/review gate 조건을 충족한다.
