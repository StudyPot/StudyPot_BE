# EXEC_PLAN: [feat] 리포트 기반 다음 주차 TODO/회고 프롬프트 재생성

- Task slug: `next-week-plan`
- Base branch: `develop`
- Feature branch: `codex/next-week-plan`
- Jira issue: `SPT-148`
- Jira URL: https://studypot.atlassian.net/browse/SPT-148
- Jira summary: [feat] 리포트 기반 다음 주차 TODO/회고 프롬프트 재생성
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
- db-schema-v1.sql: 신규 마이그레이션 없음. 직전 작업(SPT-147)에서 추가한 V7 `curriculum_week.retrospective_prompt` 컬럼을 재사용하고, weekly_task soft-delete(deleted_at) 패턴도 기존 스키마 그대로 사용한다. 따라서 db-schema-coverage 게이트 영향 없음.
- ai-contract-v1: 다음 주차 계획 생성은 LlmUsagePurpose.NEXT_WEEK_ADJUST 재사용. json_schema(tasks[],retrospectivePrompt). OpenAiOutputTokenLimits 의 NEXT_WEEK_ADJUST case 는 이미 존재. 비밀키류는 LlmPromptSanitizer 로 차단.

## Goal
그룹장이 직전 주차의 학습 리포트(게시판 글)를 기반으로 '다음 주차'의 TODO 목록과 회고 프롬프트를 AI로 재생성한다. (사용자 결정: 회고 프롬프트 컬럼 추가 + 리포트 기반 다음 주차 생성)

## Approach
- 안전성: 자동 스케줄러가 아니라 **그룹장 트리거 엔드포인트**로 구현(잘못된 AI 결과로 실제 커리큘럼 task 가 유실되는 위험 최소화). 트랜잭션 내 교체 + 빈 결과 거부.
- AI: ProviderBackedNextWeekPlanGenerator (NEXT_WEEK_ADJUST). 입력=다음 주차 정보+직전 주차 리포트 본문, 출력=tasks[]+retrospectivePrompt.
- 저장소: findNextPendingWeek(현재 weekId→다음 PENDING 주차), findLatestWeeklyReportBody(group_board_post 의 '...주차 학습 리포트' 최신 본문), replaceNextWeekTasks(기존 task soft-delete→신규 insert→retrospective_prompt 갱신, 단일 주차 재조회 반환).
- 서비스: NextWeekPlanService.regenerateNextWeek — 오너 검사(findReadContextByWeekId), 생성, LLM usage 기록, 트랜잭션 교체.
- 컨트롤러: POST /groups/{groupId}/weeks/{weekId}/next-week-plan → CurriculumWeekResponse.

## Step Plan
1. 생성기(레코드/인터페이스/예외/Provider 구현).
2. NextWeekTarget 도메인 + 저장소 SQL/메서드 3종(+단일 주차 조회).
3. NextWeekPlanService + RegenerateNextWeekCommand.
4. 빈 와이어링(생성기 conditional + 서비스).
5. 컨트롤러 엔드포인트.
6. 테스트(생성기 파싱/빈거부, 서비스 happy/non-owner, 페이크 보강).
7. `./gradlew check build` 그린.

## Done Criteria
- 그룹장이 엔드포인트 호출 시 다음 주차 task 가 리포트 기반으로 교체되고 retrospective_prompt 가 갱신된 주차가 반환된다.
- 비오너/다음주차 없음/리포트 없음 → 403/404. AI 빈 결과 거부.
- 신규 마이그레이션 없음, 전체 테스트 통과, `./gradlew check build` 그린.
