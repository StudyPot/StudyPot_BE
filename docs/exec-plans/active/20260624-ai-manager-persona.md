# EXEC_PLAN: [BE] AI 팀장 퍼소나 API

- Branch: codex/ai-manager-persona · Jira: SPT-159 · Status: in-progress

## Goal
팀원 화면 'AI 팀장' 카드의 성격(퍼소나) 조회/설정. FE getAiManager/updateAiManager(현재 404→graceful) 실연동.

## 변경
- Flyway V9: study_group 에 ai_persona/ai_persona_updated_by(FK users)/ai_persona_updated_at 컬럼.
- AiManagerView 도메인 + StudyGroupRepository.findAiManager/updateAiManager + Jdbc(users join nickname).
- StudyGroupService.getAiManager(멤버 읽기)/updateAiManager(owner 전용) + Get/Update Query·Command.
- StudyGroupController GET·PATCH /groups/{groupId}/ai-manager + AiManagerResponse/UpdateAiManagerRequest(@Size 2000).
- 페이크 2종 + 서비스 테스트 6종(멤버/비멤버/미존재/owner/비owner).

## 검증
- ./gradlew check build BUILD SUCCESSFUL.
