# EXEC_PLAN: [feat] 그룹 내 내 회고 전체 조회 API

- Task slug: `my-retrospectives-list`
- Base branch: `develop`
- Feature branch: `codex/my-retrospectives-list`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/my-retrospectives-list`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/my-retrospectives-list`
- Jira issue: `SPT-145`
- Jira URL: https://studypot.atlassian.net/browse/SPT-145
- Jira summary: [feat] 그룹 내 내 회고 전체 조회 API (리뷰=회고)
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/notification-contract-v1.md

## Related Feature IDs
- [x] retrospective

## Doc Notes
- api-contract-v1: 기존 회고 응답 스키마(RetrospectiveResponse)를 그대로 재사용한다. 신규 조회 엔드포인트는 주차별 회고를 모아 리스트로 반환하며 계약 변경 없음.
- 회고 도메인 규약: 활성 멤버만 회고를 읽을 수 있다(기존 getMyRetrospective 와 동일한 접근 규칙을 그룹 단위로 적용).

## Goal
"리뷰=회고" 화면을 위해, 활성 멤버가 그룹 내 자신의 모든 주차 회고를 최신 주차 순으로 한 번에 조회하는 API 를 추가한다. (이번 주 회고 리뷰 + 지난 주차 회고들 조회)

## Approach
- `GET /groups/{groupId}/retrospectives/me` 추가.
- 그룹 단위 멤버십 조회 `SELECT_GROUP_MEMBERSHIP` + 내 회고 목록 `SELECT_MY_RETROSPECTIVES_BY_GROUP`(retrospective→curriculum_week→curriculum 조인, member 필터, week_number desc 정렬) 추가.
- repo `findMembershipByGroupId`/`findMyRetrospectivesByGroup`, service `listMyRetrospectives`(활성 멤버 검증 후 목록 반환). 매퍼/응답은 기존 mapRetrospective/RetrospectiveResponse 재사용.

## Step Plan
1. SQL 2종 추가(SELECT_GROUP_MEMBERSHIP, SELECT_MY_RETROSPECTIVES_BY_GROUP).
2. repo 인터페이스/Jdbc 구현에 메서드 2종 추가.
3. ListMyRetrospectivesQuery + RetrospectiveService.listMyRetrospectives.
4. 컨트롤러 GET /groups/{groupId}/retrospectives/me.
5. 테스트: 서비스(목록/비멤버 거부), 컨트롤러/서비스 fake repo 메서드 보강.
6. `./gradlew check build` 그린.

## Done Criteria
- 활성 멤버가 `GET /groups/{groupId}/retrospectives/me` 로 자신의 모든 주차 회고를 최신 주차 순으로 받는다.
- 비활성/비멤버는 403.
- 기존 회고 테스트 + 신규 테스트 통과.
- `./gradlew check build` 그린.
