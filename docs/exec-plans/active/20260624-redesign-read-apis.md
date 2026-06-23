# EXEC_PLAN: [BE] FE 리디자인 지원 — 주차 단건 조회 API

- Task slug: `redesign-read-apis`
- Feature branch: `codex/redesign-read-apis`
- Jira: SPT-157 — https://studypot.atlassian.net/browse/SPT-157
- Status: in-progress

## Goal
FE Todo 화면의 주차 네비게이션(이전/다음 주차 이동)이 현재 주차가 아닌 주차를 열 때 사용하는 `GET /weeks/{weekId}` 를 백엔드에 추가한다. (현재는 미존재 → 비현재 주차 404)

## Scope (이번 PR)
- `GET /api/v1/weeks/{weekId}` → CurriculumWeekResponse. 멤버 접근제어(findReadContextByWeekId), 미존재 404.
- CurriculumRepository.findWeekById + JdbcCurriculumRepository 구현(SELECT_WEEK_BY_ID 재사용).
- CurriculumService.getWeek(GetWeekByIdQuery).
- 테스트: 서비스 3종(멤버 조회/비멤버 거부/미존재), 테스트 페이크 보강.

## 후속(별도 PR)
- GET /groups/summary, StudyGroupResponse memberCount/progressPercent, GET·PATCH /groups/{id}/ai-manager.
- docs/fe-redesign-api-audit.md 참고.

## 검증
- ./gradlew check build 통과(BUILD SUCCESSFUL).
