# EXEC_PLAN: [feat] 회고 설문 제출 + TODO 완료 잠금 + 주차별 개요

- Task slug: `retro-survey-submit`
- Base branch: `develop`
- Feature branch: `codex/retro-survey-submit`
- Jira issue: `SPT-155`
- Jira URL: https://studypot.atlassian.net/browse/SPT-155
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract (회고 엔드포인트)
- [x] docs/specs/db-schema-v1.sql

## Related Feature IDs
- [x] retrospective

## Doc Notes
- 마이그레이션 없음. 답변은 기존 retrospective.input_summary(JSON)의 "answers" 로 저장. db-schema 게이트 무관.
- 회고 = 멤버 설문 제출. 개인 AI 피드백 생성은 제출 경로에서 호출하지 않음(기존 AI 메서드는 컨트롤러 미연결로 보존 — 테스트 유지/블라스트 최소화, 후속 정리 가능).

## Goal
회고를 멤버가 답하는 설문으로 전환. 그 주차 필수 TODO 를 모두 완료해야만(잠금 해제) 제출 가능. 주차별 개요(질문/잠금/작성여부)를 한 번에 조회.

## Approach
- 도메인: Retrospective.answered(...)/withAnswers(...) — COMPLETED + input_summary.answers. RetrospectiveWeekOverview{weekId,weekNumber,status,unlocked,answered,questions}.
- 저장소: updateRetrospectiveAnswers(input_summary/status/completed_at) + findRetrospectiveOverview(curriculum_week + task_completion(필수 DONE 집계) + retrospective 조인, retrospective_questions JSON 매핑).
- 서비스: submitMyRetrospective(게이팅=필수 TODO 전부 DONE, 아니면 409) upsert. getRetrospectiveOverview.
- 컨트롤러: POST /weeks/{weekId}/retrospectives/me 가 answers body 받아 제출(200). GET /groups/{groupId}/retrospectives/overview 신설. 응답에 answers 노출.
- 테스트: 제출 성공/잠금(409), 개요 응답, 서비스 fake 보강.

## Done Criteria
- 필수 TODO 완료 시 회고 제출 저장(COMPLETED), 미완료 시 409. 개요가 주차별 unlocked/answered/questions 반환. 전체 테스트 통과.
