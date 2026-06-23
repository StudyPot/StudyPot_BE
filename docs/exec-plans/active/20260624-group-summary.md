# EXEC_PLAN: [BE] 내 그룹 요약 API (GET /groups/summary)

- Branch: codex/group-summary · Jira: SPT-160 · Status: in-progress

## Goal
전체 그룹 화면 상단 부제목 "N개 그룹 · 이번 주 활동 N회" 실데이터. FE getGroupSummary(현재 404→fallback) 실연동.

## 변경
- StudyGroupService.countMyGroups(userId) — 기존 findGroupsByMemberUserId 재사용(신규 repo 없음).
- CurriculumRepository.countMemberDoneActivity(userId,from,to) + SQL(task_completion join group_member) + Jdbc.
- CurriculumService.countMyWeeklyDoneActivity(userId) — clock 기준 최근 7일.
- 신규 GroupSummaryController GET /groups/summary → {groupCount, weeklyActivityCount} (ObjectProvider 2종, 503 패턴).
- curriculum 페이크 2종 + 서비스 테스트 2종(window/위임, countMyGroups).
- 라우팅: /groups/summary 리터럴 > /groups/{groupId} 변수.

## 검증
- ./gradlew check build BUILD SUCCESSFUL.
