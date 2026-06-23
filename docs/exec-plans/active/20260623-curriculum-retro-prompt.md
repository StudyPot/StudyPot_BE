# EXEC_PLAN: [feat] 커리큘럼 생성 시 주차별 회고 프롬프트 생성/저장

- Task slug: `curriculum-retro-prompt`
- Base branch: `develop`
- Feature branch: `codex/curriculum-retro-prompt`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/curriculum-retro-prompt`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/curriculum-retro-prompt`
- Jira issue: `SPT-147`
- Jira URL: https://studypot.atlassian.net/browse/SPT-147
- Jira summary: [feat] 커리큘럼 생성 시 주차별 회고 프롬프트 생성/저장
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
- db-schema-v1.sql: db-schema-coverage 게이트는 db-schema-v1.sql 과 V1 baseline 마이그레이션의 정확 일치만 검사한다. 증분 마이그레이션(V2~)은 doc 에 반영하지 않으므로(V2~V6 도 미반영), 신규 컬럼은 V7 마이그레이션으로만 추가하고 db-schema-v1.sql/V1 은 건드리지 않는다(게이트 유지).
- ai-contract-v1: 커리큘럼 생성 AI 스키마에 retrospectivePrompt 를 추가(strict json_schema 라 properties+required 동시 추가). 회고 '피드백'이 아니라 회고 작성용 '질문 프롬프트'다. LLM 사용 감사(llm_usage)는 기존대로.

## Goal
커리큘럼 생성 시 AI가 주차별 TODO/목표를 바탕으로 회고 유도 질문(retrospectivePrompt)을 생성해 저장하고, 주차 조회 응답에 노출한다. (사용자 결정: 주차에 회고 프롬프트 컬럼 추가)

## Approach
- Flyway V7: `alter table curriculum_week add column retrospective_prompt text null`.
- 커리큘럼 생성 AI(ProviderBackedCurriculumGenerator): week json_schema 의 properties+required 에 retrospectivePrompt 추가, INSTRUCTIONS 에 주차별 회고 질문 생성 지시. GeneratedWeek/toWeekPlan 파싱 추가.
- 도메인: CurriculumWeekPlan, CurriculumWeek 에 nullable retrospectivePrompt 추가(blank→null). CurriculumGeneration.toCurriculum 전달.
- 저장/조회: INSERT_CURRICULUM_WEEK + 3개 week SELECT 컬럼 목록 + insertWeek/mapWeekRow/CurriculumWeekRow/toWeek.
- 응답: CurriculumWeekResponse 에 retrospectivePrompt 노출.
- 테스트: 생성기 스키마 required 단언 갱신, 8개 CurriculumWeek/Plan 생성자 호출부에 인자 추가.

## Step Plan
1. V7 마이그레이션.
2. 도메인 레코드 2종 + CurriculumGeneration 빌드.
3. 생성기 스키마/프롬프트/파싱.
4. SQL(INSERT/SELECT) + repo(insert/map/row/toWeek).
5. week 응답.
6. 테스트 갱신.
7. `./gradlew check build` 그린.

## Done Criteria
- 커리큘럼 생성 시 주차별 retrospectivePrompt 가 생성·저장되고 주차 조회 응답에 내려간다(없으면 null).
- 마이그레이션 V7 만 추가, db-schema-coverage 게이트 유지.
- 전체 테스트 통과, `./gradlew check build` 그린.
