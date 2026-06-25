# EXEC_PLAN: [fix] AI 팀장 자동 리포트가 방장 명의로 게시되는 문제

- Task slug: `fix-leader-report-author`
- Base branch: `develop`
- Feature branch: `codex/fix-leader-report-author`
- Worktree: `/Users/hyunwoo/Developer/Projects/StudyPot`
- Port: `TBD`
- Log dir: `/Users/hyunwoo/Developer/Projects/StudyPot/.codex/logs/fix-leader-report-author`
- Jira issue: ``
- Jira URL:
- Jira summary:
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md
- [x] src/main/java/com/studypot/aistudyleader/report/scheduler/WeeklyReportScheduler.java
- [x] src/main/java/com/studypot/aistudyleader/studygroup/board/service/GroupBoardService.java
- [x] src/main/java/com/studypot/aistudyleader/studygroup/board/service/CreateGroupBoardPostCommand.java

## Related Docs
- [x] docs/specs/notification-contract-v1.md (리포트 게시 시 `LEADER_REPORT_POSTED` 알림 발행 — 본 변경은 알림 계약을 바꾸지 않음 확인)
- [x] src/main/java/com/studypot/aistudyleader/studygroup/board/repository/GroupBoardJdbcSql.java (작성자명 산출 SQL)
- [x] src/test/java/com/studypot/aistudyleader/report/scheduler/WeeklyReportSchedulerTest.java

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- AGENTS.md: 버그 수정은 재현/검증 테스트를 포함해야 하며 `[type] 한글 내용` 커밋 규칙을 따른다. 본 작업은 DDL/계약 변경이 없고 기존 컬럼(`group_board_post.author_display_name_override`)을 의도된 용도로 채우는 애플리케이션 버그 수정이라 LOCKED 스펙 변경에 해당하지 않음.
- GroupBoardJdbcSql: 게시글 작성자명은 `coalesce(p.author_display_name_override, nullif(gm.display_name,''), u.nickname)`로 산출되어 `author_display_name`으로 응답에 내려간다. 따라서 override만 채우면 FE 변경 없이 표시명이 바뀐다.

## Goal
AI가 자동 게시하는 주차/수료 리포트가 게시판 상세에서 작성자가 방장(OWNER) 이름으로 표시되는 버그를 고쳐, "AI 팀장" 명의로 보이게 한다.

## Approach
- 근본 원인: `WeeklyReportScheduler`가 리포트를 게시할 때 `boardService.createPost(...)`를 `authorDisplayNameOverride` 인자 없이(6-인자 호환 생성자) 호출 → `author_display_name_override`가 NULL → 작성자명 산출 coalesce에서 방장 멤버명으로 떨어짐.
- `GroupBoardService.createPost`는 override가 주어지면 그 값을 표시명/override 컬럼에 반영하도록 이미 구현돼 있음. 즉 호출부만 수정하면 됨.
- 상수 `AI_LEADER_DISPLAY_NAME = "AI 팀장"`를 추가하고, 주차 리포트(`postWeeklyReport`)·수료 리포트(`generateCompletionReport`)의 두 createPost 호출에 7-인자 생성자로 전달.

## Step Plan
1. 작성자명 산출 경로(GroupBoardJdbcSql) 및 override 미설정 호출부(WeeklyReportScheduler) 확인.
2. `AI_LEADER_DISPLAY_NAME` 상수 추가, createPost 2곳에 override 전달.
3. `WeeklyReportSchedulerTest`에 게시 커맨드 캡처 후 `authorDisplayNameOverride == "AI 팀장"` 단언 추가(회귀 방지).
4. `./gradlew check build` 통과 후 PR → develop.

## Done Criteria
- `./gradlew check build` 통과.
- 자동 생성되는 주차·수료 리포트가 `author_display_name_override = "AI 팀장"`로 게시되어 상세에서 "AI 팀장" 작성자로 표시됨.
- FE 변경 없음(백엔드 `author_display_name` 응답으로 반영).
