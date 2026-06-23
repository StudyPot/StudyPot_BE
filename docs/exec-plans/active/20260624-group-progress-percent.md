# EXEC_PLAN: [BE] 그룹 응답 progressPercent

- Branch: codex/group-progress-percent · Jira: SPT-161 · Status: in-progress

## Goal
전체 그룹 카드 진행바를 실데이터로. StudyGroupResponse 에 커리큘럼 주차 진행도(progressPercent) 추가. FE는 progressPercent 가 있으면 표시, 없으면 상태 기반 fallback.

## 변경
- GroupWeekProgress 도메인 + progressPercent()=(완료+진행*0.5)/전체 반올림(전체0→null).
- CurriculumRepository.findWeekProgressByGroupIds(IN 배치) + SQL(curriculum join curriculum_week) + Jdbc.
- CurriculumService.progressPercentByGroupIds → Map(그룹→%), 미생성 그룹 제외.
- StudyGroupController: ObjectProvider<CurriculumService> 주입, curriculumProgress() graceful(미구성→빈맵). StudyGroupResponse += progressPercent(nullable), 목록 배치/상세 단건 주입(생성=null).
- curriculum 페이크 2종 + 서비스 테스트(계산/0주차 제외).

## 검증
- ./gradlew check build BUILD SUCCESSFUL(@SpringBootTest 포함).
