# EXEC_PLAN: [feat] 주차 리포트 TODO 폴백 + 다음 주차 자동 재생성

- Task slug: `report-fallback-autonext`
- Base branch: `develop`
- Feature branch: `codex/report-fallback-autonext`
- Jira issue: `SPT-151`
- Jira URL: https://studypot.atlassian.net/browse/SPT-151
- Jira summary: [feat] 주차 리포트 TODO 폴백 + 다음 주차 자동 재생성
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md (WEEKLY_REPORT / NEXT_WEEK_ADJUST 사용)
- [x] docs/specs/db-schema-v1.sql (스키마 변경 없음 확인)

## Related Feature IDs
- [x] report
- [x] curriculum

## Doc Notes
- 신규 마이그레이션/컬럼 없음. 기존 task_completion/weekly_task/member_week_progress, curriculum_week.retrospective_prompt(V7) 재사용. db-schema-coverage 영향 없음.
- ai-contract: WEEKLY_REPORT 입력에 memberTaskProgress 추가(스키마 title/body 불변). 다음주 재생성은 NEXT_WEEK_ADJUST 재사용(OpenAiOutputTokenLimits case 존재).

## Goal
(#2) 완료된 회고가 0건이어도 멤버별 TODO 완료현황으로 주차 리포트를 생성한다. (#4) 리포트 게시 직후 그 리포트를 바탕으로 다음 주차 TODO/회고 프롬프트를 자동 재생성한다(그룹장 수동 → 자동).

## Approach
- #2: MemberTaskProgress(memberName/done/total) 추가, WeeklyReportData 에 memberTaskProgress 추가, 생성기 input/INSTRUCTIONS 보강. 스케줄러는 SELECT_MEMBER_TASK_PROGRESS(활성 멤버 LEFT JOIN task_completion DONE 집계) + COUNT_WEEK_TASKS 로 진행도 수집, 빈-회고 스킵을 "회고도 멤버도 없을 때만 스킵"으로 변경, 둘 다 WeeklyReportData 로 전달.
- #4: 스케줄러가 리포트 게시 직후 NextWeekPlanService.regenerateNextWeekAutomatically(groupId, weekId, ownerUserId) 호출(실패해도 게시는 유지). 리포트 게시 멱등 → 자동 재생성도 주차당 1회. 저장소에 findNextRegenerableWeek(다음 주차가 IN_PROGRESS 여도 마감 전이면 대상; 주차 전이 스케줄러와의 경합 대비) 추가. 자동 경로는 오너 인증 검사 없이 시스템 수행, 사용량은 그룹장 귀속.

## Step Plan
1. #2 리포트 입력/생성기/스케줄러.
2. #4 저장소 조회 + 서비스 자동 메서드 + 스케줄러 연결.
3. 테스트(생성기 입력, 자동 재생성 happy/skip 2종, 페이크 보강).
4. `./gradlew check build` 그린.

## Done Criteria
- 회고 0건이어도 TODO 완료현황 기반 리포트가 게시된다.
- 리포트 게시 후 다음 주차 task/회고 프롬프트가 자동 재생성된다(없으면 skip, 실패해도 게시 유지).
- 스키마 변경 없음, 전체 테스트 통과.
