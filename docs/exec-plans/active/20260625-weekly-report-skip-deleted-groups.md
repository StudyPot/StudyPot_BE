# EXEC_PLAN: [fix] 주차/수료 리포트 스케줄러 삭제 그룹 스킵 (WARN 노이즈 제거)

- Task slug: `weekly-report-skip-deleted-groups`
- Base branch: `develop`
- Feature branch: `codex/weekly-report-skip-deleted-groups`
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/exec-plans/active/20260623-weekly-report-scheduler.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
20260623-weekly-report-scheduler.md 의 주차 리포트 스케줄러 설계(마감 +30분 유예,
7일 lookback, 제목 기준 멱등, LEADER_REPORT 게시판 자동 게시)를 그대로 유지한다.
이번 변경은 그 위에 "삭제된 그룹 제외 + 레이스 시 그룹별 스킵"만 얹는 버그픽스.

## Goal
WeeklyReportScheduler 가 이미 삭제(soft-delete)된 스터디 그룹에도 주차/수료 리포트를
게시하려다 GroupBoardNotFoundException("study group was not found",
GroupBoardService.requireActiveMembership)을 던져, 매 기동/스케줄 주기마다 WARN 이
다발(prod 기동 직후 ~90건)로 찍히는 노이즈를 제거한다. AI 채팅 MQ 비동기화(PR #302)와
무관한 기존 이슈.

## Approach
1. 대상 선정 단계에서 살아있는 그룹만 조회: SELECT_DUE_REPORT_WEEKS 쿼리에
   `join study_group sg on sg.id = c.group_id` + `and sg.deleted_at is null` 추가.
   즉시 트리거 쿼리(SELECT_WEEK_FOR_REPORT_BY_ID)·수료 리포트 쿼리는 이미 동일 조건으로
   필터링되어 있어, 누락돼 있던 주기 스케줄 쿼리만 맞춘다.
2. 레이스(대상 선정 후 삭제) 방어: 리포트 게시 경로(주차 스케줄/수료 스케줄/즉시 주차/
   즉시 수료 4곳)에서 GroupBoardNotFoundException 을 그룹별로 catch 하여 해당 그룹만
   DEBUG 로그로 건너뛰고 루프는 계속한다. 기존 RuntimeException WARN catch 는 다른 실패용
   으로 유지(노이즈/불필요 작업만 줄이고 다른 오류 가시성은 보존).

## Step Plan
1. SELECT_DUE_REPORT_WEEKS 에 study_group 조인/삭제 필터 추가, 테스트 접근용으로
   상수·DueWeek 레코드 패키지 가시성으로 전환.
2. GroupBoardNotFoundException import + 4개 경로에 DEBUG 스킵 catch 추가.
3. WeeklyReportSchedulerTest 작성: (a) 쿼리에 삭제 그룹 필터 포함 확인, (b) 삭제 그룹
   혼재 시 살아있는 그룹만 게시되고 WARN 없이 스킵되는지 ListAppender 로 검증.
4. ./gradlew check build 통과 후 develop 대상 PR.

## Done Criteria
- SELECT_DUE_REPORT_WEEKS 가 삭제된 그룹을 대상에서 제외한다.
- 게시 시 GroupBoardNotFoundException 이 그룹별로 스킵되어 나머지 그룹 처리가 이어지고,
  WARN 대신 DEBUG 로 남는다.
- WeeklyReportSchedulerTest 가 위 두 동작을 재현하며 통과.
- ./gradlew check build 통과.
