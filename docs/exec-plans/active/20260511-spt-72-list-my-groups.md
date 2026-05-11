# EXEC_PLAN: [study-group] 내 그룹 목록 조회 API 구현

- Task slug: `spt-72-list-my-groups`
- Base branch: `develop`
- Feature branch: `codex/spt-72-list-my-groups`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-72-list-my-groups`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-72-list-my-groups`
- Jira issue: `SPT-72`
- Jira URL: https://studypot.atlassian.net/browse/SPT-72
- Jira summary: [study-group] 내 그룹 목록 조회 API 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] Jira SPT-72 issue detail
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] study-group-core

## Doc Notes
- `GET /api/v1/groups` is already locked in `api-contract-v1.md` and `openapi.yaml` as `listGroups`, returning `StudyGroupResponse[]`.
- `StudyGroupResponse` does not include member permission/status, member count, curriculum, or nested member data, so this task keeps the response shape unchanged.
- `db-contract-v1.md` calls out the hot query: fetch current user groups by `group_member.user_id`, member status, and group status.
- `auth-permissions-v1.md` requires authentication for group access. This endpoint is "my groups", so it is scoped to the authenticated user's own live membership rows.
- This task has no AI/LLM generation scope and does not touch `llm_usage`.

## Goal
`GET /api/v1/groups`가 인증된 사용자의 현재 스터디 그룹 목록을 반환하게 만든다. 조회 기준은 `group_member.user_id = authenticatedUserId`, `group_member.status in ('PENDING_ONBOARDING', 'ACTIVE')`, 삭제되지 않은 그룹/멤버십이며, 응답은 잠긴 OpenAPI의 `StudyGroupResponse[]` 형태를 따른다.

## Approach
Extend the existing `studygroup` slice without changing the locked API contract.

1. Add service behavior `StudyGroupService.listMyGroups(authenticatedUserId)` that delegates to the repository and performs null validation.
2. Add repository behavior and JDBC SQL to select the authenticated user's live membership groups, ordered by newest membership first for a stable dashboard-friendly default.
3. Add `GET /api/v1/groups` to `StudyGroupController`, reusing the existing authentication extraction and `StudyGroupResponse`.
4. Add a persistence reconstitution factory to `StudyGroup` only as much as needed to map existing rows back into the domain model.
5. Use TDD: write failing service/controller/repository tests first, then implement minimal production code.

## Step Plan
1. Add failing service test for returning only repository-provided current user groups.
2. Add failing controller tests for unauthenticated `GET /groups` and successful authenticated list response.
3. Add failing repository test for the my-groups SQL/argument mapping and a direct SQL assertion for live membership/group filtering.
4. Implement `ListStudyGroupsQuery`, service method, repository interface method, JDBC SQL/mapping, and controller GET handler.
5. Run targeted study-group service/controller/repository tests.
6. Run `./gradlew check build --no-daemon`.
7. Commit, create PR through `scripts/task/create-pr.sh`, run one CodeRabbit review, address once if needed, and prepare manual merge notification through `finish-pr.sh`.

## Done Criteria
- `GET /api/v1/groups` requires authentication.
- Authenticated users receive `200 OK` with a JSON array of `StudyGroupResponse`.
- The repository query uses `group_member.user_id` and includes only live `PENDING_ONBOARDING` or `ACTIVE` memberships.
- Deleted group/member rows are excluded.
- Locked OpenAPI response shape is not changed.
- No AI/LLM, curriculum, member-list, group-detail, pagination, search, or filter scope is added.
- Targeted study-group tests and `./gradlew check build --no-daemon` pass.
- PR/review gate follows the Korean evidence and CodeRabbit marker workflow.

## Implementation Notes
- Added `GET /api/v1/groups` to `StudyGroupController`, returning the existing `StudyGroupResponse[]` shape.
- Added `ListStudyGroupsQuery` and `StudyGroupService.listMyGroups` as a read-only use case.
- Added `StudyGroupRepository.findGroupsByMemberUserId` and JDBC SQL that joins `study_group` with live `group_member` rows for the authenticated user.
- Added `StudyGroup.rehydrate` for repository read mapping without changing creation behavior.
- Kept AI/LLM, curriculum, member list, detail view, pagination, and advanced filtering out of scope.
- CodeRabbit review fix added explicit audit timestamp null validation in `StudyGroup.rehydrate`. Pagination feedback was evaluated but kept out of scope because locked OpenAPI defines `StudyGroupResponse[]` without pagination parameters and SPT-72 explicitly excludes pagination.

## Verification
- [x] RED check: `./gradlew test --tests com.studypot.aistudyleader.studygroup.service.StudyGroupServiceTest --tests com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest --tests com.studypot.aistudyleader.studygroup.repository.JdbcStudyGroupRepositoryTest --no-daemon` failed on missing list API contracts before implementation.
- [x] `./gradlew test --tests com.studypot.aistudyleader.studygroup.service.StudyGroupServiceTest --tests com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest --tests com.studypot.aistudyleader.studygroup.repository.JdbcStudyGroupRepositoryTest --no-daemon`
- [x] `./gradlew check build --no-daemon`
- [x] CodeRabbit review fix verification: `./gradlew test --tests com.studypot.aistudyleader.studygroup.domain.StudyGroupTest --tests com.studypot.aistudyleader.studygroup.service.StudyGroupServiceTest --tests com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest --tests com.studypot.aistudyleader.studygroup.repository.JdbcStudyGroupRepositoryTest --no-daemon`
- [x] CodeRabbit review fix verification: `./gradlew check build --no-daemon`
