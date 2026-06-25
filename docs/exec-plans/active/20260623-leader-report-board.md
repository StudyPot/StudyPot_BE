# EXEC_PLAN: [feat] 팀장 리포트 보드 + 답변 기반 주차 리포트(전원 알림)

- Task slug: `leader-report-board`
- Base branch: `develop`
- Feature branch: `codex/leader-report-board`
- Jira issue: `SPT-156`
- Jira URL: https://studypot.atlassian.net/browse/SPT-156
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Feature IDs
- [x] studygroup (board)
- [x] notification
- [x] report

## Doc Notes
- 마이그레이션 없음(보드 타입 enum 추가, 기존 그룹은 find-or-create 로 보드 생성). 알림 발행은 in-process/Queued 양쪽 구현.

## Goal
주차 리포트를 전용 "팀장 리포트" 게시판에 게시하고 전원 알림. 멤버는 이 보드에 작성 불가. 리포트 회고 집계를 설문 답변 기반으로 변경.

## Approach
- GroupBoardType.LEADER_REPORT("팀장 리포트") 추가. createPost 에서 LEADER_REPORT 는 owner만 허용. defaultBoards 자동 포함, 기존 그룹은 GroupBoardService.findOrCreateBoardId 로 생성.
- 알림: NotificationType.LEADER_REPORT_POSTED + publishLeaderReportPosted(전원, 작성자 제외 없음) in-process/Queued + factory. createPost 가 LEADER_REPORT 게시 시 발행.
- WeeklyReportScheduler: 게시 대상 RETROSPECTIVE → LEADER_REPORT(find-or-create). 회고 집계 SELECT 를 ai_feedback.summary → input_summary.answers(설문)로 변경.
- 테스트: 보드 owner전용/멤버거부/알림/ find-or-create, defaultBoards 5종, 알림 페이크 보강.

## Done Criteria
- 리포트가 팀장 리포트 보드에 게시되고 전원 알림. 멤버 작성 거부(403). 전체 테스트 통과.
