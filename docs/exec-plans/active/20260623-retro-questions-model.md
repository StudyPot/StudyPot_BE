# EXEC_PLAN: [feat] 회고 질문 세트(리커트+자유서술) 모델 + 커리큘럼 생성

- Task slug: `retro-questions-model`
- Base branch: `develop`
- Feature branch: `codex/retro-questions-model`
- Jira issue: `SPT-154`
- Jira URL: https://studypot.atlassian.net/browse/SPT-154
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/db-schema-v1.sql

## Related Feature IDs
- [x] curriculum

## Doc Notes
- db-schema: V8 로 curriculum_week.retrospective_questions(JSON) 추가. V1==doc 게이트는 증분 마이그레이션 무관(V2~V8 미반영). V7 retrospective_prompt 컬럼은 미사용으로 남김.
- ai-contract: 커리큘럼/다음주차 생성 스키마의 회고 산출물을 자유텍스트 → 질문 배열({text,type:LIKERT_5|TEXT})로 교체. 목적 enum(CURRICULUM_GENERATE/NEXT_WEEK_ADJUST) 불변.

## Goal
회고 가이드를 자유 텍스트(retrospective_prompt)에서 구조화된 회고 질문 세트(리커트 5~6 + 자유서술 1~2)로 교체하고, 커리큘럼/다음주차 생성 AI가 이를 산출·저장하도록 한다. (기획: 회고 = 설문)

## Approach
- 도메인: RetrospectiveQuestion{id,text,type}, RetrospectiveQuestionType{LIKERT_5,TEXT}. CurriculumWeek/CurriculumWeekPlan 의 retrospectivePrompt(String) → retrospectiveQuestions(List).
- 저장: V8 retrospective_questions(JSON). INSERT/SELECT/mapWeekRow/insertWeek/replaceNextWeekTasks(시그니처 List) JSON 매핑.
- 생성기: ProviderBackedCurriculumGenerator/NextWeekPlanGenerator weekSchema·INSTRUCTIONS·파싱을 질문 배열로. 파싱 시 id=q1.. 부여.
- 응답: CurriculumWeekResponse.retrospectiveQuestions.
- 테스트: 생성기 스키마/파싱, repo INSERT, 컨트롤러/서비스 생성자 호출부, NextWeekPlanService 매처.

## Done Criteria
- 커리큘럼 생성 시 주차별 회고 질문 세트가 생성·저장되고 주차 응답에 내려간다.
- 다음주차 자동 재생성도 질문 세트를 산출한다.
- V8만 추가, 전체 테스트 통과, `./gradlew check build` 그린.
