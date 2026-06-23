# EXEC_PLAN: [feat] 회고 응답에 weekId 추가(전 주차 회고 매핑용)

- Task slug: `retro-response-weekid`
- Base branch: `develop`
- Feature branch: `codex/retro-response-weekid`
- Jira issue: `SPT-152`
- Jira URL: https://studypot.atlassian.net/browse/SPT-152
- Jira summary: [feat] 회고 응답에 weekId 추가(전 주차 회고 매핑용)
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract (회고 응답 DTO)

## Related Feature IDs
- [x] retrospective

## Doc Notes
- 스키마/마이그레이션 변경 없음. 컨트롤러 응답 DTO 필드 1개 추가뿐.

## Goal
회고 응답(RetrospectiveResponse)에 weekId(curriculumWeekId)를 추가해, FE가 GET /groups/{groupId}/retrospectives/me 목록을 커리큘럼 주차 목록과 조인하여 'N주차 회고'로 표시할 수 있게 한다. (FE #2 리뷰→회고/전 주차 조회 지원)

## Approach
- RetrospectiveResponse 에 UUID weekId 추가, from() 에서 retrospective.curriculumWeekId() 전달. per-week/list 양쪽 응답에 자동 반영(공통 DTO).
- 테스트: per-week/list 응답에 weekId 단언 추가.

## Done Criteria
- 회고 단건/목록 응답에 weekId 포함. 전체 테스트 통과.
