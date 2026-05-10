# EXEC_PLAN: [study-group] study_group 생성/초대 코드 구현

- Task slug: `spt-28-study-group-study-group`
- Base branch: `develop`
- Feature branch: `codex/spt-28-study-group-study-group`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-28-study-group-study-group`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-28-study-group-study-group`
- Jira issue: `SPT-28`
- Jira URL: https://studypot.atlassian.net/browse/SPT-28
- Jira summary: [study-group] study_group 생성/초대 코드 구현
- Status: `active`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/domain-erd.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/architecture/backend-map.md

## Related Feature IDs
- [x] study-group-core

## Doc Notes
- `study-group-core` covers `REQ-GRP-001`, `REQ-GRP-002`, and `REQ-INV-001`.
- `POST /api/v1/groups` is the SPT-28 API surface. `GET /groups`, `GET/PATCH /groups/{groupId}`, `POST /groups/{groupId}/join`, and `GET /groups/{groupId}/members` remain follow-up work unless needed for test setup.
- Create group request fields are locked by OpenAPI: `name`, `topic`, `detailKeywords`, `maxMembers`, `startsAt`, `endsAt`, plus optional `description`.
- Created groups must persist `study_group.status = ONBOARDING`, `onboarding_started_at = now`, and a unique `invite_code`.
- Creator must be persisted as owner `group_member.permission = OWNER` and `group_member.status = PENDING_ONBOARDING`.
- DB schema already stores invite code on `study_group`; a separate invitation table is explicitly deferred.
- Permission docs require authentication for group creation. Bearer token and access-token cookie authentication are already handled by the global security layer.
- QA coverage for this task should include group creation validation and owner member creation. Invite join limits and owner-only updates are locked requirements but belong to later endpoints.

## Goal
SPT-28의 목표는 MVP golden path의 첫 제품 흐름인 `study-group-core`를 시작할 수 있도록, 인증된 사용자가 스터디 그룹을 생성하면 `study_group`과 owner `group_member`가 일관된 초기 상태로 저장되고, 공유 가능한 고유 초대 코드가 발급되는 백엔드 수직 흐름을 완성하는 것이다.

구현 범위는 그룹 생성에 필요한 입력 검증, 도메인 불변식, 저장소, 서비스, API 응답, 테스트까지 포함한다. 생성된 그룹은 `ONBOARDING` 상태여야 하며, 생성자는 `OWNER` 권한과 `PENDING_ONBOARDING` 상태의 멤버로 등록되어야 한다. 초대 코드는 중복 없이 발급되어 이후 초대/가입 흐름에서 사용할 수 있어야 한다.

이 작업은 전체 StudyPot 도메인을 미리 만드는 것이 아니라, 그룹 생성과 초대 코드 발급에 필요한 최소 도메인 조각을 검증 가능한 형태로 구현한다. 멤버 가입/권한 상세, 온보딩 응답, 커리큘럼 생성, 그룹 규칙, 알림, 외부 채널 연동은 후속 Jira 작업 범위로 남긴다.

## Approach
- Follow the existing domain-oriented layered package shape under `com.studypot.aistudyleader.studygroup`: `domain`, `service`, `repository`, and `controller`.
- Keep domain classes framework-free and enforce core invariants there: required name/topic/detail keywords/max members/period, `endsAt >= startsAt`, creation status `ONBOARDING`, owner permission/status.
- Introduce a small invite-code generator port in the service layer so service tests can force retry behavior without binding domain code to randomness.
- Persist `study_group` and owner `group_member` in one transactional service call. Use the existing JDBC style, `UuidBinary`, and JSON serialization through Jackson's `ObjectMapper`.
- Handle uniqueness conflicts conservatively: retry invite-code collisions a bounded number of times, and surface a service-unavailable style failure if all generated codes collide.
- Implement only `POST /api/v1/groups` for this task. Return the locked `StudyGroupResponse` shape from OpenAPI.
- Use TDD: add failing domain/service/controller/repository tests before implementation, watch each fail, then add the smallest production code needed.

## Step Plan
1. Add domain tests for valid group creation, blank/oversized fields, empty detail keywords, invalid max members, and invalid date range.
2. Implement `StudyGroup`, `StudyGroupStatus`, `GroupMember`, `GroupMemberPermission`, and `GroupMemberStatus` with factory/rehydration methods and framework-free validation.
3. Add service tests for authenticated group creation: generated UUIDs are used, invite code is generated, group starts in `ONBOARDING`, owner member is `OWNER`/`PENDING_ONBOARDING`, and repeated invite collisions retry.
4. Implement command/result records, repository port, invite-code generator port, and `StudyGroupService`.
5. Add JDBC repository tests with mocked `JdbcTemplate` proving SQL arguments, JSON detail keyword serialization, owner insert, and collision translation.
6. Implement `StudyGroupJdbcSql`, `JdbcStudyGroupRepository`, and persistence configuration.
7. Add controller tests for `POST /api/v1/groups`: unauthenticated rejection, validation errors, and authenticated `201 Created` response matching OpenAPI fields.
8. Implement `StudyGroupController` with Jakarta validation DTOs and `@AuthenticationPrincipal Jwt` user extraction consistent with `AuthController`.
9. Add/adjust application wiring tests only if Spring context requires explicit beans.
10. Run focused tests after each red/green slice, then run `./gradlew check build --no-daemon`.
11. Commit with `[feat] 스터디 그룹 생성 흐름 구현`, create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, address one feedback loop if needed, and finish readiness notification through repo scripts.

## Done Criteria
- `POST /api/v1/groups` requires authentication and accepts the locked `CreateGroupRequest` shape.
- Successful creation returns `201 Created` and a `StudyGroupResponse` containing UUID, name, topic, detail keywords, status `ONBOARDING`, max members, invite code, startsAt, and endsAt.
- `study_group` is persisted with a unique invite code, creator user id, `ONBOARDING` status, valid period, and `onboarding_started_at`.
- Creator is persisted in `group_member` with `OWNER` permission and `PENDING_ONBOARDING` status.
- Validation failures produce RFC problem details through the existing validation handler.
- Tests cover happy path, edge cases, input validation, invite-code collision retry, controller authentication, and repository SQL/JSON mapping.
- `./gradlew check build --no-daemon` passes and task state records the successful verification.
- PR is created against `develop`; CodeRabbit review marker and GitHub Actions Review Gate pass before manual merge notification.

## Implementation Notes
- Added the `studygroup` bounded context with domain, service, repository, and controller layers.
- Implemented `POST /api/v1/groups` only; list/read/update/join/member-list endpoints remain follow-up SPT work.
- Added bounded invite-code collision retries in `StudyGroupService`.
- Kept invite data on `study_group.invite_code` per locked DB contract; no invitation table was introduced.

## Verification
- `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.*' --no-daemon` passed.
- `./gradlew check build --no-daemon` passed on 2026-05-09 before commit.
