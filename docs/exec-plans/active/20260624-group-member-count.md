# EXEC_PLAN: [BE] 그룹 응답 memberCount

- Branch: codex/group-member-count · Jira: SPT-158 · Status: in-progress

## Goal
FE 전체 그룹 카드의 "멤버 N/M" 표기를 실데이터로. StudyGroupResponse 에 현재 참여(활성/온보딩) 멤버 수(memberCount)를 추가.

## 변경
- StudyGroupResponse += memberCount; from(group, memberCount).
- StudyGroupService.countActiveMembers(groupId) (기존 repo.countActiveOrOnboardingMembers 재사용 — 신규 인터페이스/페이크 변경 없음).
- 컨트롤러 list/get/create/update 4개 매핑에서 memberCount 주입.

## 검증
- ./gradlew check build BUILD SUCCESSFUL.
